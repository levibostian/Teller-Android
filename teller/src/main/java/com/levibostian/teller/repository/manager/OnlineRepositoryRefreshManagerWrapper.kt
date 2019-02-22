package com.levibostian.teller.repository.manager

import com.levibostian.teller.repository.GetCacheRequirementsTag
import com.levibostian.teller.repository.OnlineRepository
import io.reactivex.Single

/**
 * Exists to allow testing [OnlineRepositoryRefreshManager]. Kotlin `object`s cannot be mocked so this class exists to pass in for the Teller library implementation where a real instance of [OnlineRepositoryRefreshManager] is required.
 */
internal class OnlineRepositoryRefreshManagerWrapper: OnlineRepositoryRefreshManager {

    /**
     * Delegate call to singleton.
     *
     * @see SharedOnlineRepositoryRefreshManager.refresh
     */
    override fun <RefreshResponseType: Any> refresh(task: Single<OnlineRepository.FetchResponse<RefreshResponseType>>, tag: GetCacheRequirementsTag, repository: OnlineRepositoryRefreshManager.Listener): Single<OnlineRepository.RefreshResult> {
        return SharedOnlineRepositoryRefreshManager.refresh(task, tag, repository)
    }

    /**
     * Delegate call to singleton.
     *
     * @see SharedOnlineRepositoryRefreshManager.cancelTasksForRepository
     */
    override fun cancelTasksForRepository(tag: GetCacheRequirementsTag, repository: OnlineRepositoryRefreshManager.Listener) {
        return SharedOnlineRepositoryRefreshManager.cancelTasksForRepository(tag, repository)
    }

}