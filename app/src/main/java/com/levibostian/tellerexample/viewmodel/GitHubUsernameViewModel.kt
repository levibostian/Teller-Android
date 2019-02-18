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

    private val requirements = GitHubUsernameRepository.GetRequirements()

    fun init(context: Context) {
        repository = GitHubUsernameRepository(context).apply {
            requirements = this@GitHubUsernameViewModel.requirements
        }
    }

    fun setUsername(username: String) {
        repository.newCache(username, requirements)
    }

    fun observeUsername(): LiveData<LocalDataState<String>> {
        return LiveDataReactiveStreams.fromPublisher(repository.observe().toFlowable(BackpressureStrategy.LATEST))
    }

}
