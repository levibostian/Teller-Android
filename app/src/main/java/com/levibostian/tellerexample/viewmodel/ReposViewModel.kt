package com.levibostian.tellerexample.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.LiveDataReactiveStreams
import android.arch.lifecycle.ViewModel
import com.levibostian.teller.datastate.OnlineDataState
import com.levibostian.tellerexample.model.db.AppDatabase
import com.levibostian.tellerexample.model.RepoModel
import com.levibostian.tellerexample.repository.ReposRepository
import com.levibostian.tellerexample.service.GitHubService
import io.reactivex.BackpressureStrategy
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class ReposViewModel: ViewModel() {

    private lateinit var reposRepository: ReposRepository

    fun init(service: GitHubService, db: AppDatabase) {
        reposRepository = ReposRepository(service, db)
    }

    fun setUsername(username: String) {
        reposRepository.requirements = ReposRepository.GetReposRequirements(username)
    }

    fun observeRepos(): LiveData<OnlineDataState<List<RepoModel>>> {
        return LiveDataReactiveStreams.fromPublisher(reposRepository.observe()
                .toFlowable(BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()))
    }

    fun dispose() {
        reposRepository.dispose()
    }

}
