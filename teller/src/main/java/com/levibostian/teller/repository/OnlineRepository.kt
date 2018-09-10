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
import io.reactivex.disposables.Disposable
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
     * Used to set how old cacheData can be in the app for this certain type of cacheData before it is considered too old and new cacheData should be fetched.
     */
    abstract var maxAgeOfData: AgeOfData

    /**
     * Requirements needed to be able to load cacheData in the [OnlineRepository].
     *
     * When this property is set, the [OnlineRepository] instance will begin to observe the cacheData by loading the cached cacheData and checking if it needs to fetch fresh.
     *
     * **Note:** Make sure to call [dispose]
     *
     * @see sync On how to force the cacheData source to fetch fresh cacheData if you want that after setting [loadDataRequirements].
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
                            .subscribe { cachedData ->
                                val needsToFetchFreshData = this.doSyncNextTimeFetched() || this.isDataTooOld()

                                if (cachedData == null || isDataEmpty(cachedData)) {
                                    stateOfDate?.onNextCacheEmpty(needsToFetchFreshData)
                                } else {
                                    stateOfDate?.onNextCachedData(cachedData, lastTimeFetchedFreshData!!, needsToFetchFreshData)
                                }

                                if (needsToFetchFreshData) {
                                    this._sync(getDataRequirements, {
                                        stateOfDate?.onNextDoneFetchingFreshData(null)
                                    }, { error ->
                                        stateOfDate?.onNextDoneFetchingFreshData(error)
                                    })
                                }
                            }
                }
            }

            if (!hasEverFetchedData()) {
                stateOfDate?.onNextFirstFetchOfData()

                this._sync(getDataRequirements, {
                    stateOfDate?.onNextDoneFirstFetch(null)
                    initializeObservingCachedData()
                }, { error ->
                    stateOfDate?.onNextDoneFirstFetch(error)
                })
            } else {
                initializeObservingCachedData()
            }
        }
    }

    /**
     * How to begin observing the state of the data for this [OnlineRepository]. Teller will automatically perform a [sync] if the cached data does not exist or is too old. You will get notified anytime that the state of the data changes or the data itself ever changes.
     */
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
     * It is assumed that you will use [observe] while using an [OnlineRepository]. However, if you ever do not, make sure to call [dispose] to dispose of the repository and make sure there are not trailing [Observable]s.
     */
    fun dispose() {
        observeCachedDataDisposable?.let {
            if (!it.isDisposed) it.dispose()
        }
    }

    /**
     * Perform a one time sync of the cacheData. Sync will check the last updated date and the allowed age of cacheData to determine if it should fetch fresh cacheData or not. You may override this behavior with `force`.
     *
     * Note: If [loadDataRequirements] is null, [Completable.complete] will be returned from this function and it will be like the sync never happened.
     */
    fun sync(force: Boolean): Single<SyncResult> {
        val getDataRequirements = this.loadDataRequirements ?: return Single.just(SyncResult.skipped(SyncResult.SkippedReason.LOAD_DATA_REQUIREMENTS_NOT_SET))

        return if (force || this.doSyncNextTimeFetched() || this.isDataTooOld()) {
            Single.create<SyncResult> { observer ->
                this._sync(getDataRequirements, {
                    observer.onSuccess(OnlineRepository.SyncResult.success())
                }, { error ->
                    observer.onSuccess(OnlineRepository.SyncResult.failure(error))
                })
            }
        } else {
            Single.just(SyncResult.skipped(SyncResult.SkippedReason.DATA_NOT_TOO_OLD))
        }
    }

    private fun _sync(getDataRequirements: GET_DATA_REQUIREMENTS, onComplete: () -> Unit, onError: (Throwable) -> Unit) {
        this.fetchFreshData(getDataRequirements)
                .subscribe { freshData ->
                    if (freshData.isSuccessful()) {
                        this.saveData(freshData.data!!)
                        this.resetForceSyncNextTimeFetched()
                        this.updateLastTimeFreshDataFetched(getDataRequirements)
                        onComplete()
                    } else {
                        this.resetForceSyncNextTimeFetched()
                        val error = freshData.failure!!
                        onError(error)
                    }
                }
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
     * Call if you want to flag the [Repository] to force sync the next time that it needs to sync cacheData.
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
     * Repository does what it needs in order to fetch fresh cacheData. Probably call network API.
     */
    abstract fun fetchFreshData(requirements: GET_DATA_REQUIREMENTS): Single<FetchResponse<REQUEST>>

    /**
     * Save the cacheData to whatever storage method Repository chooses.
     *
     * It is up to you to call [saveData] when you have new cacheData to save. A good place to do this is in a ViewModel.
     *
     * *Note:* It is up to you to run this function from a background thread. This is not done by default for you.
     */
    abstract fun saveData(data: REQUEST)

    /**
     * Get existing cached cacheData saved to the device if it exists. Return nil is cacheData does not exist or is empty.
     */
    abstract fun observeCachedData(requirements: GET_DATA_REQUIREMENTS): Observable<RESULT>

    /**
     * DataType determines if cacheData is empty or not. Because cacheData can be of `Any` type, the DataType must determine when cacheData is empty or not.
     */
    abstract fun isDataEmpty(data: RESULT): Boolean

    /**
     * Data object that are the requirements to fetch, get cached cacheData. It could contain a query term to search for cacheData. It could contain a user id to fetch a user from an API.
     *
     * @property tag Unique tag that drives the behavior of a [OnlineDataSource]. If the [tag] is ever changed, [OnlineDataSource] will trigger a new cacheData fetch so that you can get new cacheData.
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

    class SyncResult private constructor(val successful: Boolean = false,
                                         val failedError: Throwable? = null,
                                         val skipped: SkippedReason? = null) {

        companion object {
            fun success(): SyncResult = SyncResult(successful = true)

            fun failure(error: Throwable): SyncResult = SyncResult(failedError = error)

            fun skipped(reason: SkippedReason): SyncResult = SyncResult(skipped = reason)
        }

        fun didSkip(): Boolean = skipped != null

        fun didFail(): Boolean = failedError != null

        fun didSucceed(): Boolean = successful

        enum class SkippedReason {
            /**
             * The [OnlineRepository.loadDataRequirements] is not set which is required for sync to run.
             */
            LOAD_DATA_REQUIREMENTS_NOT_SET,
            /**
             * Cached cacheData already exists for the cacheData type, it's not too old yet, and force sync was not true to force sync to run.
             */
            DATA_NOT_TOO_OLD
        }

    }


}