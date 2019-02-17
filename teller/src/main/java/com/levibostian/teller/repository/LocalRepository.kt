package com.levibostian.teller.repository

import com.levibostian.teller.datastate.LocalDataState
import com.levibostian.teller.extensions.plusAssign
import com.levibostian.teller.provider.SchedulersProvider
import com.levibostian.teller.provider.TellerSchedulersProvider
import com.levibostian.teller.subject.LocalDataStateCompoundBehaviorSubject
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

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

    private val schedulersProvider: SchedulersProvider

    var requirements: GET_CACHE_REQUIREMENTS? = null
        set(value) {
            field = value

            val newValue = field
            if (newValue != null) {
                beginObservingCachedData(newValue)
            } else {
                currentStateOfCache.resetStateToNone()
            }
        }

    private fun beginObservingCachedData(requirements: GET_CACHE_REQUIREMENTS) {
        stopObservingCache()

        // I need to subscribe and observe on the UI thread because popular database solutions such as Realm, SQLite all have a "write on background, read on UI" approach. You cannot read on the background and send the read objects to the UI thread. So, we read on the UI.
        Observable.fromCallable {
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
                .subscribeOn(schedulersProvider.main())
                .subscribe()
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
        Observable.fromCallable {
            stopObservingCache()

            saveCache(cache, requirements)

            beginObservingCachedData(requirements)
        }
                .subscribeOn(schedulersProvider.io())
                .subscribe()
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

}