package com.levibostian.tellerexample.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import com.levibostian.teller.cachestate.OnlineCacheState
import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.tellerexample.model.IssueCommentModel
import com.levibostian.tellerexample.model.db.AppDatabase
import com.levibostian.tellerexample.model.RepoModel
import com.levibostian.tellerexample.repository.IssueCommentsRepository
import com.levibostian.tellerexample.repository.ReposRepository
import com.levibostian.tellerexample.service.GitHubService
import com.levibostian.tellerexample.service.provider.SchedulersProvider
import io.reactivex.BackpressureStrategy
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class IssueCommentsViewModel: ViewModel() {

    private lateinit var repository: IssueCommentsRepository
    private lateinit var schedulersProvider: SchedulersProvider

    fun init(repository: IssueCommentsRepository, schedulersProvider: SchedulersProvider) {
        this.repository = repository
        this.schedulersProvider = schedulersProvider

        // This issue has a lot of comments: https://github.com/electron/electron/issues/673
        repository.requirements = IssueCommentsRepository.Requirements("electron", "electron", 673)
    }

    fun init(service: GitHubService, db: AppDatabase, schedulersProvider: SchedulersProvider) {
        init(IssueCommentsRepository(service, db, schedulersProvider), schedulersProvider)
    }

    fun observeIssueComments(): LiveData<OnlineCacheState<PagedList<IssueCommentModel>>> {
        return LiveDataReactiveStreams.fromPublisher(repository.observe()
                .toFlowable(BackpressureStrategy.LATEST)
                .subscribeOn(schedulersProvider.io())
                .observeOn(schedulersProvider.mainThread()))
    }

    override fun onCleared() {
        repository.dispose()

        super.onCleared()
    }

    fun refresh(): Single<OnlineRepository.RefreshResult> {
        return repository.refresh(force = true)
    }

}
