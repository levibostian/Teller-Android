package com.levibostian.teller.subject


import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.cachestate.LocalCacheState
import com.levibostian.teller.extensions.plusAssign
import com.levibostian.teller.repository.LocalRepository
import org.junit.Test
import com.levibostian.teller.util.AssertionUtil.Companion.check

import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import io.reactivex.disposables.CompositeDisposable
import org.junit.After
import org.junit.Before
import org.mockito.Mock

@RunWith(MockitoJUnitRunner::class)
class LocalCacheStateBehaviorSubjectTest {

    private lateinit var subject: LocalCacheStateBehaviorSubject<String>

    @Mock private lateinit var requirements: LocalRepository.GetCacheRequirements

    private lateinit var compositeDisposable: CompositeDisposable

    @Before
    fun setup() {
        compositeDisposable = CompositeDisposable()
        subject = LocalCacheStateBehaviorSubject()
    }

    @After
    fun teardown() {
        compositeDisposable.clear()
    }

    @Test
    fun init_defaultValue() {
        compositeDisposable += subject.asObservable()
                .test()
                .awaitCount(1)
                .assertValue(check {
                    assertThat(it).isEqualTo(LocalCacheState.none<String>())
                })
    }

    @Test
    fun onNextEmpty_receive1Event() {
        subject.onNextEmpty(requirements)

        compositeDisposable += subject.asObservable()
                .test()
                .awaitCount(1)
                .assertValue(check {
                    assertThat(it).isEqualTo(LocalCacheState.isEmpty<String>(requirements))
                })
    }

    @Test
    fun onNextEmpty_receive2Events() {
        val testObserver = subject.asObservable().test()

        subject.onNextEmpty(requirements)

        compositeDisposable += testObserver
                .awaitCount(2)
                .assertValues(
                        LocalCacheState.none(),
                        LocalCacheState.isEmpty(requirements)
                )
    }

    @Test
    fun multipleObservers() {
        val testObserver1 = subject.asObservable().test()

        subject.onNextEmpty(requirements)

        val testObserver2 = subject.asObservable().test()

        val data = "foo"
        subject.onNextCache(requirements, data)

        val expectedSeriesOfEvents = listOf(
                LocalCacheState.none(),
                LocalCacheState.isEmpty(requirements),
                LocalCacheState.cache(requirements, data)
        )

        compositeDisposable += testObserver1
                .awaitCount(expectedSeriesOfEvents.size)
                .assertValueSequence(expectedSeriesOfEvents)

        compositeDisposable += testObserver2
                .awaitCount(expectedSeriesOfEvents.size - 1)
                .assertValueSequence(expectedSeriesOfEvents.subList(1, expectedSeriesOfEvents.size))
    }

    @Test
    fun onNextData() {
        val data = "foo"
        subject.onNextCache(requirements, data)

        compositeDisposable += subject.asObservable()
                .test()
                .awaitCount(1)
                .assertValue(check {
                    assertThat(it).isEqualTo(LocalCacheState.cache(requirements, data))
                })
    }

    @Test
    fun resetStateToNone() {
        val data = "foo"
        subject.onNextCache(requirements, data)

        val testObserver = subject.asObservable().test()

        subject.resetStateToNone()

        compositeDisposable += testObserver
                .awaitCount(2)
                .assertValues(
                        LocalCacheState.cache(requirements, data),
                        LocalCacheState.none()
                )
    }

}