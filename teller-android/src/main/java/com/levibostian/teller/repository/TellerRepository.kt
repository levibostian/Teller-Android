package com.levibostian.teller.repository

import com.levibostian.teller.Teller
import com.levibostian.teller.cachestate.CacheState
import com.levibostian.teller.error.TellerLimitedFunctionalityException
import com.levibostian.teller.extensions.plusAssign
import com.levibostian.teller.provider.SchedulersProvider
import com.levibostian.teller.provider.TellerSchedulersProvider
import com.levibostian.teller.repository.manager.RepositoryRefreshManager
import com.levibostian.teller.repository.manager.RepositoryRefreshManagerWrapper
import com.levibostian.teller.repository.manager.RepositoryCacheAgeManager
import com.levibostian.teller.repository.manager.TellerRepositoryCacheAgeManager
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

typealias RepositoryCache = Any
typealias DataSourceFetchResponse = Any

/**
 * Teller repository that manages a cache that is obtained from a network fetch request.
 *
 * Using [TellerRepository] is quite simple:
 * 1. Subclass [TellerRepository] for each of your cache types.
 * 2. Call [observe] to begin observing the current state of the cache.
 * 3. Set [requirements] with an object used to begin querying cache and fetching fresh cache if the cache does not exist or is old.
 * 4. Call [dispose] when you are done using the [TellerRepository].
 *
 * [TellerRepository] is thread safe. Actions called upon for [TellerRepository] can be performed on any thread.
 */
abstract class TellerRepository<CACHE: RepositoryCache, GET_CACHE_REQUIREMENTS: TellerRepository.GetCacheRequirements, FETCH_RESPONSE: DataSourceFetchResponse> {

    constructor() {
        if (!Teller.shared.unitTesting) init()
    }

    private fun init(
            schedulersProvider: SchedulersProvider = TellerSchedulersProvider(),
            cacheAgeManager: RepositoryCacheAgeManager = TellerRepositoryCacheAgeManager(),
            refreshManager: RepositoryRefreshManager = RepositoryRefreshManagerWrapper(),
            taskExecutor: TaskExecutor = TellerTaskExecutor(),
            refreshManagerListener: RepositoryRefreshManager.Listener = RefreshManagerListener(),
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
    @Volatile internal var disposed = false

    internal var observeCacheDisposeBag: CompositeDisposable = CompositeDisposable()
    // Important to never be nil so that we can call `observe` on this class and always be able to listen.
    internal var currentStateOfCache: OnlineCacheStateBehaviorSubject<CACHE> = OnlineCacheStateBehaviorSubject()
    internal lateinit var schedulersProvider: SchedulersProvider
    internal lateinit var taskExecutor: TaskExecutor
    /**
     * Use of an object as listener to allow [refreshBegin], [refreshComplete] functions to be private.
     */
    internal lateinit var refreshManagerListener: RepositoryRefreshManager.Listener

    internal lateinit var cacheAgeManager: RepositoryCacheAgeManager
    internal lateinit var refreshManager: RepositoryRefreshManager
    internal lateinit var teller: Teller

    internal constructor(schedulersProvider: SchedulersProvider,
                         cacheAgeManager: RepositoryCacheAgeManager,
                         refreshManager: RepositoryRefreshManager,
                         taskExecutor: TaskExecutor,
                         refreshManagerListener: RepositoryRefreshManager.Listener,
                         teller: Teller) {
        init(schedulersProvider, cacheAgeManager, refreshManager, taskExecutor, refreshManagerListener, teller)
    }

    /**
     * Used for testing purposes to initialize the state of a [TellerRepository] subclass instance.
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
     * When this property is set, the [TellerRepository] instance will begin to observe the cache by loading the cache from the device and checking if it needs to fetch fresh cache from the network. All of the work will be done for you.
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
        set(newRequirements) {
            teller.assertNotLimitedFunctionality()
            assertNotDisposed()

            field?.let { oldValue ->
                refreshManager.cancelTasksForRepository(oldValue.tag, refreshManagerListener)
                stopObservingCache()
            }

            field = newRequirements

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

    /**
     * Have thought about making this function protected so that subclass can dispose of old observable to restart with a new one but always decide to keep it private. I can always conclude with, "Observables should be able to react to changes and stream data of any type". So if you need to change the cache you're observing for some reason, perhaps use a Subject or something. Design of your subclass may solve this problem.
     */
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
        /**
         * Do not throw an exception if disposed (such as with [assertNotDisposed]), as [performRefresh] is used internally. This check is mostly to prevent calling refresh if the observed cache triggers an onNext call on a different thread while the repository is disposing.
         */
        if (disposed) return

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
     * How to begin observing the state of the cache for this [TellerRepository].
     *
     * Teller will automatically perform a [refresh] if the cache does not exist or is too old. You will get notified anytime that the state of the cache changes.
     *
     * @throws RuntimeException If calling after calling [dispose].
     * @throws TellerLimitedFunctionalityException If calling when initializing Teller via [Teller.initUnitTesting].
     */
    fun observe(): Observable<CacheState<CACHE>> {
        teller.assertNotLimitedFunctionality()
        assertNotDisposed()

        if (requirements != null) {
            // Trigger a refresh to help keep cache up-to-date.
            performRefresh()
        }

        return currentStateOfCache.asObservable()
    }

    /**
     * Only exists because need to override this in [TellerPagingRepository], but don't want public subclasses to be able to override. [See](https://github.com/levibostian/Teller-Android/issues/57) for how this function could be removed because we would only have internal subclasses.
     */
    internal open fun _dispose() {
        teller.assertNotLimitedFunctionality()

        if (disposed) return
        disposed = true

        requirements?.let {
            refreshManager.cancelTasksForRepository(it.tag, refreshManagerListener)
        }

        currentStateOfCache.subject.onComplete()

        stopObservingCache()
    }

    /**
     * Dispose of the [TellerRepository] to shut down observing of the cache and stops refresh tasks if they have begun.
     *
     * Do this in onDestroy() of your Fragment or Activity, for example.
     *
     * After calling [dispose], your [TellerRepository] instance is useless. Calling any function on the instance in the future will result in a [RuntimeException].
     *
     * @throws TellerLimitedFunctionalityException If calling when initializing Teller via [Teller.initUnitTesting].
     */
    @Synchronized
    fun dispose() {
        this._dispose()
    }

    private fun stopObservingCache() {
        observeCacheDisposeBag.clear()
        observeCacheDisposeBag = CompositeDisposable()
    }

    /**
     * Only exists because need to override this in [TellerPagingRepository], but don't want public subclasses to be able to override. [See](https://github.com/levibostian/Teller-Android/issues/57) for how this function could be removed because we would only have internal subclasses.
     */
    internal open fun _refresh(force: Boolean, requirements: GET_CACHE_REQUIREMENTS): Single<RefreshResult> {
        teller.assertNotLimitedFunctionality()
        assertNotDisposed()

        return getRefresh(force, requirements)
    }

    /**
     * Manually perform a refresh of the cache.
     *
     * Ideal in these scenarios:
     * 1. User indicates in the UI they would like to check for new cache. Example: `UIRefreshControl` in a `UITableView` indicating to refresh.
     * 2. Keep app cache up-to-date at all times through a background job.
     *
     * @throws IllegalStateException If [requirements] have not yet been set for the [TellerRepository]. [TellerRepository] cannot refresh it it does not know what to refresh.
     * @throws RuntimeException If calling after calling [dispose].
     * @throws TellerLimitedFunctionalityException If calling when initializing Teller via [Teller.initUnitTesting].
     */
    @Throws(IllegalStateException::class)
    fun refresh(force: Boolean): Single<RefreshResult> {
        teller.assertNotLimitedFunctionality()

        val requirements = this.requirements ?: throw IllegalStateException("You need to set requirements before calling refresh.")

        return _refresh(force, requirements)
    }

    internal fun getRefresh(force: Boolean, requirements: GET_CACHE_REQUIREMENTS): Single<RefreshResult> {
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
            // It's important to update the last time fetched *after* a successful save in case it fails. However, make sure that the observer of the cache does not kick off an update until after the last successful fetch is updated.
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
     * Save the new cache [cache] to whatever storage method [TellerRepository] chooses.
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
     * @property tag Unique tag that drives the behavior of a [TellerRepository]. The tag needs to describe (1) the type of cache being stored (example: friend, friend request, song, user profile, etc) and (2) identity the fetch call to obtain this cache. Example: "FriendRequests_page1" for paging, "UserProfile_user2332" for a query param. Teller uses this [tag] to determine how old some particular cache is. If it's too old, new cache will be fetched.
     */
    interface GetCacheRequirements {
        var tag: GetCacheRequirementsTag
    }

    /**
     * When a [TellerRepository.fetchFreshCache] task is performed, Teller needs to know if the fetch request is considered to be a success or a failure.
     */
    class FetchResponse<FETCH_RESPONSE: DataSourceFetchResponse> private constructor(val response: FETCH_RESPONSE? = null,
                                                                                     val failure: Throwable? = null) {

        companion object {
            fun <FETCH_RESPONSE: DataSourceFetchResponse> success(response: FETCH_RESPONSE): FetchResponse<FETCH_RESPONSE> {
                return FetchResponse(response = response)
            }

            fun <FETCH_RESPONSE: DataSourceFetchResponse> fail(throwable: Throwable): FetchResponse<FETCH_RESPONSE> {
                return FetchResponse(failure = throwable)
            }
        }

        fun isSuccessful(): Boolean = response != null

        fun isFailure(): Boolean = failure != null
    }

    /**
     * Result object of a call to [TellerRepository.refresh]. Understand if a refresh call was successful, not successful, or skipped for some reason.
     */
    class RefreshResult private constructor(val successful: Boolean = false,
                                            val failedError: Throwable? = null,
                                            val skipped: SkippedReason? = null) {

        /**
         * Used for testing purposes to create instances of [CacheState].
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
         * If a [TellerRepository.refresh] task was skipped, compare the [skipped] property with the enum cases below to determine why the refresh task was skipped.
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
    private inner class RefreshManagerListener: RepositoryRefreshManager.Listener {

        override fun refreshBegin(tag: GetCacheRequirementsTag) {
            this@TellerRepository.refreshBegin(tag)
        }

        override fun <RefreshResponseType: DataSourceFetchResponse> refreshComplete(tag: GetCacheRequirementsTag, response: FetchResponse<RefreshResponseType>) {
            @Suppress("UNCHECKED_CAST") val fetchResponse = response as FetchResponse<FETCH_RESPONSE>
            this@TellerRepository.refreshComplete(tag, fetchResponse)
        }
    }

}