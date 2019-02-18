package com.levibostian.teller.repository.manager

import com.levibostian.teller.repository.GetDataRequirementsTag
import com.levibostian.teller.repository.OnlineRepository
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.ReplaySubject
import java.lang.ref.WeakReference

/**
 * Exists to allow testing [OnlineRepositoryRefreshManager]. Kotlin `object`s cannot be mocked so this class exists to pass in for the Teller library implementation where a real instance of [OnlineRepositoryRefreshManager] is required.
 */
internal class OnlineRepositoryRefreshManagerWrapper: OnlineRepositoryRefreshManager {

    /**
     * Delegate call to singleton.
     *
     * @see SharedOnlineRepositoryRefreshManager.refresh
     */
    override fun <RefreshResultDataType : Any> refresh(task: Single<OnlineRepository.FetchResponse<RefreshResultDataType>>, tag: GetDataRequirementsTag, repository: OnlineRepositoryRefreshManager.Listener): Single<OnlineRepository.RefreshResult> {
        return SharedOnlineRepositoryRefreshManager.refresh(task, tag, repository)
    }

    /**
     * Delegate call to singleton.
     *
     * @see SharedOnlineRepositoryRefreshManager.cancelTasksForRepository
     */
    override fun cancelTasksForRepository(tag: GetDataRequirementsTag, repository: OnlineRepositoryRefreshManager.Listener) {
        return SharedOnlineRepositoryRefreshManager.cancelTasksForRepository(tag, repository)
    }

}