package com.levibostian.tellerexample.repository

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.error.ServerNotAvailableException
import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.tellerexample.dao.ReposDao
import com.levibostian.tellerexample.model.RepoModel
import com.levibostian.tellerexample.model.db.AppDatabase
import com.levibostian.tellerexample.rule.TellerUnitTestRule
import com.levibostian.tellerexample.service.GitHubService
import com.levibostian.tellerexample.service.provider.SchedulersProvider
import com.levibostian.tellerexample.util.FakeDataUtil
import com.levibostian.tellerexample.util.TestDependencyUtil
import com.nhaarman.mockito_kotlin.*
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.MediaType
import okhttp3.ResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.Response
import retrofit2.adapter.rxjava2.Result
import com.levibostian.tellerexample.util.AssertionsUtil.Companion.check
import io.reactivex.schedulers.Schedulers

@RunWith(MockitoJUnitRunner::class)
class ReposRepositoryTest {

    @Mock private lateinit var db: AppDatabase
    @Mock private lateinit var schedulersProvider: SchedulersProvider
    @Mock private lateinit var reposDao: ReposDao

    @get:Rule val rule = InstantTaskExecutorRule()
    @get:Rule val tellerRule = TellerUnitTestRule()

    private val defaultGithubUsername = FakeDataUtil.githubUsername
    private val defaultReposRequirements = ReposRepository.GetReposRequirements(defaultGithubUsername)

    private lateinit var mockServer: MockWebServer
    private lateinit var service: GitHubService

    private lateinit var repository: ReposRepository

    @Before
    fun setUp() {
        whenever(schedulersProvider.io()).thenReturn(Schedulers.trampoline())
        whenever(schedulersProvider.mainThread()).thenReturn(Schedulers.trampoline())
        whenever(db.reposDao()).thenReturn(reposDao)

        mockServer = MockWebServer()
        mockServer.start()

        service = TestDependencyUtil.testServiceInstance(mockServer.url("/"))

        repository = ReposRepository(service, db, schedulersProvider)
    }

    @After
    fun teardown() {
        mockServer.shutdown()
    }

    @Test
    fun fetchFreshCache_serverNotAvailableResponse_expectFailedFetch() {
        mockServer.enqueue(MockResponse().setResponseCode(500))

        repository.fetchFreshCache(ReposRepository.GetReposRequirements(defaultGithubUsername))
                .test()
                .assertComplete()
                .assertValue(check { fetchResponse ->
                    assertThat(fetchResponse.isFailure()).isTrue()
                    assertThat(fetchResponse.failure).isInstanceOf(ServerNotAvailableException::class.java)
                })
                .dispose()
    }

    @Test
    fun saveCache_insertRepos() {
        val cache = listOf(RepoModel())

        repository.saveCache(cache, defaultReposRequirements)

        verify(reposDao).insertRepos(cache)
    }

    @Test
    fun observeCache_queryOnBackgroundThread() {
        whenever(reposDao.observeReposForUser(defaultGithubUsername)).thenReturn(Observable.never())

        repository.observeCache(defaultReposRequirements)
                .test()
                .assertNotComplete()
                .dispose()

        verify(schedulersProvider).io()
        verify(schedulersProvider).mainThread()
    }

    @Test
    fun isCacheEmpty_expectEmpty() {
        assertThat(repository.isCacheEmpty(emptyList(), defaultReposRequirements)).isTrue()
    }

    @Test
    fun isCacheEmpty_expectNotEmpty() {
        assertThat(repository.isCacheEmpty(listOf(RepoModel()), defaultReposRequirements)).isFalse()
    }

}