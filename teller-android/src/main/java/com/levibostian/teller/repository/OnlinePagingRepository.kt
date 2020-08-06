package com.levibostian.teller.repository

import com.levibostian.teller.extensions.plusAssign
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable

/**
 * [OnlineRepository] that is designed specifically for paging.
 */
abstract class OnlinePagingRepository<CACHE: OnlineRepositoryCache, PAGING_REQUIREMENTS: OnlinePagingRepository.PagingRequirements, GET_CACHE_REQUIREMENTS: OnlineRepository.GetCacheRequirements, FETCH_RESPONSE: OnlineRepositoryFetchResponse>(val firstPageRequirements: PAGING_REQUIREMENTS): OnlineRepository<CACHE, GET_CACHE_REQUIREMENTS, FETCH_RESPONSE>() {

    private var disposeBag: CompositeDisposable = CompositeDisposable()

    /**
     * Requirements specifically to determine what page of data we are requesting.
     *
     * *Note: When the paging requirements are set, a fetch will be executed no matter what. No check if data too old. Checking if a cache is old is only done by Teller for the first page of the cache. All future pages are fetched because the way pagination works is to delete all of the cache except the first page of the cache so if you set the page requirements, that means the user has scrolled. Because they have scrolled, we need to fetch the next page and put into the cache to show.*
     */
    var pagingRequirements: PAGING_REQUIREMENTS
        set(newValue) {
            _pagingRequirements = newValue

            this.requirements?.let { requirements ->
                // no need to check if loading the first page to avoid too many network calls because this set() function is only called when setting the paging requirements as you scroll. The first page this will not be executed so that avoids calling refresh even when the first page of data is not too old according to teller.
                getRefresh(true, requirements)
                        .subscribeOn(schedulersProvider.io())
                        .subscribe()
            }
        }
        get() = _pagingRequirements

    private val isFirstPage: Boolean
        get() = pagingRequirements == firstPageRequirements

    /**
     * Used to hold the value of [pagingRequirements], without actions. [pagingRequirements] is meant to trigger actions when set. This is the backing property for it.
     */
    @Volatile internal var _pagingRequirements: PAGING_REQUIREMENTS = firstPageRequirements

    /**
     * Repository does what it needs in order to fetch fresh cache. Probably call network API.
     *
     * **Called on a background thread.**
     */
    abstract fun fetchFreshCache(requirements: GET_CACHE_REQUIREMENTS, pagingRequirements: PAGING_REQUIREMENTS): Single<FetchResponse<FETCH_RESPONSE>>

    override fun fetchFreshCache(requirements: GET_CACHE_REQUIREMENTS): Single<FetchResponse<FETCH_RESPONSE>> {
        return fetchFreshCache(requirements, this.pagingRequirements)
    }

    override fun _refresh(force: Boolean, requirements: GET_CACHE_REQUIREMENTS): Single<RefreshResult> {
        var first: Completable = Completable.complete()

        /**
         * If you are forcing a refresh either:
         * 1. Doing a pull-to-refresh in the UI in which case you want to get the first page anyway.
         * 2. Doing a background refresh in which case, you only care about saving the new first page anyway.
         *
         * For these reasons, we are going to reset the paging requirements for you and also delete old cache data.
         */
        if (force) {
            _pagingRequirements = firstPageRequirements
            first = deleteOldCache(requirements, persistFirstPage = true)
        }

        return first
                .andThen(super._refresh(force, requirements))
    }

    override fun _dispose() {
        disposeBag.clear()

        super._dispose()
    }

    /**
     * Called by the repository to ask you to delete the cached data that is beyond the first page of the cache. If you, for example, have 150 items of data saved to the device as a cache (3 pages of 50 items per page), keep the first 50 items (because 50 is the page size) and delete the last 100 items.
     *
     * This is done to prevent the scenario happening where the app opens up in the future, queries all of the old cached data, the user scrolls to the end of the list, and the repository is asked to go to the next page but if the cache size is larger then the page size, then the next page will not be accurate. If your page size is 50 but you scroll 150 items of an old cache, you are telling the repository to go to page 2 with the last cache item being at position 150 when that's actually the end of page 3 because the page size is 50.
     *
     * **Called on main thread** to match what [observeCache] is called on. Because this is an async function, you can move into another thread if you need.
     */
    protected abstract fun deleteOldCache(requirements: GET_CACHE_REQUIREMENTS, persistFirstPage: Boolean): Completable

    /**
     * Save the new cache [cache] to whatever storage method [OnlineRepository] chooses.
     *
     * **Called on a background thread.**
     */
    protected abstract fun saveCache(cache: FETCH_RESPONSE, requirements: GET_CACHE_REQUIREMENTS, pagingRequirements: PAGING_REQUIREMENTS)

    override fun saveCache(cache: FETCH_RESPONSE, requirements: GET_CACHE_REQUIREMENTS) {
        // saveCache is called after successfully fetching pages of data. If we just got the first page of data, we should delete the old cache before saving the new. That way you don't have 2 pages of cache: 1 brand new and one that is old.
        val first: Completable = if (isFirstPage) deleteOldCache(requirements, persistFirstPage = false) else Completable.complete()

        disposeBag += first
                .subscribe {
                    saveCache(cache, requirements, pagingRequirements)
                }
    }

    /**
     * Get existing cache saved on the device if it exists. If no cache exists, return an empty response set in the Observable and return true in [isCacheEmpty]. **Do not** return nil or an Observable with nil as a value as this will cause an exception.
     *
     * This function is only called after cache has been fetched successfully from [fetchFreshCache].
     *
     * **Called on main UI thread.**
     */
    protected abstract fun observeCache(requirements: GET_CACHE_REQUIREMENTS, pagingRequirements: PAGING_REQUIREMENTS): Observable<CACHE>

    override fun observeCache(requirements: GET_CACHE_REQUIREMENTS): Observable<CACHE> {
        /**
         * If you are observing the cache, you are beginning to read the cache. You have not yet shown the cache in the UI yet. Because of that, we need to delete the old cache beyond the first page so that the page number aligns with how much data is in the cache.
         * But, deleting data needs to be asynchronous in case you need to run it in the UI thread or background thread. You choose. So, delete the old cache and then begin observing.
         *
         * Only delete if paging requirements is set to initial. If the paging requirements has changed, it means the user has scrolled in the app and you don't want to delete that data while they are viewing it.
         */
        val first: Completable = if (isFirstPage) deleteOldCache(requirements, persistFirstPage = true) else Completable.complete()

        return first
                .andThen(observeCache(requirements, pagingRequirements))
    }

    /**
     * Used to determine if cache is empty or not.
     *
     * **Called on main UI thread.**
     */
    protected abstract fun isCacheEmpty(cache: CACHE, requirements: GET_CACHE_REQUIREMENTS, pagingRequirements: PAGING_REQUIREMENTS): Boolean

    override fun isCacheEmpty(cache: CACHE, requirements: GET_CACHE_REQUIREMENTS): Boolean {
        return isCacheEmpty(cache, requirements, pagingRequirements)
    }

    interface PagingRequirements

}