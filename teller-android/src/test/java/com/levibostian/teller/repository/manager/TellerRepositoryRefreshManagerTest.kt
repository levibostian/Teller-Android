package com.levibostian.teller.repository.manager

import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.extensions.awaitComplete
import com.levibostian.teller.repository.GetCacheRequirementsTag
import com.levibostian.teller.repository.TellerRepository
import com.levibostian.teller.util.AssertionUtil.Companion.check
import com.levibostian.teller.util.Wait
import com.nhaarman.mockito_kotlin.*
import io.reactivex.subjects.ReplaySubject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class TellerRepositoryRefreshManagerTest {

    @Mock private lateinit var repo1: RepositoryRefreshManager.Listener
    @Mock private lateinit var repo2: RepositoryRefreshManager.Listener

    private val defaultTag: GetCacheRequirementsTag = "defaultTag"
    private val otherTag: GetCacheRequirementsTag = "otherTag"

    @Test
    fun refresh_twoRepositoriesSameTag_expectRefreshTaskToBeShared() {
        val task1 = ReplaySubject.create<TellerRepository.FetchResponse<String>>()
        val task2 = ReplaySubject.create<TellerRepository.FetchResponse<String>>()

        val task1Result = TellerRepository.FetchResponse.success("task1")
        val task2Result = RuntimeException("task2")

        val repo1TestObserver = SharedRepositoryRefreshManager.refresh(task1.singleOrError(), defaultTag, repo1).test()
        val repo2TestObserver = SharedRepositoryRefreshManager.refresh(task2.singleOrError(), defaultTag, repo2).test()

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
                .awaitComplete()
                .assertValue(check {
                    assertThat(it.didSucceed()).isTrue()
                })

        repo2TestObserver
                .awaitComplete()
                .assertValue(check {
                    assertThat(it.didSucceed()).isTrue()
                })

        verify(repo1).refreshBegin(defaultTag)
        verify(repo2).refreshBegin(defaultTag)

        val repo1RefreshCompleteArgumentCaptor = argumentCaptor<TellerRepository.FetchResponse<String>>()
        verify(repo1).refreshComplete(eq(defaultTag), repo1RefreshCompleteArgumentCaptor.capture())
        val repo2RefreshCompleteArgumentCaptor = argumentCaptor<TellerRepository.FetchResponse<String>>()
        verify(repo2).refreshComplete(eq(defaultTag), repo2RefreshCompleteArgumentCaptor.capture())

        assertThat(repo1RefreshCompleteArgumentCaptor.firstValue).isEqualTo(task1Result)
        assertThat(repo2RefreshCompleteArgumentCaptor.firstValue).isEqualTo(task1Result)
    }

    @Test
    fun refresh_twoRepositoriesDifferentTag_expectBothRefreshTasksToRun() {
        val task1 = ReplaySubject.create<TellerRepository.FetchResponse<String>>()
        val task2 = ReplaySubject.create<TellerRepository.FetchResponse<String>>()

        val task1Result = TellerRepository.FetchResponse.success("task1")
        val task2Result = TellerRepository.FetchResponse.fail<String>(RuntimeException("error"))

        val repo1TestObserver = SharedRepositoryRefreshManager.refresh(task1.singleOrError(), defaultTag, repo1).test()
        val repo2TestObserver = SharedRepositoryRefreshManager.refresh(task2.singleOrError(), otherTag, repo2).test()

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
                .awaitComplete()
                .assertValue(check {
                    assertThat(it.didSucceed()).isTrue()
                })

        repo2TestObserver
                .awaitComplete()
                .assertValue(check {
                    assertThat(it.didFail()).isTrue()
                })

        verify(repo1).refreshBegin(defaultTag)
        verify(repo2).refreshBegin(otherTag)

        val repo1RefreshCompleteArgumentCaptor = argumentCaptor<TellerRepository.FetchResponse<String>>()
        verify(repo1).refreshComplete(eq(defaultTag), repo1RefreshCompleteArgumentCaptor.capture())
        val repo2RefreshCompleteArgumentCaptor = argumentCaptor<TellerRepository.FetchResponse<String>>()
        verify(repo2).refreshComplete(eq(otherTag), repo2RefreshCompleteArgumentCaptor.capture())

        assertThat(repo1RefreshCompleteArgumentCaptor.firstValue).isEqualTo(task1Result)
        assertThat(repo2RefreshCompleteArgumentCaptor.firstValue).isEqualTo(task2Result)
    }

    @Test
    fun refresh_oneRepositoryMultipleRefreshCalls_expectToIgnoreSecondRequest() {
        val task1 = ReplaySubject.create<TellerRepository.FetchResponse<String>>()
        val task2 = ReplaySubject.create<TellerRepository.FetchResponse<String>>()

        val task1Result = TellerRepository.FetchResponse.success("task1")
        val task2Result = RuntimeException("task2")

        val repo1TestObserver = SharedRepositoryRefreshManager.refresh(task1.singleOrError(), defaultTag, repo1).test()
        val repo1SecondTestObserver = SharedRepositoryRefreshManager.refresh(task2.singleOrError(), defaultTag, repo1).test()

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
                .awaitComplete()
                .assertValue(check {
                    assertThat(it.didSucceed()).isTrue()
                })

        repo1SecondTestObserver
                .awaitComplete()
                .assertValue(check {
                    assertThat(it.didSucceed()).isTrue()
                })

        verify(repo1).refreshBegin(defaultTag)

        val repo1RefreshCompleteArgumentCaptor = argumentCaptor<TellerRepository.FetchResponse<String>>()
        verify(repo1).refreshComplete(eq(defaultTag), repo1RefreshCompleteArgumentCaptor.capture())

        assertThat(repo1RefreshCompleteArgumentCaptor.firstValue).isEqualTo(task1Result)
    }

    @Test
    fun cancelTasksForRepository_oneRepository_refreshTaskGetsCancelled() {
        val wait = Wait.times(1)
        val task1 = ReplaySubject.create<TellerRepository.FetchResponse<String>>()
                .doOnDispose {
                    wait.countDown()
                }
        val repo1TestObserver = SharedRepositoryRefreshManager.refresh(task1.singleOrError(), defaultTag, repo1).test()

        verify(repo1).refreshBegin(defaultTag)

        SharedRepositoryRefreshManager.cancelTasksForRepository(defaultTag, repo1)

        repo1TestObserver
                .awaitComplete()
                .assertValue(check {
                    assertThat(it.didSkip()).isTrue()
                })

        wait.await()

        verify(repo1, never()).refreshComplete(any(), any<TellerRepository.FetchResponse<String>>())
    }

    @Test
    fun cancelTasksForRepository_twoRepositoriesSameTask_refreshTaskContinuesForTaskThatDidNotCancel() {
        val wait = Wait.times(1)
        val task1Subject = ReplaySubject.create<TellerRepository.FetchResponse<String>>()
        val task1Single = task1Subject
                .doOnDispose {
                    wait.countDown()
                }.singleOrError()
        val task2 = ReplaySubject.create<TellerRepository.FetchResponse<String>>()

        val task1Response = TellerRepository.FetchResponse.success("task1")
        val task2Response = TellerRepository.FetchResponse.success("task2")

        val repo1TestObserver = SharedRepositoryRefreshManager.refresh(task1Single, defaultTag, repo1).test()
        val repo2TestObserver = SharedRepositoryRefreshManager.refresh(task2.singleOrError(), defaultTag, repo2).test()

        verify(repo1).refreshBegin(defaultTag)
        verify(repo2).refreshBegin(defaultTag)

        SharedRepositoryRefreshManager.cancelTasksForRepository(defaultTag, repo1)

        // Continue running task as repo2 is still observing.
        assertThat(wait.notDone()).isTrue()

        task1Subject.apply {
            onNext(task1Response)
            onComplete()
        }
        task2.apply {
            onNext(task2Response)
            onComplete()
        }

        repo1TestObserver
                .awaitComplete()
                .assertValue(check {
                    assertThat(it.didSkip()).isTrue()
                    assertThat(it.didSucceed()).isFalse()
                })

        repo2TestObserver
                .awaitComplete()
                .assertValue(check {
                    assertThat(it.didSkip()).isFalse()
                    assertThat(it.didSucceed()).isTrue()
                })

        verify(repo1, never()).refreshComplete(any(), any<TellerRepository.FetchResponse<String>>())
        val refreshCompleteArgumentCaptor = argumentCaptor<TellerRepository.FetchResponse<String>>()
        verify(repo2).refreshComplete(any(), refreshCompleteArgumentCaptor.capture())
        assertThat(refreshCompleteArgumentCaptor.firstValue).isEqualTo(task1Response)
    }

    @Test
    fun cancelTasksForRepository_otherRepoThatDidntCallRefresh_ignoreCancelRequest() {
        val wait = Wait.times(1)
        val task1Subject = ReplaySubject.create<TellerRepository.FetchResponse<String>>()
        val task1Single = task1Subject
                .doOnDispose {
                    wait.countDown()
                }.singleOrError()

        val task1Response = TellerRepository.FetchResponse.success("task1")

        val repo1TestObserver = SharedRepositoryRefreshManager.refresh(task1Single, defaultTag, repo1).test()

        // Call cancel with same tag, but repo that never started a refresh call.
        SharedRepositoryRefreshManager.cancelTasksForRepository(defaultTag, repo2)

        // Continue running task as repo1 is still observing refresh task that was started.
        assertThat(wait.notDone()).isTrue()

        task1Subject.apply {
            onNext(task1Response)
            onComplete()
        }

        repo1TestObserver
                .awaitCount(1)
                .assertValue(check {
                    assertThat(it.didSucceed()).isTrue()
                    assertThat(it.didSkip()).isFalse()
                })

        val refreshCompleteArgumentCaptor = argumentCaptor<TellerRepository.FetchResponse<String>>()
        verify(repo1).refreshComplete(any(), refreshCompleteArgumentCaptor.capture())
        assertThat(refreshCompleteArgumentCaptor.firstValue).isEqualTo(task1Response)
    }

}