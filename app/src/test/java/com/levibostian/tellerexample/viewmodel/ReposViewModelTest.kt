package com.levibostian.tellerexample.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.cachestate.CacheState
import com.levibostian.teller.repository.TellerRepository
import com.levibostian.teller.testing.extensions.cache
import com.levibostian.teller.testing.extensions.success
import com.levibostian.tellerexample.model.RepoModel
import com.levibostian.tellerexample.repository.ReposRepository
import com.levibostian.tellerexample.service.provider.SchedulersProvider
import com.nhaarman.mockito_kotlin.*
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class ReposViewModelTest {

    @Mock lateinit var reposRepository: ReposRepository
    @Mock lateinit var schedulers: SchedulersProvider
    @Mock private lateinit var reposObserver: Observer<CacheState<List<RepoModel>>>
    @Mock lateinit var requirements: ReposRepository.GetReposRequirements

    @get:Rule val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: ReposViewModel

    @Before
    fun setUp() {
        `when`(schedulers.io()).thenReturn(Schedulers.trampoline())
        `when`(schedulers.mainThread()).thenReturn(Schedulers.trampoline())
        viewModel = ReposViewModel().apply {
            init(reposRepository, schedulers)
        }
    }

    @Test
    fun `setUsername() - Requirements gets set on repo`() {
        val username = "username"
        viewModel.setUsername(username)

        val argumentCaptor = argumentCaptor<ReposRepository.GetReposRequirements>()
        verify(reposRepository).requirements = argumentCaptor.capture()

        argumentCaptor.apply {
            assertThat(firstValue.githubUsername).isEqualTo(username)
        }
    }

    @Test
    fun `observeRepos() - Observe repos from repository`() {
        val reposCache = CacheState.Testing.cache<List<RepoModel>>(requirements, Date()) {
            cache(listOf(RepoModel()))
        }
        `when`(reposRepository.observe()).thenReturn(Observable.just(reposCache))

        viewModel.observeRepos().observeForever(reposObserver)

        verify(reposObserver).onChanged(reposCache)
        verify(schedulers).io()
        verify(schedulers).mainThread()
    }

    @Test
    fun `refresh() - Refreshes from repository`() {
        val refreshResult = TellerRepository.RefreshResult.Testing.success()

        `when`(reposRepository.refresh(force = true)).thenReturn(Single.just(refreshResult))

        viewModel.refresh()
                .test()
                .await()
                .assertValues(refreshResult)

        verify(reposRepository).refresh(force = true)
    }

    @Test
    fun `dispose() - Dispose repository`() {
        viewModel.dispose()

        verify(reposRepository).dispose()
    }

}