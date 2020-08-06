package com.levibostian.teller.repository.manager

import com.levibostian.teller.repository.GetCacheRequirementsTag
import com.levibostian.teller.repository.TellerRepository
import com.levibostian.teller.repository.DataSourceFetchResponse
import io.reactivex.Single

/**
 * Exists to allow testing [RepositoryRefreshManager]. Kotlin `object`s cannot be mocked so this class exists to pass in for the Teller library implementation where a real instance of [RepositoryRefreshManager] is required.
 */
internal class RepositoryRefreshManagerWrapper: RepositoryRefreshManager {

    /**
     * Delegate call to singleton.
     *
     * @see SharedRepositoryRefreshManager.refresh
     */
    override fun <RefreshResponseType: DataSourceFetchResponse> refresh(task: Single<TellerRepository.FetchResponse<RefreshResponseType>>, tag: GetCacheRequirementsTag, repository: RepositoryRefreshManager.Listener): Single<TellerRepository.RefreshResult> {
        return SharedRepositoryRefreshManager.refresh(task, tag, repository)
    }

    /**
     * Delegate call to singleton.
     *
     * @see SharedRepositoryRefreshManager.cancelTasksForRepository
     */
    override fun cancelTasksForRepository(tag: GetCacheRequirementsTag, repository: RepositoryRefreshManager.Listener) {
        return SharedRepositoryRefreshManager.cancelTasksForRepository(tag, repository)
    }

}