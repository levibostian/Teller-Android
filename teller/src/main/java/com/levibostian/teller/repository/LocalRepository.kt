package com.levibostian.teller.repository

import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import com.levibostian.teller.datastate.LocalDataState
import com.levibostian.teller.extensions.plusAssign
import com.levibostian.teller.provider.SchedulersProvider
import com.levibostian.teller.provider.TellerSchedulersProvider
import com.levibostian.teller.subject.LocalDataStateCompoundBehaviorSubject
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.Executors

/**
 * Teller repository that manages cache that is obtained and stored on the local device.
 *
 * Using [LocalRepository] is quite simple:
 * 1. Subclass [LocalRepository] for each of your cache data types
 * 2. Call [observe] to begin observing the current state of the cached data.
 * 3. Set [requirements] with an object used to querying cached data. Set [requirements] object as many times as you wish and have [observe] receive all of the changes.
 *
 * [LocalRepository] is thread safe. Actions called upon for [LocalRepository] can be performed on any thread.
 */
abstract class LocalRepository<CACHE: Any, GET_CACHE_REQUIREMENTS: LocalRepository.GetCacheRequirements> {

    constructor() {
        schedulersProvider = TellerSchedulersProvider()
    }

    internal constructor(schedulersProvider: SchedulersProvider) {
        this.schedulersProvider = schedulersProvider
    }

    private var currentStateOfCache: LocalDataStateCompoundBehaviorSubject<CACHE> = LocalDataStateCompoundBehaviorSubject()
    private var observeCacheDisposeBag: CompositeDisposable = CompositeDisposable()
    // Single thread executor to act as a "background queue".
    private val saveCacheExecutor = Executors.newSingleThreadExecutor()

    private val schedulersProvider: SchedulersProvider

    var requirements: GET_CACHE_REQUIREMENTS? = null
        set(value) {
            field = value

            val newValue = field
            if (newValue != null) {
                restartObservingCachedData(newValue)
            } else {
                currentStateOfCache.resetStateToNone()
            }
        }

    @Synchronized
    private fun restartObservingCachedData(requirements: GET_CACHE_REQUIREMENTS) {
        // I need to subscribe and observe on the UI thread because popular database solutions such as Realm, SQLite all have a "write on background, read on UI" approach. You cannot read on the background and send the read objects to the UI thread. So, we read on the UI.
        Handler(Looper.getMainLooper()).post {
            stopObservingCache()

            observeCacheDisposeBag += observeCache(requirements)
                    .subscribeOn(schedulersProvider.main())
                    .observeOn(schedulersProvider.main())
                    .subscribe { cache ->
                        if (isCacheEmpty(cache, requirements)) {
                            currentStateOfCache.onNextEmpty()
                        } else {
                            currentStateOfCache.onNextData(cache)
                        }
                    }
        }
    }

    /**
     * Dispose of the [LocalRepository] to shut down observing of cached data.
     *
     * Do this in onDestroy() of your Fragment or Activity, for example.
     */
    fun dispose() {
        currentStateOfCache.subject.onComplete()

        stopObservingCache()
    }

    private fun stopObservingCache() {
        observeCacheDisposeBag.clear()
        observeCacheDisposeBag = CompositeDisposable()
    }

    /**
     * Save new cache data.
     *
     * It is up to you to call [newCache] when you have new cache data to save. A good place to do this is in a ViewModel.
     *
     * This function will call [saveCache] for you on a background thread.
     *
     * This function will trigger a save to a background thread. To be notified on the new cache data, use [observe] to observe the state of data after it has been updated.
     */
    @Synchronized
    fun newCache(cache: CACHE, requirements: GET_CACHE_REQUIREMENTS) {
        SaveCacheAsyncTask(WeakReference(this), requirements).apply {
            executeOnExecutor(saveCacheExecutor, cache)
        }
    }

    /**
     * Get an observable that gets the current state of data and all future states.
     */
    fun observe(): Observable<LocalDataState<CACHE>> {
        return currentStateOfCache.asObservable()
    }

    /**
     * Save new cache data to whatever storage method Repository chooses.
     *
     * It is up to you to call [saveCache] when you have new cache data to save. A good place to do this is in a ViewModel.
     *
     * **This will be called from background thread**
     */
    protected abstract fun saveCache(cache: CACHE, requirements: GET_CACHE_REQUIREMENTS)

    /**
     * This function should be setup to trigger anytime there is a data change. So if you were to call [saveCache], anyone observing the [Observable] returned here will get notified of a new update.
     *
     * **This will be called from UI thread**
     */
    protected abstract fun observeCache(requirements: GET_CACHE_REQUIREMENTS): Observable<CACHE>

    /**
     * Determines if cache is empty or not. This is used internally by Teller to determine if the cache from [observeCache] is empty or not to then pass to [LocalDataState.deliverState] with the state of the cache.
     *
     * **This is called from same thread as [observeCache]**
     */
    protected abstract fun isCacheEmpty(cache: CACHE, requirements: GET_CACHE_REQUIREMENTS): Boolean

    interface GetCacheRequirements

    /**
     * [saveCache] on background thread via [AsyncTask].
     */
    private class SaveCacheAsyncTask<CACHE: Any, GET_CACHE_REQUIREMENTS: LocalRepository.GetCacheRequirements>(
            val repo: WeakReference<LocalRepository<CACHE, GET_CACHE_REQUIREMENTS>>,
            val requirements: GET_CACHE_REQUIREMENTS) : AsyncTask<CACHE, Void, Void>() {

        override fun doInBackground(vararg params: CACHE): Void? {
            val newCache: CACHE = params[0]

            repo.get()?.let { repo ->
                repo.stopObservingCache()
                repo.saveCache(newCache, requirements)
                repo.restartObservingCachedData(requirements)
            }
            return null
        }
    }

}