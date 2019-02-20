package com.levibostian.teller.repository.manager

import com.levibostian.teller.repository.GetDataRequirementsTag
import com.levibostian.teller.repository.OnlineRepository
import io.reactivex.Single

/**
 * Manager responsible for managing what is run when [OnlineRepository.refresh] is called. This manager decides if refresh calls are shared between instances, if previous [OnlineRepository.refresh] calls are cancelled before, etc.
 *
 * @see SharedOnlineRepositoryRefreshManager
 * @see OnlineRepositoryRefreshManagerWrapper
 */
internal interface OnlineRepositoryRefreshManager {

    /**
     * Listener to get notified when a refresh call has begun and when it completes. This is different from the Observer returned from [refresh]. [Listener] is mostly intended for [OnlineRepository] to know when a refresh is begun and complete so it can notify observers of fetch status and to save new cache data.
     */
    interface Listener {
        fun refreshBegin(tag: GetDataRequirementsTag)
        fun <RefreshResultDataType: Any> refreshComplete(tag: GetDataRequirementsTag, response: OnlineRepository.FetchResponse<RefreshResultDataType>)
    }

    /**
     * Start a refresh call.
     *
     * Manager decides how refresh is operated. If that means starting a new refresh call each time function is called, sharing the refresh calls with other objects, it is up to the manager.
     */
    fun <RefreshResultDataType: Any> refresh(task: Single<OnlineRepository.FetchResponse<RefreshResultDataType>>, tag: GetDataRequirementsTag, repository: Listener): Single<OnlineRepository.RefreshResult>

    /**
     * A listener no longer cares about a refresh call. Perhaps the user wants to refresh data with different requirements?
     *
     * Implementation decides what "cancel" means. Removing listener, canceling the fetch request, all depends on the manager's implementation.
     */
    fun cancelTasksForRepository(tag: GetDataRequirementsTag, repository: Listener)

}