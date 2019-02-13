package com.levibostian.teller.repository

import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.extensions.plusAssign
import com.levibostian.teller.provider.SchedulersProvider
import com.levibostian.teller.repository.manager.OnlineRepositoryRefreshManager
import com.levibostian.teller.repository.manager.OnlineRepositorySyncStateManager
import org.junit.Test
import com.levibostian.teller.util.AssertionUtil.Companion.check

import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import org.junit.Before
import org.mockito.Mockito.`when`
import io.reactivex.schedulers.Schedulers
import org.junit.After
import kotlin.test.assertFailsWith

@RunWith(MockitoJUnitRunner::class)
class OnlineRepositoryTest {

    private lateinit var repository: OnlineRepositoryStub
    private lateinit var requirements: OnlineRepositoryStub.GetRequirements

    private lateinit var compositeDisposable: CompositeDisposable

    @Mock private lateinit var syncStateManager: OnlineRepositorySyncStateManager
    @Mock private lateinit var refreshManager: OnlineRepositoryRefreshManager
    @Mock private lateinit var schedulersProvider: SchedulersProvider

    @Before
    fun setup() {
        compositeDisposable = CompositeDisposable()
        schedulersProvider.apply {
            `when`(io()).thenReturn(Schedulers.trampoline())
            `when`(main()).thenReturn(Schedulers.trampoline())
        }
        requirements = OnlineRepositoryStub.GetRequirements()
        repository = OnlineRepositoryStub(syncStateManager, refreshManager, schedulersProvider)
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
        `when`(refreshManager.refresh(repository.fetchFreshData_return, requirements.tag, repository)).thenReturn(refreshResult)

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
        `when`(syncStateManager.isDataTooOld(requirements.tag, repository.maxAgeOfData)).thenReturn(true)
        `when`(refreshManager.refresh(repository.fetchFreshData_return, requirements.tag, repository)).thenReturn(refreshResult)

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
        `when`(syncStateManager.hasEverFetchedData(requirements.tag)).thenReturn(false)
        `when`(syncStateManager.isDataTooOld(requirements.tag, repository.maxAgeOfData)).thenReturn(true)
        `when`(refreshManager.refresh(repository.fetchFreshData_return, requirements.tag, repository)).thenReturn(refreshResult)

        compositeDisposable += repository.refresh(true)
                .test()
                .await()
                .assertValue(check {
                    assertThat(it).isEqualTo(OnlineRepository.RefreshResult.success())
                })
    }

    @Test
    fun refresh_hasFetchedDataNotTooOldNoForceRefresh_expectSkipRefreshCall() {
        `when`(syncStateManager.hasEverFetchedData(requirements.tag)).thenReturn(false)
        `when`(syncStateManager.isDataTooOld(requirements.tag, repository.maxAgeOfData)).thenReturn(true)

        compositeDisposable += repository.refresh(false)
                .test()
                .await()
                .assertValue(check {
                    assertThat(it.didSkip()).isTrue()
                    assertThat(it.skipped).isEqualTo(OnlineRepository.RefreshResult.SkippedReason.DATA_NOT_TOO_OLD)
                })
    }

//    @Test
//    fun requirements_setNewValue_oldRefreshTaskGetsCancelled() {
//        `when`(syncStateManager.hasEverFetchedData(requirements.tag)).thenReturn(false)
//        `when`(refreshManager.refresh(repository.fetchFreshData_return, requirements.tag, repository)).thenReturn(Single.never())
//
//        repository.requirements = requirements
//
//        // 2nd time needed in order to cancel request
//        repository.requirements = requirements
//
//        verify(refreshManager).cancelTasksForRepository(requirements.tag, repository)
//    }

//    @Test
//    fun requirements_setNewValue_stopObservingOldCache() {
//        var isDisposed = false
//        val observeCachedDataObservable = Observable.never<String>().doOnDispose {
//            isDisposed = true
//        }
//        repository.observeCachedData_return = observeCachedDataObservable
//        `when`(syncStateManager.hasEverFetchedData(requirements.tag)).thenReturn(true)
//        `when`(refreshManager.refresh(repository.fetchFreshData_return, requirements.tag, repository)).thenReturn(Single.never())
//
//        repository.requirements = requirements
//
//        // 2nd time needed in order to stop observing
//        repository.requirements = requirements
//
//        assertThat(isDisposed).isTrue()
//    }

//    @Test
//    fun requirements_setNull_expectSetStateOfDataToNone() {
//        `when`(syncStateManager.hasEverFetchedData(requirements.tag)).thenReturn(false)
//        `when`(refreshManager.refresh(repository.fetchFreshData_return, requirements.tag, repository)).thenReturn(Single.never())
//
//        repository.requirements = requirements
//
//        val testObserver = repository.observe().test()
//
//        // 2nd time needed in order to cancel request
//        repository.requirements = null
//
//        compositeDisposable += testObserver
//                .awaitCount(2)
//                .assertValueAt(1, OnlineDataState.none())
//    }

}