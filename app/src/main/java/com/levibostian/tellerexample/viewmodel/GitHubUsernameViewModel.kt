package com.levibostian.tellerexample.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.LiveDataReactiveStreams
import android.arch.lifecycle.ViewModel
import android.content.Context
import com.levibostian.teller.datastate.LocalDataState
import com.levibostian.tellerexample.repository.GitHubUsernameRepository
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
