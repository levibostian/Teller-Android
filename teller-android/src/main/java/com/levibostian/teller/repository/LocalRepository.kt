package com.levibostian.teller.repository

import android.os.AsyncTask
import com.levibostian.teller.Teller
import com.levibostian.teller.cachestate.LocalCacheState
import com.levibostian.teller.error.TellerLimitedFunctionalityException
import com.levibostian.teller.extensions.plusAssign
import com.levibostian.teller.provider.SchedulersProvider
import com.levibostian.teller.provider.TellerSchedulersProvider
import com.levibostian.teller.subject.LocalCacheStateBehaviorSubject
import com.levibostian.teller.util.TaskExecutor
import com.levibostian.teller.util.TellerTaskExecutor
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

typealias LocalRepositoryCache = Any

/**
 * Teller repository that manages cache that is obtained and stored on the local device.
 *
 * Using [LocalRepository] is quite simple:
 * 1. Subclass [LocalRepository] for each of your cache response types
 * 2. Call [observe] to begin observing the current state of the cached response.
 * 3. Set [requirements] with an object used to querying cached response. Set [requirements] object as many times as you wish and have [observe] receive all of the changes.
 *
 * [LocalRepository] is thread safe. Actions called upon for [LocalRepository] can be performed on any thread.
 */
abstract class LocalRepository<CACHE: LocalRepositoryCache, GET_CACHE_REQUIREMENTS: LocalRepository.GetCacheRequirements> {

    constructor() {
        if (!Teller.shared.unitTesting) init()
    }

    private fun init(
            schedulersProvider: SchedulersProvider = TellerSchedulersProvider(),
            taskExecutor: TaskExecutor = TellerTaskExecutor(),
            teller: Teller = Teller.shared
    ) {
        this.schedulersProvider = schedulersProvider
        this.taskExecutor = taskExecutor
        this.teller = teller
    }

    internal constructor(schedulersProvider: SchedulersProvider,
                         taskExecutor: TaskExecutor,
                         teller: Teller) {
        init(schedulersProvider, taskExecutor, teller)
    }

    /**
     * Idea taken from [CompositeDisposable]. Use of volatile variable to mark object as no longer used.
     */
    @Volatile private var disposed = false

    private var currentStateOfCache: LocalCacheStateBehaviorSubject<CACHE> = LocalCacheStateBehaviorSubject()
    private var observeCacheDisposeBag: CompositeDisposable = CompositeDisposable()
    // Single thread executor to act as a "background queue".
    private val saveCacheExecutor = Executors.newSingleThreadExecutor()

    private lateinit var schedulersProvider: SchedulersProvider
    private lateinit var taskExecutor: TaskExecutor
    private lateinit var teller: Teller

    /**
     * Requirements needed to be able to load cache from the device.
     *
     * When this property is set, the [LocalRepository] instance will begin to observe the cache by loading the cache from the device.
     *
     * If the user decides to scroll to the bottom of a list, view a different user profile, or any other reason you need to change to observe a different piece of cache, just set [requirements] again.
     *
     * If requirements is set to null, we will stop observing the cache changes and reset the state of cache to none.
     *
     * @throws RuntimeException If calling after calling [dispose].
     * @throws TellerLimitedFunctionalityException If calling when initializing Teller via [Teller.initUnitTesting].
     */
    var requirements: GET_CACHE_REQUIREMENTS? = null
        set(value) {
            teller.assertNotLimitedFunctionality()
            assertNotDisposed()
            field = value

            val newRequirements = field
            if (newRequirements != null) {
                restartObservingCache(newRequirements)
            } else {
                currentStateOfCache.resetStateToNone()
            }
        }

    @Synchronized
    private fun restartObservingCache(requirements: GET_CACHE_REQUIREMENTS) {
        /**
         * You need to subscribe and observe on the UI thread because popular database solutions such as Realm, SQLite all have a "write on background, read on UI" approach. You cannot read on the background and send the read objects to the UI thread. So, we read on the UI.
         */
        taskExecutor.postUI {
            if (disposed) return@postUI

            stopObservingCache()

            observeCacheDisposeBag += observeCache(requirements)
                    .subscribeOn(schedulersProvider.main())
                    .observeOn(schedulersProvider.main())
                    .subscribe { cache ->
                        if (isCacheEmpty(cache, requirements)) {
                            currentStateOfCache.onNextEmpty(requirements)
                        } else {
                            currentStateOfCache.onNextCache(requirements, cache)
                        }
                    }
        }
    }

    /**
     * Dispose of the [LocalRepository] to shut down observing of cached response.
     *
     * Do this in onDestroy() of your Fragment or Activity, for example.
     *
     * @throws TellerLimitedFunctionalityException If calling when initializing Teller via [Teller.initUnitTesting].
     */
    fun dispose() {
        teller.assertNotLimitedFunctionality()
        if (disposed) return
        disposed = true

        currentStateOfCache.subject.onComplete()

        stopObservingCache()
    }

    private fun stopObservingCache() {
        observeCacheDisposeBag.clear()
        observeCacheDisposeBag = CompositeDisposable()
    }

    /**
     * Save new cache response.
     *
     * It is up to you to call [newCache] when you have new cache response to save. A good place to do this is in a ViewModel.
     *
     * This function will call [saveCache] for you on a background thread.
     *
     * This function will trigger a save to a background thread. To be notified on the new cache response, use [observe] to observe the state of response after it has been updated.
     *
     * @throws RuntimeException If calling after calling [dispose].
     * @throws TellerLimitedFunctionalityException If calling when initializing Teller via [Teller.initUnitTesting].
     */
    @Synchronized
    fun newCache(cache: CACHE, requirements: GET_CACHE_REQUIREMENTS) {
        teller.assertNotLimitedFunctionality()
        assertNotDisposed()

        SaveCacheAsyncTask(WeakReference(this), requirements).apply {
            executeOnExecutor(saveCacheExecutor, cache)
        }
    }

    /**
     * Get an observable that gets the current state of response and all future states.
     *
     * @throws RuntimeException If calling after calling [dispose].
     * @throws TellerLimitedFunctionalityException If calling when initializing Teller via [Teller.initUnitTesting].
     */
    fun observe(): Observable<LocalCacheState<CACHE>> {
        teller.assertNotLimitedFunctionality()
        assertNotDisposed()

        return currentStateOfCache.asObservable()
    }

    private fun assertNotDisposed() {
        if (disposed) throw RuntimeException("Cannot call after calling `dispose()`")
    }

    /**
     * Save new cache response to whatever storage method Repository chooses.
     *
     * It is up to you to call [saveCache] when you have new cache response to save. A good place to do this is in a ViewModel.
     *
     * **This will be called from background thread**
     */
    protected abstract fun saveCache(cache: CACHE, requirements: GET_CACHE_REQUIREMENTS)

    /**
     * This function should be setup to trigger anytime there is a response change. So if you were to call [saveCache], anyone observing the [Observable] returned here will get notified of a new update.
     *
     * **This will be called from UI thread**
     */
    protected abstract fun observeCache(requirements: GET_CACHE_REQUIREMENTS): Observable<CACHE>

    /**
     * Determines if cache is empty or not. This is used internally by Teller to determine if the cache from [observeCache] is empty or not to then pass to [LocalCacheState.deliverState] with the state of the cache.
     *
     * **This is called from same thread as [observeCache]**
     */
    protected abstract fun isCacheEmpty(cache: CACHE, requirements: GET_CACHE_REQUIREMENTS): Boolean

    interface GetCacheRequirements

    /**
     * [saveCache] on background thread via [AsyncTask].
     */
    private class SaveCacheAsyncTask<CACHE: LocalRepositoryCache, GET_CACHE_REQUIREMENTS: LocalRepository.GetCacheRequirements>(
            val repo: WeakReference<LocalRepository<CACHE, GET_CACHE_REQUIREMENTS>>,
            val requirements: GET_CACHE_REQUIREMENTS) : AsyncTask<CACHE, Void, Void>() {

        override fun doInBackground(vararg params: CACHE): Void? {
            val newCache: CACHE = params[0]

            repo.get()?.let { repo ->
                repo.stopObservingCache()
                repo.saveCache(newCache, requirements)
                repo.restartObservingCache(requirements)
            }
            return null
        }
    }

}