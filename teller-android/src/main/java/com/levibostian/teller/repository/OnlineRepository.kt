package com.levibostian.teller.repository

import com.levibostian.teller.Teller
import com.levibostian.teller.cachestate.OnlineCacheState
import com.levibostian.teller.error.TellerLimitedFunctionalityException
import com.levibostian.teller.extensions.plusAssign
import com.levibostian.teller.provider.SchedulersProvider
import com.levibostian.teller.provider.TellerSchedulersProvider
import com.levibostian.teller.repository.manager.OnlineRepositoryRefreshManager
import com.levibostian.teller.repository.manager.OnlineRepositoryRefreshManagerWrapper
import com.levibostian.teller.repository.manager.OnlineRepositoryCacheAgeManager
import com.levibostian.teller.repository.manager.TellerOnlineRepositoryCacheAgeManager
import com.levibostian.teller.subject.OnlineCacheStateBehaviorSubject
import com.levibostian.teller.testing.repository.OnlineRepositoryRefreshResultTesting
import com.levibostian.teller.type.Age
import com.levibostian.teller.util.TaskExecutor
import com.levibostian.teller.util.TellerTaskExecutor
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import java.lang.ref.WeakReference
import java.util.*

typealias GetCacheRequirementsTag = String

typealias OnlineRepositoryCache = Any
typealias OnlineRepositoryFetchResponse = Any

/**
 * Teller repository that manages a cache that is obtained from a network fetch request.
 *
 * Using [OnlineRepository] is quite simple:
 * 1. Subclass [OnlineRepository] for each of your cache types.
 * 2. Call [observe] to begin observing the current state of the cache.
 * 3. Set [requirements] with an object used to begin querying cache and fetching fresh cache if the cache does not exist or is old.
 * 4. Call [dispose] when you are done using the [OnlineRepository].
 *
 * [OnlineRepository] is thread safe. Actions called upon for [OnlineRepository] can be performed on any thread.
 */
abstract class OnlineRepository<CACHE: OnlineRepositoryCache, GET_CACHE_REQUIREMENTS: OnlineRepository.GetCacheRequirements, FETCH_RESPONSE: OnlineRepositoryFetchResponse> {

    constructor() {
        if (!Teller.shared.unitTesting) init()
    }

    private fun init(
            schedulersProvider: SchedulersProvider = TellerSchedulersProvider(),
            cacheAgeManager: OnlineRepositoryCacheAgeManager = TellerOnlineRepositoryCacheAgeManager(),
            refreshManager: OnlineRepositoryRefreshManager = OnlineRepositoryRefreshManagerWrapper(),
            taskExecutor: TaskExecutor = TellerTaskExecutor(),
            refreshManagerListener: OnlineRepositoryRefreshManager.Listener = RefreshManagerListener(),
            teller: Teller = Teller.shared
    ) {
        this.schedulersProvider = schedulersProvider
        this.cacheAgeManager = cacheAgeManager
        this.refreshManager = refreshManager
        this.taskExecutor = taskExecutor
        this.refreshManagerListener = refreshManagerListener
        this.teller = teller
    }

    /**
     * Idea taken from [CompositeDisposable]. Use of volatile variable to mark object as no longer used.
     */
    @Volatile private var disposed = false

    private var observeCacheDisposeBag: CompositeDisposable = CompositeDisposable()
    // Important to never be nil so that we can call `observe` on this class and always be able to listen.
    private var currentStateOfCache: OnlineCacheStateBehaviorSubject<CACHE> = OnlineCacheStateBehaviorSubject()
    private lateinit var schedulersProvider: SchedulersProvider
    private lateinit var taskExecutor: TaskExecutor
    /**
     * Use of an object as listener to allow [refreshBegin], [refreshComplete] functions to be private.
     */
    private lateinit var refreshManagerListener: OnlineRepositoryRefreshManager.Listener

    private lateinit var cacheAgeManager: OnlineRepositoryCacheAgeManager
    private lateinit var refreshManager: OnlineRepositoryRefreshManager
    private lateinit var teller: Teller

    internal constructor(schedulersProvider: SchedulersProvider,
                         cacheAgeManager: OnlineRepositoryCacheAgeManager,
                         refreshManager: OnlineRepositoryRefreshManager,
                         taskExecutor: TaskExecutor,
                         refreshManagerListener: OnlineRepositoryRefreshManager.Listener,
                         teller: Teller) {
        init(schedulersProvider, cacheAgeManager, refreshManager, taskExecutor, refreshManagerListener, teller)
    }

    /**
     * Used for testing purposes to initialize the state of a [OnlineRepository] subclass instance.
     *
     * @see OnlineRepositoryRefreshResultTesting
     */
    object Testing

    /**
     * Used to set how old cache can be on the device before it is considered too old and new cache should be fetched.
     */
    abstract var maxAgeOfCache: Age

    /**
     * Requirements needed to be able to load cache from the device and to fetch new cache from the network.
     *
     * When this property is set, the [OnlineRepository] instance will begin to observe the cache by loading the cache from the device and checking if it needs to fetch fresh cache from the network. All of the work will be done for you.
     *
     * If the user decides to scroll to the bottom of a list, view a different user profile, or any other reason you need to change to observe a different piece of cache, just set [requirements] again.
     *
     * If requirements is set to null, we will stop observing the cache changes and reset the state of cache to none.
     *
     * @throws RuntimeException If calling after calling [dispose].
     * @throws TellerLimitedFunctionalityException If calling when initializing Teller via [Teller.initUnitTesting].
     */
    var requirements: GET_CACHE_REQUIREMENTS? = null
        // 1. Cancel observing cache so no more reading of cache updates can happen.
        // 2. Cancel refreshing so no fetch can finish.
        // 3. Set currentStateOfCache to something so anyone observing does not think they are still observing old requirements (old cache).
        // 4. Start everything up again.
        set(value) {
            teller.assertNotLimitedFunctionality()
            assertNotDisposed()

            field?.let { oldValue ->
                refreshManager.cancelTasksForRepository(oldValue.tag, refreshManagerListener)
                stopObservingCache()
            }

            field = value

            val newRequirements = field
            if (newRequirements != null) {
                if (cacheAgeManager.hasSuccessfullyFetchedCache(newRequirements.tag)) {
                    currentStateOfCache.resetToCacheState(newRequirements, cacheAgeManager.lastSuccessfulFetch(newRequirements.tag)!!)
                    restartObservingCache(newRequirements)
                } else {
                    currentStateOfCache.resetToNoCacheState(newRequirements)
                    // When we set new requirements, we want to fetch for first time if have never been done before. Example: paging cache. If we go to a new page we have never gotten before, we want to fetch that cache for the first time.
                    performRefresh()
                }
            } else {
                currentStateOfCache.resetStateToNone()
            }
        }

    private fun restartObservingCache(requirements: GET_CACHE_REQUIREMENTS) {
        /**
         * You need to subscribe and observe on the UI thread because popular database solutions such as Realm, SQLite all have a "write on background, read on UI" approach. You cannot read on the background and send the read objects to the UI thread. So, we read on the UI.
         *
         * Also, you need to call [observeCache] while on the UI thread for cases like Realm where the Realm instance constructed in [observeCache] needs to be constructed and used on the same thread.
         */
        taskExecutor.postUI {
            if (disposed) return@postUI

            stopObservingCache()

            observeCacheDisposeBag += observeCache(requirements)
                    .subscribeOn(schedulersProvider.main())
                    .observeOn(schedulersProvider.main())
                    .subscribe { cache ->
                        val needsToRefreshCache = cacheAgeManager.isCacheTooOld(requirements.tag, maxAgeOfCache)

                        if (isCacheEmpty(cache, requirements)) {
                            currentStateOfCache.changeState { it.cacheEmpty() }
                        } else {
                            currentStateOfCache.changeState { it.cache(cache) }
                        }

                        if (needsToRefreshCache) {
                            performRefresh()
                        }
                    }
        }
    }

    private fun performRefresh() {
        this.requirements?.let { requirements ->
            getRefresh(false, requirements)
                    .subscribeOn(schedulersProvider.io())
                    .subscribe()
        }
    }

    private fun assertNotDisposed() {
        if (disposed) throw RuntimeException("Cannot call after calling `dispose()`")
    }

    /**
     * How to begin observing the state of the cache for this [OnlineRepository].
     *
     * Teller will automatically perform a [refresh] if the cache does not exist or is too old. You will get notified anytime that the state of the cache changes.
     *
     * @throws RuntimeException If calling after calling [dispose].
     * @throws TellerLimitedFunctionalityException If calling when initializing Teller via [Teller.initUnitTesting].
     */
    fun observe(): Observable<OnlineCacheState<CACHE>> {
        teller.assertNotLimitedFunctionality()
        assertNotDisposed()

        if (requirements != null) {
            // Trigger a refresh to help keep cache up-to-date.
            performRefresh()
        }

        return currentStateOfCache.asObservable()
    }

    /**
     * Dispose of the [OnlineRepository] to shut down observing of the cache and stops refresh tasks if they have begun.
     *
     * Do this in onDestroy() of your Fragment or Activity, for example.
     *
     * After calling [dispose], your [OnlineRepository] instance is useless. Calling any function on the instance in the future will result in a [RuntimeException].
     *
     * @throws TellerLimitedFunctionalityException If calling when initializing Teller via [Teller.initUnitTesting].
     */
    @Synchronized
    fun dispose() {
        teller.assertNotLimitedFunctionality()

        if (disposed) return
        disposed = true

        requirements?.let {
            refreshManager.cancelTasksForRepository(it.tag, refreshManagerListener)
        }

        currentStateOfCache.subject.onComplete()

        stopObservingCache()
    }

    private fun stopObservingCache() {
        observeCacheDisposeBag.clear()
        observeCacheDisposeBag = CompositeDisposable()
    }

    /**
     * Manually perform a refresh of the cache.
     *
     * Ideal in these scenarios:
     * 1. User indicates in the UI they would like to check for new cache. Example: `UIRefreshControl` in a `UITableView` indicating to refresh.
     * 2. Keep app cache up-to-date at all times through a background job.
     *
     * @throws IllegalStateException If [requirements] have not yet been set for the [OnlineRepository]. [OnlineRepository] cannot refresh it it does not know what to refresh.
     * @throws RuntimeException If calling after calling [dispose].
     * @throws TellerLimitedFunctionalityException If calling when initializing Teller via [Teller.initUnitTesting].
     */
    @Throws(IllegalStateException::class)
    fun refresh(force: Boolean): Single<RefreshResult> {
        teller.assertNotLimitedFunctionality()
        assertNotDisposed()

        val requirements = this.requirements ?: throw IllegalStateException("You need to set requirements before calling refresh.")

        return getRefresh(force, requirements)
    }

    private fun getRefresh(force: Boolean, requirements: GET_CACHE_REQUIREMENTS): Single<RefreshResult> {
        return if (force || !cacheAgeManager.hasSuccessfullyFetchedCache(requirements.tag) || cacheAgeManager.isCacheTooOld(requirements.tag, maxAgeOfCache)) {
            refreshManager.refresh(fetchFreshCache(requirements), requirements.tag, refreshManagerListener)
        } else {
            Single.just(RefreshResult.skipped(RefreshResult.SkippedReason.CACHE_NOT_TOO_OLD))
        }
    }

    @Synchronized
    internal fun refreshBegin(tag: GetCacheRequirementsTag) {
        // User may have changed requirements
        val requirements = requirements ?: return
        if (requirements.tag != tag) return
        // Ignore async callback if disposed
        if (disposed) return

        val noCacheExists = currentStateOfCache.currentState?.noCacheExists ?: return

        if (noCacheExists) {
            currentStateOfCache.changeState { it.firstFetch() }
        } else {
            currentStateOfCache.changeState { it.fetchingFreshCache() }
        }
    }

    /**
     * Note: Make sure this is called from a background thread.
     */
    @Synchronized
    internal fun refreshComplete(tag: GetCacheRequirementsTag,
                                 response: FetchResponse<FETCH_RESPONSE>) {
        // User may have changed requirements
        val requirements = requirements ?: return
        if (requirements.tag != tag) return
        // Ignore async callback if disposed
        if (disposed) return

        val noCacheExists = currentStateOfCache.currentState?.noCacheExists ?: return

        val fetchError = response.failure
        if (fetchError != null) {
            /**
             * Note: Make sure that you **do not** [restartObservingCache] if there is a failure and we have never fetched cache successfully before. We cannot begin observing a cache until we know for sure a cache actually exists!
             */
            if (noCacheExists) {
                currentStateOfCache.changeState { it.failFirstFetch(fetchError) }
            } else {
                currentStateOfCache.changeState { it.failRefreshCache(fetchError) }
            }
        } else {
            val timeFetched = Date()

            if (noCacheExists) {
                currentStateOfCache.changeState { it.successfulFirstFetch(timeFetched) }
            } else {
                currentStateOfCache.changeState { it.successfulRefreshCache(timeFetched) }
            }

            val newCache = response.response!!
            stopObservingCache()
            saveCache(newCache, requirements)
            cacheAgeManager.updateLastSuccessfulFetch(requirements.tag, timeFetched)
            restartObservingCache(requirements)
        }
    }

    /**
     * Repository does what it needs in order to fetch fresh cache. Probably call network API.
     *
     * **Called on a background thread.**
     */
    abstract fun fetchFreshCache(requirements: GET_CACHE_REQUIREMENTS): Single<FetchResponse<FETCH_RESPONSE>>

    /**
     * Save the new cache [cache] to whatever storage method [OnlineRepository] chooses.
     *
     * **Called on a background thread.**
     */
    protected abstract fun saveCache(cache: FETCH_RESPONSE, requirements: GET_CACHE_REQUIREMENTS)

    /**
     * Used to allow [saveCache] is available internally *and* protected.
     */
    internal fun saveCacheSyncCurrentThread(cache: FETCH_RESPONSE, requirements: GET_CACHE_REQUIREMENTS) {
        this.saveCache(cache, requirements)
    }

    /**
     * Get existing cache saved on the device if it exists. If no cache exists, return an empty response set in the Observable and return true in [isCacheEmpty]. **Do not** return nil or an Observable with nil as a value as this will cause an exception.
     *
     * This function is only called after cache has been fetched successfully from [fetchFreshCache].
     *
     * **Called on main UI thread.**
     */
    protected abstract fun observeCache(requirements: GET_CACHE_REQUIREMENTS): Observable<CACHE>

    /**
     * Used to determine if cache is empty or not.
     *
     * **Called on main UI thread.**
     */
    protected abstract fun isCacheEmpty(cache: CACHE, requirements: GET_CACHE_REQUIREMENTS): Boolean

    /**
     * Used by Teller to determine what chunk of cache to fetch and query.
     *
     * @property tag Unique tag that drives the behavior of a [OnlineRepository]. The tag needs to describe (1) the type of cache being stored (example: friend, friend request, song, user profile, etc) and (2) identity the fetch call to obtain this cache. Example: "FriendRequests_page1" for paging, "UserProfile_user2332" for a query param. Teller uses this [tag] to determine how old some particular cache is. If it's too old, new cache will be fetched.
     */
    interface GetCacheRequirements {
        var tag: GetCacheRequirementsTag
    }

    /**
     * When a [OnlineRepository.fetchFreshCache] task is performed, Teller needs to know if the fetch request is considered to be a success or a failure.
     */
    class FetchResponse<FETCH_RESPONSE: OnlineRepositoryFetchResponse> private constructor(val response: FETCH_RESPONSE? = null,
                                                                 val failure: Throwable? = null) {

        companion object {
            @JvmStatic
            fun <FETCH_RESPONSE: OnlineRepositoryFetchResponse> success(response: FETCH_RESPONSE): FetchResponse<FETCH_RESPONSE> {
                return FetchResponse(response = response)
            }

            @JvmStatic
            fun <FETCH_RESPONSE: OnlineRepositoryFetchResponse> fail(throwable: Throwable): FetchResponse<FETCH_RESPONSE> {
                return FetchResponse(failure = throwable)
            }
        }

        fun isSuccessful(): Boolean = response != null

        fun isFailure(): Boolean = failure != null
    }

    /**
     * Result object of a call to [OnlineRepository.refresh]. Understand if a refresh call was successful, not successful, or skipped for some reason.
     */
    class RefreshResult private constructor(val successful: Boolean = false,
                                            val failedError: Throwable? = null,
                                            val skipped: SkippedReason? = null) {

        /**
         * Used for testing purposes to create instances of [OnlineCacheState].
         *
         * @see OnlineRepositoryRefreshResultTesting
         */
        object Testing

        internal companion object {
            fun success(): RefreshResult = RefreshResult(successful = true)

            fun failure(error: Throwable): RefreshResult = RefreshResult(failedError = error)

            fun skipped(reason: SkippedReason): RefreshResult = RefreshResult(skipped = reason)
        }

        fun didSkip(): Boolean = skipped != null

        fun didFail(): Boolean = failedError != null

        fun didSucceed(): Boolean = successful

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is RefreshResult) return false

            return this.successful == other.successful &&
                    this.failedError == other.failedError &&
                    this.skipped == other.skipped
        }

        override fun hashCode(): Int {
            var result = successful.hashCode()
            result = 31 * result + (failedError?.hashCode() ?: 0)
            result = 31 * result + (skipped?.hashCode() ?: 0)
            return result
        }

        /**
         * If a [OnlineRepository.refresh] task was skipped, compare the [skipped] property with the enum cases below to determine why the refresh task was skipped.
         */
        enum class SkippedReason {
            /**
             * Cache exists but, it's not too old yet. Call [refresh] with `force = true` to perform a refresh manually.
             */
            CACHE_NOT_TOO_OLD,
            /**
             * The refresh call got cancelled. The result of the fetch call will be ignored and not saved.
             */
            CANCELLED
        }

    }

    /**
     * Inner class to pass on calls to parent object. As long as this inner class is referred by a [WeakReference], this will be fine and will avoid memory leaks.
     */
    private inner class RefreshManagerListener: OnlineRepositoryRefreshManager.Listener {

        override fun refreshBegin(tag: GetCacheRequirementsTag) {
            this@OnlineRepository.refreshBegin(tag)
        }

        override fun <RefreshResponseType: OnlineRepositoryFetchResponse> refreshComplete(tag: GetCacheRequirementsTag, response: FetchResponse<RefreshResponseType>) {
            @Suppress("UNCHECKED_CAST") val fetchResponse = response as FetchResponse<FETCH_RESPONSE>
            this@OnlineRepository.refreshComplete(tag, fetchResponse)
        }
    }

}