package com.levibostian.tellerexample.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.levibostian.teller.cachestate.LocalCacheState
import com.levibostian.teller.repository.LocalRepository
import com.levibostian.tellerexample.repository.GitHubUsernameRepository
import com.nhaarman.mockito_kotlin.*
import io.reactivex.Observable
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class GitHubUsernameViewModelTest {

    @Mock lateinit var repository: GitHubUsernameRepository
    @Mock private lateinit var usernameObserver: Observer<LocalCacheState<String>>
    @Mock private lateinit var requirements: LocalRepository.GetCacheRequirements

    @get:Rule val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: GitHubUsernameViewModel

    @Before
    fun setUp() {
        viewModel = GitHubUsernameViewModel().apply {
            init(repository)
        }
    }

    @Test
    fun `setUsername() - Requirements gets set on repo`() {
        val username = "username"
        viewModel.setUsername(username)

        verify(repository).newCache(eq(username), isA())
    }

    @Test
    fun `observeUsername() - Observe username from repository`() {
        val cache = "cache"
        val cacheState = LocalCacheState.Testing.cache(requirements, cache)
        `when`(repository.observe()).thenReturn(Observable.just(cacheState))

        viewModel.observeUsername().observeForever(usernameObserver)

        verify(usernameObserver).onChanged(cacheState)
    }

    @Test
    fun `dispose() - Dispose repository`() {
        viewModel.dispose()

        verify(repository).dispose()
    }

}