package com.levibostian.teller.repository

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.levibostian.teller.datastate.OnlineDataState
import com.levibostian.teller.extensions.plusAssign
import com.levibostian.teller.provider.SchedulersProvider
import com.levibostian.teller.provider.TellerSchedulersProvider
import com.levibostian.teller.repository.manager.OnlineRepositoryRefreshManager
import com.levibostian.teller.repository.manager.OnlineRepositoryRefreshManagerWrapper
import com.levibostian.teller.repository.manager.OnlineRepositorySyncStateManager
import com.levibostian.teller.repository.manager.TellerOnlineRepositorySyncStateManager
import com.levibostian.teller.subject.OnlineDataStateBehaviorSubject
import com.levibostian.teller.type.AgeOfData
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*

typealias GetDataRequirementsTag = String

/**
 * Teller repository that manages cached data that is obtained from a network fetch request.
 *
 * Using [OnlineRepository] is quite simple:
 * 1. Subclass [OnlineRepository] for each of your data types
 * 2. Call [observe] to begin observing the current state of the cached data.
 * 3. Set [requirements] with an object used to begin querying cached data and fetching fresh data if the data does not exist or is old.
 *
 * OnlineRepository is thread safe. Actions called upon for OnlineRepository can be performed on any thread.
 */
abstract class OnlineRepository<CACHE: Any, GET_DATA_REQUIREMENTS: OnlineRepository.GetDataRequirements, FETCH_RESPONSE: Any>: OnlineRepositoryRefreshManager.Listener {

    constructor() {
        schedulersProvider = TellerSchedulersProvider()
        syncStateManager = TellerOnlineRepositorySyncStateManager()
        refreshManager = OnlineRepositoryRefreshManagerWrapper()
    }

    private var observeCacheDisposeBag: CompositeDisposable = CompositeDisposable()
    private var currentStateOfData: OnlineDataStateBehaviorSubject<CACHE> = OnlineDataStateBehaviorSubject() // This is important to never be nil so that we can call `observe` on this class and always be able to listen.
    private val schedulersProvider: SchedulersProvider

    private val syncStateManager: OnlineRepositorySyncStateManager
    private val refreshManager: OnlineRepositoryRefreshManager

    internal constructor(syncStateManager: OnlineRepositorySyncStateManager,
                         refreshManager: OnlineRepositoryRefreshManager,
                         schedulersProvider: SchedulersProvider) {
        this.syncStateManager = syncStateManager
        this.refreshManager = refreshManager
        this.schedulersProvider = schedulersProvider
    }

    /**
     * Used to set how old cached data can be on the device before it is considered too old and new cached data should be fetched.
     */
    abstract var maxAgeOfData: AgeOfData

    /**
     * Requirements needed to be able to load cached data from the device and to fetch new cached data from the network.
     *
     * When this property is set, the [OnlineRepository] instance will begin to observe the cacheData by loading the cached data on the device and checking if it needs to fetch fresh data from the network. All of the work will be done for you.
     *
     * If the user decides to scroll to the next page of data, view a different user profile, or any other reason you need to change to observe a different piece of data, just set [requirements] again.
     *
     * If requirements is set to null, we will stop observing the cache changes and reset the state of data to null.
     */
    var requirements: GET_DATA_REQUIREMENTS? = null
        // 1. Cancel observing cache so no more reading of cache updates can happen.
        // 2. Cancel refreshing so no fetch can finish.
        // 3. Set curentStateOfData to something so anyone observing does not think they are still observing old requirements (old data).
// 4. Start everything up again.
        set(value) {
            field?.let { oldValue ->
                refreshManager.cancelTasksForRepository(oldValue.tag, this)
                stopObservingCache()
            }

            field = value

            val newValue = field
            if (newValue != null) {
                if (syncStateManager.hasEverFetchedData(newValue.tag)) {
                    currentStateOfData.resetToCacheState(newValue, syncStateManager.lastTimeFetchedData(newValue.tag)!!)
                    beginObservingCachedData(newValue)
                } else {
                    currentStateOfData.resetToNoCacheState(newValue)
                    // When we set new requirements, we want to fetch for first time if have never been done before. Example: paging data. If we go to a new page we have never gotten before, we want to fetch that data for the first time.
                    performRefresh()
                }
            } else {
                currentStateOfData.resetStateToNone()
            }
        }

    @Synchronized
    private fun beginObservingCachedData(requirements: GET_DATA_REQUIREMENTS) {
        if (!syncStateManager.hasEverFetchedData(requirements.tag)) {
            throw RuntimeException("You cannot begin observing cached data until after data has been successfully fetched at least once")
        }

        // I need to subscribe and observe on the UI thread because popular database solutions such as Realm, Core Data all have a "write on background, read on UI" approach. You cannot read on the background and send the read objects to the UI thread. So, we read on the UI.
        Observable.fromCallable {
            stopObservingCache()

            observeCacheDisposeBag += observeCachedData(requirements)
                    .subscribeOn(schedulersProvider.main())
                    .observeOn(schedulersProvider.main())
                    .subscribe { cache ->
                        val needsToFetchFreshData = syncStateManager.isDataTooOld(requirements.tag, maxAgeOfData)

                        if (isDataEmpty(cache, requirements)) {
                            currentStateOfData.changeState { it.cacheIsEmpty() }
                        } else {
                            currentStateOfData.changeState { it.cachedData(cache) }
                        }

                        if (needsToFetchFreshData) {
                            performRefresh()
                        }
                    }
        }
                .subscribeOn(schedulersProvider.main())
                .subscribe()
    }

    private fun performRefresh() {
        refresh(false)
                .subscribeOn(schedulersProvider.io())
                .subscribe()
    }

    /**
     * How to begin observing the state of the data for this [OnlineRepository].
     *
     * Teller will automatically perform a [refresh] if the cached data does not exist or is too old. You will get notified anytime that the state of the data changes or the data itself ever changes.
     */
    fun observe(): Observable<OnlineDataState<CACHE>> {
        if (requirements != null) {
            // Trigger a refresh to help keep data up-to-date.
            performRefresh()
        }

        return currentStateOfData.asObservable()
    }

    /**
     * Dispose of the [OnlineRepository] to shut down observing of cached data and stops refresh tasks if they have begun.
     *
     * Do this in onDestroy() of your Fragment or Activity, for example.
     */
    fun dispose() {
        requirements?.let {
            refreshManager.cancelTasksForRepository(it.tag, this)
        }

        currentStateOfData.subject.onComplete()

        stopObservingCache()
    }

    private fun stopObservingCache() {
        observeCacheDisposeBag.clear()
        observeCacheDisposeBag = CompositeDisposable()
    }

    /**
     * Manually perform a refresh of the cached data.
     *
     * Ideal in these scenarios:
     * 1. User indicates in the UI they would like to check for new data. Example: `UIRefreshControl` in a `UITableView` indicating to refresh the data.
     * 2. Keep app data up-to-date at all times through a background job.
     *
     * @throws IllegalStateException If [requirements] have not yet been set for the [OnlineRepository]. [OnlineRepository] cannot refresh it it does not know what to refresh.
     */
    @Throws(IllegalStateException::class)
    fun refresh(force: Boolean): Single<RefreshResult> {
        val requirements = this.requirements ?: throw IllegalStateException("You need to set requirements before calling refresh.")

        return if (force || !syncStateManager.hasEverFetchedData(requirements.tag) || syncStateManager.isDataTooOld(requirements.tag, maxAgeOfData)) {
            refreshManager.refresh(fetchFreshData(requirements), requirements.tag, this)
        } else {
            Single.just(RefreshResult.skipped(RefreshResult.SkippedReason.DATA_NOT_TOO_OLD))
        }
    }

    @Synchronized
    override fun refreshBegin() {
        val hasEverFetchedDataBefore = !currentStateOfData.currentState.noCacheExists

        if (!hasEverFetchedDataBefore) {
            currentStateOfData.changeState { it.firstFetch() }
        } else {
            currentStateOfData.changeState { it.fetchingFreshCache() }
        }
    }

    @Synchronized
    override fun <RefreshResultDataType: Any> refreshComplete(response: FetchResponse<RefreshResultDataType>) {
        val requirements = requirements ?: return

        val hasEverFetchedDataBefore = !currentStateOfData.currentState.noCacheExists

        val fetchError = response.failure
        if (fetchError != null) {
            // Note: Make sure that you **do not** beginObservingCachedData() if there is a failure and we have never fetched data successfully before. We cannot begin observing cached data until we know for sure a cache actually exists!
            if (!hasEverFetchedDataBefore) {
                currentStateOfData.changeState { it.errorFirstFetch(fetchError) }
            } else {
                currentStateOfData.changeState { it.failFetchingFreshCache(fetchError) }
            }
        } else {
            val timeFetched = Date()

            if (!hasEverFetchedDataBefore) {
                currentStateOfData.changeState { it.successfulFirstFetch(timeFetched) }
            } else {
                currentStateOfData.changeState { it.successfulFetchingFreshCache(timeFetched) }
            }

            Observable.fromCallable {
                stopObservingCache()

                @Suppress("UNCHECKED_CAST") val newCache = response.data as FETCH_RESPONSE
                saveData(newCache, requirements)

                syncStateManager.updateAgeOfData(requirements.tag, timeFetched)

                beginObservingCachedData(requirements)
            }
                    .subscribeOn(schedulersProvider.io())
                    .subscribe()
        }
    }

    /**
     * Repository does what it needs in order to fetch fresh cacheData. Probably call network API.
     *
     * Feel free to call this function yourself anytime that you want to perform an API call *without* affecting the [OnlineRepository].
     *
     * **Called on a background thread.**
     */
    abstract fun fetchFreshData(requirements: GET_DATA_REQUIREMENTS): Single<FetchResponse<FETCH_RESPONSE>>

    /**
     * Save the new cache [data] to whatever storage method [OnlineRepository] chooses.
     *
     * **Called on a background thread.**
     */
    abstract fun saveData(data: FETCH_RESPONSE, requirements: GET_DATA_REQUIREMENTS)

    /**
     * Get existing cached data saved on the device if it exists. If no data exists, return an empty data set in the Observable and return true in [isDataEmpty]. **Do not** return nil or an Observable with nil as a value as this will cause an exception.
     *
     * This function is only called after data has been fetched successfully.
     *
     * **Called on main UI thread.**
     */
    abstract fun observeCachedData(requirements: GET_DATA_REQUIREMENTS): Observable<CACHE>

    /**
     * DataType determines if cacheData is empty or not. Because cacheData can be of `Any` type, the DataType must determine when cacheData is empty or not.
     *
     * **Called on main UI thread.**
     */
    abstract fun isDataEmpty(cache: CACHE, requirements: GET_DATA_REQUIREMENTS): Boolean

    /**
     * Data object that are the requirements to fetch fresh data or get cached data on device.
     *
     * @property tag Unique tag that drives the behavior of a [OnlineRepository]. The tag needs to describe (1) the type of data being stored (example: friend, friend request, song, user profile, etc) and (2) identity the fetch call to obtain this data. Example: "FriendRequests_page1" for paging, "UserProfile_user2332" for a query param. Teller uses this [tag] to determine how old some particular cache data is. If it's too old, new data will be fetched.
     */
    interface GetDataRequirements {
        var tag: GetDataRequirementsTag
    }

    /**
     * When a [OnlineRepository.fetchFreshData] task is performed, Teller needs to know if the fetch request is considered to be a success or a failure.
     */
    class FetchResponse<FETCH_RESPONSE: Any> private constructor(val data: FETCH_RESPONSE? = null,
                                                                 val failure: Throwable? = null) {
        companion object {
            @JvmStatic
            fun <FETCH_RESPONSE: Any> success(data: FETCH_RESPONSE): FetchResponse<FETCH_RESPONSE> {
                return FetchResponse(data = data)
            }

            @JvmStatic
            fun <FETCH_RESPONSE: Any> fail(message: String): FetchResponse<FETCH_RESPONSE> {
                return FetchResponse(failure = ResponseFail(message))
            }

            @JvmStatic
            fun <FETCH_RESPONSE: Any> fail(throwable: Throwable): FetchResponse<FETCH_RESPONSE> {
                return FetchResponse(failure = throwable)
            }
        }

        fun isSuccessful(): Boolean = data != null

        fun isFailure(): Boolean = failure != null

        class ResponseFail(message: String): Throwable(message)
    }

    /**
     * Result object of a call to [OnlineRepository.refresh]. Understand if a refresh call was successful, not successful, or skipped for some reason.
     */
    class RefreshResult private constructor(val successful: Boolean = false,
                                            val failedError: Throwable? = null,
                                            val skipped: SkippedReason? = null) {

        internal companion object {
            fun success(): RefreshResult = RefreshResult(successful = true)

            fun failure(error: Throwable): RefreshResult = RefreshResult(failedError = error)

            fun skipped(reason: SkippedReason): RefreshResult = RefreshResult(skipped = reason)
        }

        fun didSkip(): Boolean = skipped != null

        fun didFail(): Boolean = failedError != null

        fun didSucceed(): Boolean = successful

        /**
         * If a [OnlineRepository.refresh] task was skipped, compare the [skipped] property with the enum cases below to determine why the refresh task was skipped.
         */
        enum class SkippedReason {
            /**
             * Cached cacheData already exists for the cacheData type, it's not too old yet, and force sync was not true to force sync to run.
             */
            DATA_NOT_TOO_OLD,
            /**
             * The refresh call got cancelled. The result of the fetch call will be ignored and not saved.
             */
            CANCELLED
        }

    }

}