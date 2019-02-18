package com.levibostian.teller.repository.manager

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.repository.GetDataRequirementsTag
import com.levibostian.teller.repository.OnlineRepository
import io.reactivex.subjects.PublishSubject
import org.junit.Test
import com.levibostian.teller.util.AssertionUtil.Companion.check

import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import com.nhaarman.mockito_kotlin.*
import io.reactivex.subjects.ReplaySubject

@RunWith(MockitoJUnitRunner::class)
class OnlineRepositoryRefreshManagerTest {

    @Mock private lateinit var repo1: OnlineRepository<String, OnlineRepository.GetDataRequirements, String>
    @Mock private lateinit var repo2: OnlineRepository<String, OnlineRepository.GetDataRequirements, String>

    private val defaultTag: GetDataRequirementsTag = "defaultTag"
    private val otherTag: GetDataRequirementsTag = "otherTag"

    @Test
    fun refresh_twoRepositoriesSameTag_expectRefreshTaskToBeShared() {
        val task1 = ReplaySubject.create<OnlineRepository.FetchResponse<String>>()
        val task2 = ReplaySubject.create<OnlineRepository.FetchResponse<String>>()

        val task1Result = OnlineRepository.FetchResponse.success("task1")
        val task2Result = RuntimeException("task2")

        val repo1TestObserver = SharedOnlineRepositoryRefreshManager.refresh(task1.singleOrError(), defaultTag, repo1).test()
        val repo2TestObserver = SharedOnlineRepositoryRefreshManager.refresh(task2.singleOrError(), defaultTag, repo2).test()

        task1.apply {
            onNext(task1Result)
            onComplete()
        }
        // This should be ignored.
        task2.apply {
            onError(task2Result)
            onComplete()
        }

        repo1TestObserver
                .await()
                .assertValue(check {
                    assertThat(it.didSucceed()).isTrue()
                })

        repo2TestObserver
                .await()
                .assertValue(check {
                    assertThat(it.didSucceed()).isTrue()
                })

        verify(repo1).refreshBegin()
        verify(repo2).refreshBegin()

        val repo1RefreshCompleteArgumentCaptor = argumentCaptor<OnlineRepository.FetchResponse<String>>()
        verify(repo1).refreshComplete(repo1RefreshCompleteArgumentCaptor.capture())
        val repo2RefreshCompleteArgumentCaptor = argumentCaptor<OnlineRepository.FetchResponse<String>>()
        verify(repo2).refreshComplete(repo2RefreshCompleteArgumentCaptor.capture())

        assertThat(repo1RefreshCompleteArgumentCaptor.firstValue).isEqualTo(task1Result)
        assertThat(repo2RefreshCompleteArgumentCaptor.firstValue).isEqualTo(task1Result)
    }

    @Test
    fun refresh_twoRepositoriesDifferentTag_expectBothRefreshTasksToRun() {
        val task1 = PublishSubject.create<OnlineRepository.FetchResponse<String>>()
        val task2 = PublishSubject.create<OnlineRepository.FetchResponse<String>>()

        val task1Result = OnlineRepository.FetchResponse.success("task1")
        val task2Result = OnlineRepository.FetchResponse.fail<String>(RuntimeException("error"))

        val repo1TestObserver = SharedOnlineRepositoryRefreshManager.refresh(task1.singleOrError(), defaultTag, repo1).test()
        val repo2TestObserver = SharedOnlineRepositoryRefreshManager.refresh(task2.singleOrError(), otherTag, repo2).test()

        task1.apply {
            onNext(task1Result)
            onComplete()
        }
        // This should be ignored.
        task2.apply {
            onNext(task2Result)
            onComplete()
        }

        repo1TestObserver
                .await()
                .assertValue(check {
                    assertThat(it.didSucceed()).isTrue()
                })

        repo2TestObserver
                .await()
                .assertValue(check {
                    assertThat(it.didFail()).isTrue()
                })

        verify(repo1).refreshBegin()
        verify(repo2).refreshBegin()

        val repo1RefreshCompleteArgumentCaptor = argumentCaptor<OnlineRepository.FetchResponse<String>>()
        verify(repo1).refreshComplete(repo1RefreshCompleteArgumentCaptor.capture())
        val repo2RefreshCompleteArgumentCaptor = argumentCaptor<OnlineRepository.FetchResponse<String>>()
        verify(repo2).refreshComplete(repo2RefreshCompleteArgumentCaptor.capture())

        assertThat(repo1RefreshCompleteArgumentCaptor.firstValue).isEqualTo(task1Result)
        assertThat(repo2RefreshCompleteArgumentCaptor.firstValue).isEqualTo(task2Result)
    }

    @Test
    fun refresh_oneRepositoryMultipleRefreshCalls_expectToIgnoreSecondRequest() {
        val task1 = PublishSubject.create<OnlineRepository.FetchResponse<String>>()
        val task2 = PublishSubject.create<OnlineRepository.FetchResponse<String>>()

        val task1Result = OnlineRepository.FetchResponse.success("task1")
        val task2Result = RuntimeException("task2")

        val repo1TestObserver = SharedOnlineRepositoryRefreshManager.refresh(task1.singleOrError(), defaultTag, repo1).test()
        val repo1SecondTestObserver = SharedOnlineRepositoryRefreshManager.refresh(task2.singleOrError(), defaultTag, repo1).test()

        task1.apply {
            onNext(task1Result)
            onComplete()
        }
        // This should be ignored.
        task2.apply {
            onError(task2Result)
            onComplete()
        }

        repo1TestObserver
                .await()
                .assertValue(check {
                    assertThat(it.didSucceed()).isTrue()
                })

        repo1SecondTestObserver
                .await()
                .assertValue(check {
                    assertThat(it.didSucceed()).isTrue()
                })

        verify(repo1).refreshBegin()

        val repo1RefreshCompleteArgumentCaptor = argumentCaptor<OnlineRepository.FetchResponse<String>>()
        verify(repo1).refreshComplete(repo1RefreshCompleteArgumentCaptor.capture())

        assertThat(repo1RefreshCompleteArgumentCaptor.firstValue).isEqualTo(task1Result)
    }

    @Test
    fun cancelTasksForRepository_oneRepository_refreshTaskGetsCancelled() {
        var task1Disposed = false
        val task1 = PublishSubject.create<OnlineRepository.FetchResponse<String>>()
                .doOnDispose {
                    task1Disposed = true
                }
        val repo1TestObserver = SharedOnlineRepositoryRefreshManager.refresh(task1.singleOrError(), defaultTag, repo1).test()

        verify(repo1).refreshBegin()

        SharedOnlineRepositoryRefreshManager.cancelTasksForRepository(defaultTag, repo1)

        assertThat(task1Disposed).isTrue()

        repo1TestObserver
                .await()
                .assertValue(check {
                    assertThat(it.didSkip()).isTrue()
                })

        verify(repo1, never()).refreshComplete(any<OnlineRepository.FetchResponse<String>>())
    }

    @Test
    fun cancelTasksForRepository_twoRepositoriesSameTask_refreshTaskContinuesForTaskThatDidNotCancel() {
        var task1Disposed = false
        val task1Subject = PublishSubject.create<OnlineRepository.FetchResponse<String>>()
        val task1Single = task1Subject
                .doOnDispose {
                    task1Disposed = true
                }.singleOrError()
        val task2 = PublishSubject.create<OnlineRepository.FetchResponse<String>>()

        val task1Response = OnlineRepository.FetchResponse.success("task1")
        val task2Response = OnlineRepository.FetchResponse.success("task2")

        val repo1TestObserver = SharedOnlineRepositoryRefreshManager.refresh(task1Single, defaultTag, repo1).test()
        val repo2TestObserver = SharedOnlineRepositoryRefreshManager.refresh(task2.singleOrError(), defaultTag, repo2).test()

        verify(repo1).refreshBegin()
        verify(repo2).refreshBegin()

        SharedOnlineRepositoryRefreshManager.cancelTasksForRepository(defaultTag, repo1)

        // Continue running task as repo2 is still observing.
        assertThat(task1Disposed).isFalse()

        task1Subject.apply {
            onNext(task1Response)
            onComplete()
        }
        task2.apply {
            onNext(task2Response)
            onComplete()
        }

        repo1TestObserver
                .await()
                .assertValue(check {
                    assertThat(it.didSkip()).isTrue()
                    assertThat(it.didSucceed()).isFalse()
                })

        repo2TestObserver
                .await()
                .assertValue(check {
                    assertThat(it.didSkip()).isFalse()
                    assertThat(it.didSucceed()).isTrue()
                })

        verify(repo1, never()).refreshComplete(any<OnlineRepository.FetchResponse<String>>())
        val refreshCompleteArgumentCaptor = argumentCaptor<OnlineRepository.FetchResponse<String>>()
        verify(repo2).refreshComplete(refreshCompleteArgumentCaptor.capture())
        assertThat(refreshCompleteArgumentCaptor.firstValue).isEqualTo(task1Response)
    }

    @Test
    fun cancelTasksForRepository_repoDidNotStartRefresh_ignoreRequest() {
        var task1Disposed = false
        val task1Subject = PublishSubject.create<OnlineRepository.FetchResponse<String>>()
        val task1Single = task1Subject
                .doOnDispose {
                    task1Disposed = true
                }.singleOrError()

        val task1Response = OnlineRepository.FetchResponse.success("task1")

        val repo1TestObserver = SharedOnlineRepositoryRefreshManager.refresh(task1Single, defaultTag, repo1).test()

        // Call cancel with same tag, but repo that never started a refresh call.
        SharedOnlineRepositoryRefreshManager.cancelTasksForRepository(defaultTag, repo2)

        // Continue running task as repo2 is still observing.
        assertThat(task1Disposed).isFalse()

        task1Subject.apply {
            onNext(task1Response)
            onComplete()
        }

        repo1TestObserver
                .await()
                .assertValue(check {
                    assertThat(it.didSucceed()).isTrue()
                    assertThat(it.didSkip()).isFalse()
                })

        val refreshCompleteArgumentCaptor = argumentCaptor<OnlineRepository.FetchResponse<String>>()
        verify(repo1).refreshComplete(refreshCompleteArgumentCaptor.capture())
        assertThat(refreshCompleteArgumentCaptor.firstValue).isEqualTo(task1Response)
    }

}