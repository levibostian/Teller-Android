package com.levibostian.teller.repository

import android.os.Looper
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.extensions.plusAssign
import com.levibostian.teller.provider.SchedulersProvider
import com.levibostian.teller.repository.manager.OnlineRepositoryRefreshManager
import com.levibostian.teller.repository.manager.OnlineRepositorySyncStateManager
import org.junit.Test
import com.levibostian.teller.util.AssertionUtil.Companion.check
import com.levibostian.teller.util.TaskExecutor
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq

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

    @Mock private lateinit var syncStateManager: OnlineRepositorySyncStateManager
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
        repository = OnlineRepositoryStub(syncStateManager, refreshManager, schedulersProvider, taskExecutor, refreshManagerListener)
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
    fun refresh_hasNeverFetchedData_expectRefreshCall() {
        val refreshResult = Single.just(OnlineRepository.RefreshResult.success())
        `when`(syncStateManager.hasEverFetchedData(requirements.tag)).thenReturn(false)
        `when`(refreshManager.refresh(repository.fetchFreshData_return, requirements.tag, refreshManagerListener)).thenReturn(refreshResult)

        repository.requirements = requirements

        compositeDisposable += repository.refresh(false)
                .test()
                .await()
                .assertValue(check {
                    assertThat(it).isEqualTo(OnlineRepository.RefreshResult.success())
                })
    }

    @Test
    fun refresh_hasFetchedDataTooOld_expectRefreshCall() {
        val refreshResult = Single.just(OnlineRepository.RefreshResult.success())
        `when`(syncStateManager.hasEverFetchedData(requirements.tag)).thenReturn(true)
        `when`(syncStateManager.lastTimeFetchedData(requirements.tag)).thenReturn(Date())
        `when`(syncStateManager.isDataTooOld(requirements.tag, repository.maxAgeOfData)).thenReturn(true)
        `when`(refreshManager.refresh(repository.fetchFreshData_return, requirements.tag, refreshManagerListener)).thenReturn(refreshResult)

        repository.requirements = requirements

        compositeDisposable += repository.refresh(false)
                .test()
                .await()
                .assertValue(check {
                    assertThat(it).isEqualTo(OnlineRepository.RefreshResult.success())
                })
    }

    @Test
    fun refresh_hasFetchedDataNotTooOldForceRefresh_expectRefreshCall() {
        val refreshResult = Single.just(OnlineRepository.RefreshResult.success())
        `when`(syncStateManager.hasEverFetchedData(requirements.tag)).thenReturn(true)
        `when`(syncStateManager.lastTimeFetchedData(requirements.tag)).thenReturn(Date())
        // `when`(syncStateManager.isDataTooOld(requirements.tag, repository.maxAgeOfData)).thenReturn(false)
        `when`(refreshManager.refresh(repository.fetchFreshData_return, requirements.tag, refreshManagerListener)).thenReturn(refreshResult)

        repository.requirements = requirements

        compositeDisposable += repository.refresh(true)
                .test()
                .await()
                .assertValue(check {
                    assertThat(it).isEqualTo(OnlineRepository.RefreshResult.success())
                })
    }

    @Test
    fun refresh_hasFetchedDataNotTooOldNoForceRefresh_expectSkipRefreshCall() {
        `when`(syncStateManager.hasEverFetchedData(requirements.tag)).thenReturn(true)
        `when`(syncStateManager.lastTimeFetchedData(requirements.tag)).thenReturn(Date())
        `when`(syncStateManager.isDataTooOld(requirements.tag, repository.maxAgeOfData)).thenReturn(false)

        repository.requirements = requirements

        compositeDisposable += repository.refresh(false)
                .test()
                .await()
                .assertValue(check {
                    assertThat(it.didSkip()).isTrue()
                    assertThat(it.skipped).isEqualTo(OnlineRepository.RefreshResult.SkippedReason.DATA_NOT_TOO_OLD)
                })
    }

}