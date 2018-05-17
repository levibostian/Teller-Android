package com.levibostian.tellerexample.viewmodel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.LiveDataReactiveStreams
import android.arch.lifecycle.ViewModel
import android.content.Context
import com.levibostian.teller.datastate.LocalDataState
import com.levibostian.teller.datastate.OnlineDataState
import com.levibostian.tellerexample.model.AppDatabase
import com.levibostian.tellerexample.model.RepoModel
import com.levibostian.tellerexample.repository.GitHubUsernameRepository
import com.levibostian.tellerexample.repository.ReposRepository
import com.levibostian.tellerexample.service.GitHubService
import io.reactivex.BackpressureStrategy

class GitHubUsernameViewModel: ViewModel() {

    private lateinit var repository: GitHubUsernameRepository

    fun init(context: Context) {
        repository = GitHubUsernameRepository(context)
    }

    fun setUsername(username: String) {
        repository.saveData(username)
    }

    fun observeUsername(): LiveData<LocalDataState<String>> {
        return LiveDataReactiveStreams.fromPublisher(repository.observe().toFlowable(BackpressureStrategy.LATEST))
    }

}
