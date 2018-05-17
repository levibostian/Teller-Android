package com.levibostian.teller.repository

import android.annotation.SuppressLint
import com.levibostian.teller.Teller
import com.levibostian.teller.datastate.OnlineDataState
import com.levibostian.teller.subject.OnlineDataStateBehaviorSubject
import com.levibostian.teller.type.AgeOfData
import com.levibostian.teller.util.ConstantsUtil
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*

abstract class OnlineRepository<RESULT: Any, GET_DATA_REQUIREMENTS: OnlineRepository.GetDataRequirements, REQUEST: Any> {

    private val forceSyncNextTimeFetchKey by lazy {
        "${ConstantsUtil.PREFIX}_forceSyncNextTimeFetch_dataSource_${this::class.java.simpleName}_key"
    }

    companion object {
        const val INVALID_LAST_FETCHED_VALUE: Long = -1L
    }

    private var observeCachedDataDisposable: Disposable? = null
    private var stateOfDate: OnlineDataStateBehaviorSubject<RESULT>? = null

    /**
     * Used to set how old data can be in the app for this certain type of data before it is considered too old and new data should be fetched.
     */
    abstract var maxAgeOfData: AgeOfData

    /**
     * Requirements needed to be able to load data in the [OnlineRepository].
     *
     * When this property is set, the [OnlineRepository] instance will begin to observe the data by loading the cached data and checking if it needs to fetch fresh.
     *
     * @see sync On how to force the data source to fetch fresh data if you want that after setting [loadDataRequirements].
     */
    var loadDataRequirements: GET_DATA_REQUIREMENTS? = null
        set(value) {
            field = value
            observeCachedDataDisposable?.dispose()
            observeCachedDataDisposable = null
            beginObservingStateOfData()
        }

    var lastTimeFetchedFreshData: Date? = null
        get() {
            val loadDataRequirements = this.loadDataRequirements ?: return null

            val lastFetchedTime = Teller.shared.sharedPreferences.getLong(loadDataRequirements.tag, INVALID_LAST_FETCHED_VALUE)
            if (lastFetchedTime <= INVALID_LAST_FETCHED_VALUE) return null

            return Date(lastFetchedTime)
        }

    private fun beginObservingStateOfData() {
        loadDataRequirements?.let { getDataRequirements ->
            fun initializeObservingCachedData() {
                if (observeCachedDataDisposable == null || !observeCachedDataDisposable!!.isDisposed) {
                    observeCachedDataDisposable = this.observeCachedData(getDataRequirements)
                            .subscribe({ cachedData ->
                                val needsToFetchFreshData = this.doSyncNextTimeFetched() || this.isDataTooOld()

                                if (cachedData == null || isDataEmpty(cachedData)) {
                                    stateOfDate?.onNextEmpty(needsToFetchFreshData)
                                } else {
                                    stateOfDate?.onNextData(cachedData, lastTimeFetchedFreshData!!, needsToFetchFreshData)
                                }

                                if (needsToFetchFreshData) {
                                    this._sync(getDataRequirements, {
                                        stateOfDate?.onNextDoneFetchingFreshData(null)
                                    }, { error ->
                                        stateOfDate?.onNextDoneFetchingFreshData(error)
                                    })
                                }
                            }, { error ->
                                this.stateOfDate?.onNextError(error)
                            })
                }
            }

            if (!hasEverFetchedData()) {
                stateOfDate?.onNextFirstFetchOfData()

                this._sync(getDataRequirements, {
                    initializeObservingCachedData()
                }, { _ ->
                    // Here, I am manually setting empty as an error occurred and I don't want the UI to show an infinite loading screen so I set empty manually but do not begin observing data.
                    stateOfDate?.onNextEmpty(false)
                })
            } else {
                initializeObservingCachedData()
            }
        }
    }

    fun observe(): Observable<OnlineDataState<RESULT>> {
        if (stateOfDate == null) {
            stateOfDate = OnlineDataStateBehaviorSubject()
        }
        beginObservingStateOfData()

        return stateOfDate!!.asObservable().doOnDispose {
            observeCachedDataDisposable?.dispose()
        }
    }

    /**
     * Perform a one time sync of the data. Sync will check the last updated date and the allowed age of data to determine if it should fetch fresh data or not. You may override this behavior with `force`.
     *
     * Note: If [loadDataRequirements] is null, [Completable.complete] will be returned from this function and it will be like the sync never happened.
     */
    fun sync(force: Boolean): Completable {
        val getDataRequirements = this.loadDataRequirements ?: return Completable.complete()

        return if (force || this.doSyncNextTimeFetched() || this.isDataTooOld()) {
            Completable.create { observer ->
                this._sync(getDataRequirements, {
                    observer.onComplete()
                }, { error ->
                    observer.onError(error)
                })
            }
        } else {
            Completable.complete()
        }
    }

    private fun _sync(getDataRequirements: GET_DATA_REQUIREMENTS, onComplete: () -> Unit, onError: (Throwable) -> Unit) {
        fun processFailedFetchResult(error: Throwable) {
            this.resetForceSyncNextTimeFetched()
            // Before 5-14-18:
            // Note: We need to set the last updated time here or else we could run an infinite loop if the api call errors.
            // The way that I handle errors now: if an error occurs (network error, status code error, any other error) I tell the rxswift subscriber that the error has occurred so you can tell the user. From there, you have the option to ask the user to retry the network call and perform the retry by calling datasource set data query requirements with force param true.
            //
            // Update 5-14-18:
            // I am commenting out line below because of this example. What if I am building a GitHub app where I take a username and I call API for a list of repos for that username. What if that username does not exist and we get a 404 back from GitHub? If we call `updateLastTimeFreshDataFetched()`, we will set the data to empty the next time the user tries to use that github username again in the app. We don't want that because the data is not empty. It's non-existing. So after some thought, I am going to try and comment this out because I should only update the last time fetched fresh data when the data was actually fetched successfully, right? I say this because in the UI I want to show how old data is. I don't want to show "5 minutes ago" for a failed API request because then users will think that data is 5 minutes old. We want to show to the user the age of the data, not when it was last synced. If I do want to show both, I need to store both individually.
            // this.updateLastTimeFreshDataFetched(getDataRequirements)
            this.stateOfDate?.onNextError(error)
            onError(error)
        }

        this.fetchFreshData(getDataRequirements)
                .subscribe({ freshData ->
                    if (freshData.isSuccessful()) {
                        this.saveData(freshData.data!!)
                        this.resetForceSyncNextTimeFetched()
                        this.updateLastTimeFreshDataFetched(getDataRequirements)
                        onComplete()
                    } else {
                        processFailedFetchResult(freshData.failure!!)
                    }
                }, { error ->
                    processFailedFetchResult(error)
                })
    }

    private fun getDateFromAllowedAgeOfDate(ageOfDate: AgeOfData): Date {
        return ageOfDate.toDate()
    }

    private fun isDataTooOld(): Boolean {
        if (!hasEverFetchedData()) return true

        return isDataOlderThan(getDateFromAllowedAgeOfDate(maxAgeOfData))
    }

    @SuppressLint("ApplySharedPref")
    private fun updateLastTimeFreshDataFetched(getDataRequirements: GET_DATA_REQUIREMENTS) {
        Teller.shared.sharedPreferences.edit().putLong(getDataRequirements.tag, Date().time).commit()
    }

    @SuppressLint("ApplySharedPref")
    private fun resetForceSyncNextTimeFetched() {
        Teller.shared.sharedPreferences.edit().putBoolean(forceSyncNextTimeFetchKey, false).commit()
    }

    private fun doSyncNextTimeFetched(): Boolean {
        return Teller.shared.sharedPreferences.getBoolean(forceSyncNextTimeFetchKey, false)
    }

    /**
     * Call if you want to flag the [Repository] to force sync the next time that it needs to sync data.
     */
    @SuppressLint("ApplySharedPref")
    fun forceSyncNextTimeFetched() {
        Teller.shared.sharedPreferences.edit().putBoolean(forceSyncNextTimeFetchKey, true).commit()
    }

    private fun hasEverFetchedData(): Boolean {
        return lastTimeFetchedFreshData != null
    }

    private fun isDataOlderThan(date: Date): Boolean {
        val lastTimeFetchedData = lastTimeFetchedFreshData ?: return true
        return lastTimeFetchedData < date
    }

    /**
     * Repository does what it needs in order to fetch fresh data. Probably call network API.
     */
    abstract fun fetchFreshData(requirements: GET_DATA_REQUIREMENTS): Single<FetchResponse<REQUEST>>

    /**
     * Save the data to whatever storage method Repository chooses.
     *
     * It is up to you to call [saveData] when you have new data to save. A good place to do this is in a ViewModel.
     *
     * *Note:* It is up to you to run this function from a background thread. This is not done by default for you.
     */
    abstract fun saveData(data: REQUEST)

    /**
     * Get existing cached data saved to the device if it exists. Return nil is data does not exist or is empty.
     */
    abstract fun observeCachedData(requirements: GET_DATA_REQUIREMENTS): Observable<RESULT>

    /**
     * DataType determines if data is empty or not. Because data can be of `Any` type, the DataType must determine when data is empty or not.
     */
    abstract fun isDataEmpty(data: RESULT): Boolean

    /**
     * Data object that are the requirements to fetch, get cached data. It could contain a query term to search for data. It could contain a user id to fetch a user from an API.
     *
     * @property tag Unique tag that drives the behavior of a [OnlineDataSource]. If the [tag] is ever changed, [OnlineDataSource] will trigger a new data fetch so that you can get new data.
     */
    interface GetDataRequirements {
        var tag: String
    }

    class FetchResponse<DATA: Any> private constructor(val data: DATA? = null,
                                                       val failure: Throwable? = null) {
        companion object {
            fun <DATA: Any> success(data: DATA): FetchResponse<DATA> {
                return FetchResponse(data = data)
            }

            fun <DATA: Any> fail(message: String): FetchResponse<DATA> {
                return FetchResponse(failure = ResponseFail(message))
            }

            fun <DATA: Any> fail(throwable: Throwable): FetchResponse<DATA> {
                return FetchResponse(failure = throwable)
            }
        }

        fun isSuccessful(): Boolean = data != null

        fun isFailure(): Boolean = failure != null

        class ResponseFail(message: String): Throwable(message)
    }

}