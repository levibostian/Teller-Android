package com.levibostian.tellerexample.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import com.levibostian.teller.cachestate.LocalCacheState
import com.levibostian.tellerexample.repository.GitHubUsernameRepository
import io.reactivex.BackpressureStrategy

class GitHubUsernameViewModel: ViewModel() {

    private lateinit var repository: GitHubUsernameRepository

    private val requirements = GitHubUsernameRepository.GitHubUsernameGetCacheRequirements()

    fun init(repository: GitHubUsernameRepository) {
        this.repository = repository
    }

    fun init(context: Context) {
        init(GitHubUsernameRepository(context).apply {
            requirements = this@GitHubUsernameViewModel.requirements
        })
    }

    fun setUsername(username: String) {
        repository.newCache(username, requirements)
    }

    fun observeUsername(): LiveData<LocalCacheState<String>> {
        return LiveDataReactiveStreams.fromPublisher(repository.observe().toFlowable(BackpressureStrategy.LATEST))
    }

    fun dispose() {
        repository.dispose()
    }

}
