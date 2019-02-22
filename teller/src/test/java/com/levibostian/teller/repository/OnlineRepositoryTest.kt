package com.levibostian.teller.repository

import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.extensions.plusAssign
import com.levibostian.teller.provider.SchedulersProvider
import com.levibostian.teller.repository.manager.OnlineRepositoryRefreshManager
import com.levibostian.teller.repository.manager.OnlineRepositoryCacheAgeManager
import org.junit.Test
import com.levibostian.teller.util.AssertionUtil.Companion.check
import com.levibostian.teller.util.TaskExecutor
import com.nhaarman.mockito_kotlin.any

import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import org.junit.Before
import org.mockito.Mockito.`when`
import io.reactivex.schedulers.Schedulers
import org.junit.After
import java.util.*
import kotlin.test.assertFailsWith

@RunWith(MockitoJUnitRunner::class)
class OnlineRepositoryTest {

    private lateinit var repository: OnlineRepositoryStub
    private lateinit var requirements: OnlineRepositoryStub.GetRequirements

    private lateinit var compositeDisposable: CompositeDisposable

    @Mock private lateinit var cacheAgeManager: OnlineRepositoryCacheAgeManager
    @Mock private lateinit var refreshManager: OnlineRepositoryRefreshManager
    @Mock private lateinit var schedulersProvider: SchedulersProvider
    @Mock private lateinit var taskExecutor: TaskExecutor
    @Mock private lateinit var refreshManagerListener: OnlineRepositoryRefreshManager.Listener

    @Before
    fun setup() {
        compositeDisposable = CompositeDisposable()
        schedulersProvider.apply {
            `when`(io()).thenReturn(Schedulers.trampoline())
            `when`(main()).thenReturn(Schedulers.trampoline())
        }
        `when`(taskExecutor.postUI(any())).thenAnswer {
            (it.getArgument(0) as () -> Unit).invoke()
        }
        requirements = OnlineRepositoryStub.GetRequirements()
        repository = OnlineRepositoryStub(cacheAgeManager, refreshManager, schedulersProvider, taskExecutor, refreshManagerListener)
    }

    @After
    fun teardown() {
        compositeDisposable.clear()
    }

    @Test
    fun refresh_requirementsNotSet_expectThrowException() {
        assertFailsWith(IllegalStateException::class) {
            repository.refresh(false)
                    .subscribe()
        }
    }

    @Test
    fun refresh_hasNeverFetched_expectRefreshCall() {
        val refreshResult = Single.just(OnlineRepository.RefreshResult.success())
        `when`(cacheAgeManager.hasSuccessfullyFetchedCache(requirements.tag)).thenReturn(false)
        `when`(refreshManager.refresh(repository.fetchFreshCache_return, requirements.tag, refreshManagerListener)).thenReturn(refreshResult)

        repository.requirements = requirements

        compositeDisposable += repository.refresh(false)
                .test()
                .await()
                .assertValue(check {
                    assertThat(it).isEqualTo(OnlineRepository.RefreshResult.success())
                })
    }

    @Test
    fun refresh_fetchedCacheTooOld_expectRefreshCall() {
        val refreshResult = Single.just(OnlineRepository.RefreshResult.success())
        `when`(cacheAgeManager.hasSuccessfullyFetchedCache(requirements.tag)).thenReturn(true)
        `when`(cacheAgeManager.lastSuccessfulFetch(requirements.tag)).thenReturn(Date())
        `when`(cacheAgeManager.isCacheTooOld(requirements.tag, repository.maxAgeOfCache)).thenReturn(true)
        `when`(refreshManager.refresh(repository.fetchFreshCache_return, requirements.tag, refreshManagerListener)).thenReturn(refreshResult)

        repository.requirements = requirements

        compositeDisposable += repository.refresh(false)
                .test()
                .await()
                .assertValue(check {
                    assertThat(it).isEqualTo(OnlineRepository.RefreshResult.success())
                })
    }

    @Test
    fun refresh_fetchedCacheNotTooOldForceRefresh_expectRefreshCall() {
        val refreshResult = Single.just(OnlineRepository.RefreshResult.success())
        `when`(cacheAgeManager.hasSuccessfullyFetchedCache(requirements.tag)).thenReturn(true)
        `when`(cacheAgeManager.lastSuccessfulFetch(requirements.tag)).thenReturn(Date())
        // `when`(cacheAgeManager.isCacheTooOld(requirements.tag, repository.maxAgeOfCache)).thenReturn(false)
        `when`(refreshManager.refresh(repository.fetchFreshCache_return, requirements.tag, refreshManagerListener)).thenReturn(refreshResult)

        repository.requirements = requirements

        compositeDisposable += repository.refresh(true)
                .test()
                .await()
                .assertValue(check {
                    assertThat(it).isEqualTo(OnlineRepository.RefreshResult.success())
                })
    }

    @Test
    fun refresh_fetchedCacheNotTooOldNoForceRefresh_expectSkipRefreshCall() {
        `when`(cacheAgeManager.hasSuccessfullyFetchedCache(requirements.tag)).thenReturn(true)
        `when`(cacheAgeManager.lastSuccessfulFetch(requirements.tag)).thenReturn(Date())
        `when`(cacheAgeManager.isCacheTooOld(requirements.tag, repository.maxAgeOfCache)).thenReturn(false)

        repository.requirements = requirements

        compositeDisposable += repository.refresh(false)
                .test()
                .await()
                .assertValue(check {
                    assertThat(it.didSkip()).isTrue()
                    assertThat(it.skipped).isEqualTo(OnlineRepository.RefreshResult.SkippedReason.CACHE_NOT_TOO_OLD)
                })
    }

}