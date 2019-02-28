package com.levibostian.tellerexample.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import com.levibostian.teller.cachestate.OnlineCacheState
import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.tellerexample.model.db.AppDatabase
import com.levibostian.tellerexample.model.RepoModel
import com.levibostian.tellerexample.repository.ReposRepository
import com.levibostian.tellerexample.service.GitHubService
import com.levibostian.tellerexample.service.provider.SchedulersProvider
import io.reactivex.BackpressureStrategy
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class ReposViewModel: ViewModel() {

    private lateinit var reposRepository: ReposRepository
    private lateinit var schedulersProvider: SchedulersProvider

    fun init(reposRepository: ReposRepository, schedulersProvider: SchedulersProvider) {
        this.reposRepository = reposRepository
        this.schedulersProvider = schedulersProvider
    }

    fun init(service: GitHubService, db: AppDatabase, schedulersProvider: SchedulersProvider) {
        init(ReposRepository(service, db), schedulersProvider)
    }

    fun setUsername(username: String) {
        reposRepository.requirements = ReposRepository.GetReposRequirements(username)
    }

    fun observeRepos(): LiveData<OnlineCacheState<List<RepoModel>>> {
        return LiveDataReactiveStreams.fromPublisher(reposRepository.observe()
                .toFlowable(BackpressureStrategy.LATEST)
                .subscribeOn(schedulersProvider.io())
                .observeOn(schedulersProvider.mainThread()))
    }

    fun dispose() {
        reposRepository.dispose()
    }

    fun refresh(): Single<OnlineRepository.RefreshResult> {
        return reposRepository.refresh(force = true)
    }

}
