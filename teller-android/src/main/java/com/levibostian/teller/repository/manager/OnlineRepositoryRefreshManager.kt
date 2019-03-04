package com.levibostian.teller.repository.manager

import com.levibostian.teller.repository.GetCacheRequirementsTag
import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.teller.repository.OnlineRepositoryFetchResponse
import io.reactivex.Single

/**
 * Manager responsible for managing what is run when [OnlineRepository.refresh] is called. This manager decides if refresh calls are shared between instances, if previous [OnlineRepository.refresh] calls are cancelled before, etc.
 *
 * @see SharedOnlineRepositoryRefreshManager
 * @see OnlineRepositoryRefreshManagerWrapper
 */
internal interface OnlineRepositoryRefreshManager {

    /**
     * Listener to get notified when a refresh call has begun and when it completes. This is different from the Observer returned from [refresh]. [Listener] is mostly intended for [OnlineRepository] to know when a refresh is begun and complete so it can notify observers of fetch status and to save new cache response.
     */
    interface Listener {
        fun refreshBegin(tag: GetCacheRequirementsTag)
        fun <RefreshResponseType: OnlineRepositoryFetchResponse> refreshComplete(tag: GetCacheRequirementsTag, response: OnlineRepository.FetchResponse<RefreshResponseType>)
    }

    /**
     * Start a refresh call.
     *
     * Manager decides how refresh is operated. If that means starting a new refresh call each time function is called, sharing the refresh calls with other objects, it is up to the manager.
     */
    fun <RefreshResponseType: OnlineRepositoryFetchResponse> refresh(task: Single<OnlineRepository.FetchResponse<RefreshResponseType>>, tag: GetCacheRequirementsTag, repository: Listener): Single<OnlineRepository.RefreshResult>

    /**
     * A listener no longer cares about a refresh call. Perhaps the user wants to refresh cache with different requirements?
     *
     * Implementation decides what "cancel" means. Removing listener, canceling the fetch request, all depends on the manager's implementation.
     */
    fun cancelTasksForRepository(tag: GetCacheRequirementsTag, repository: Listener)

}