package com.levibostian.tellerexample.integration

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer

import org.junit.Test
import org.junit.runner.RunWith

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.cachestate.CacheState
import com.levibostian.teller.repository.TellerRepository
import com.levibostian.tellerexample.rule.TellerInitRule
import com.levibostian.teller.testing.extensions.cache
import com.levibostian.teller.testing.extensions.initState
import com.levibostian.tellerexample.model.RepoModel
import com.levibostian.tellerexample.model.db.AppDatabase
import com.levibostian.tellerexample.repository.ReposRepository
import com.levibostian.tellerexample.rule.MockitoInitRule
import com.levibostian.tellerexample.service.GitHubService
import com.levibostian.tellerexample.service.provider.AppSchedulersProvider
import com.levibostian.tellerexample.util.TestDependencyUtil
import com.levibostian.tellerexample.util.Wait
import com.levibostian.tellerexample.viewmodel.ReposViewModel
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import org.junit.After

import org.junit.Before
import org.junit.Rule
import org.mockito.Mock

@RunWith(AndroidJUnit4::class)
class ReposIntegrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    private lateinit var reposRepository: ReposRepository
    private lateinit var reposViewModel: ReposViewModel

    private lateinit var compositeDisposable: CompositeDisposable
    private lateinit var database: AppDatabase

    private val defaultGitHubUsername = "githubusername"
    private val defaultRequirements = ReposRepository.GetReposRequirements(defaultGitHubUsername)

    @Mock private lateinit var service: GitHubService

    @get:Rule val mockitoInitMocks = MockitoInitRule(this)
    @get:Rule val tellerInit = TellerInitRule()
    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        compositeDisposable = CompositeDisposable()
        database = TestDependencyUtil.testDbInstance(context)

        reposRepository = ReposRepository(service, database, AppSchedulersProvider())
        reposViewModel = ReposViewModel().apply {
            init(reposRepository, AppSchedulersProvider())
        }
    }

    @After
    fun teardown() {
        database.close()
        reposViewModel.dispose() // Very important or else tests pass individually but in suite fail as repo not disposed before next call.
        compositeDisposable.clear()
    }

    @Test
    fun observeRepos_cacheEmptyNotTooOld_expectGetEmptyRepos() {
        val wait = Wait.times(1)

        val setValues = TellerRepository.Testing.initState(reposRepository, defaultRequirements) {
            cacheEmpty {
                cacheNotTooOld()
            }
        }
        val expectObserveResult = CacheState.Testing.cache<List<RepoModel>>(defaultRequirements, setValues.lastFetched!!)

        reposRepository.requirements = defaultRequirements

        val observer = Observer<CacheState<List<RepoModel>>> { cacheState ->
            assertThat(cacheState).isEqualTo(expectObserveResult)

            wait.countDown()
        }
        reposViewModel.observeRepos().observeForever(observer)

        wait.await()

        reposViewModel.observeRepos().removeObserver(observer)
    }

    @Test
    fun observeRepos_cacheEmptyTooOld_expectGetEmptyReposAndFetching() {
        whenever(service.listRepos(defaultGitHubUsername)).thenReturn(Single.never())
        val wait = Wait.times(1)

        val setValues = TellerRepository.Testing.initState(reposRepository, defaultRequirements) {
            cacheEmpty {
                cacheTooOld()
            }
        }
        val expectObserveResult = CacheState.Testing.cache<List<RepoModel>>(defaultRequirements, setValues.lastFetched!!) {
            fetching()
        }

        reposRepository.requirements = defaultRequirements

        val observer = Observer<CacheState<List<RepoModel>>> { cacheState ->
            assertThat(cacheState).isEqualTo(expectObserveResult)

            wait.countDown()
        }
        reposViewModel.observeRepos().observeForever(observer)

        wait.await()

        reposViewModel.observeRepos().removeObserver(observer)
    }

}
