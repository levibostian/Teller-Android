package com.levibostian.teller.subject


import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.cachestate.LocalCacheState
import com.levibostian.teller.extensions.plusAssign
import org.junit.Test
import com.levibostian.teller.util.AssertionUtil.Companion.check

import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import io.reactivex.disposables.CompositeDisposable
import org.junit.After
import org.junit.Before

@RunWith(MockitoJUnitRunner::class)
class LocalCacheStateCompoundBehaviorSubjectTest {

    private lateinit var subject: LocalCacheStateCompoundBehaviorSubject<String>

    private lateinit var compositeDisposable: CompositeDisposable

    @Before
    fun setup() {
        compositeDisposable = CompositeDisposable()
        subject = LocalCacheStateCompoundBehaviorSubject()
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
        subject.onNextEmpty()

        compositeDisposable += subject.asObservable()
                .test()
                .awaitCount(1)
                .assertValue(check {
                    assertThat(it).isEqualTo(LocalCacheState.isEmpty<String>())
                })
    }

    @Test
    fun onNextEmpty_receive2Events() {
        val testObserver = subject.asObservable().test()

        subject.onNextEmpty()

        compositeDisposable += testObserver
                .awaitCount(2)
                .assertValues(
                        LocalCacheState.none(),
                        LocalCacheState.isEmpty()
                )
    }

    @Test
    fun multipleObservers() {
        val testObserver1 = subject.asObservable().test()

        subject.onNextEmpty()

        val testObserver2 = subject.asObservable().test()

        val data = "foo"
        subject.onNextCache(data)

        val expectedSeriesOfEvents = listOf(
                LocalCacheState.none(),
                LocalCacheState.isEmpty(),
                LocalCacheState.data(data)
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
        subject.onNextCache(data)

        compositeDisposable += subject.asObservable()
                .test()
                .awaitCount(1)
                .assertValue(check {
                    assertThat(it).isEqualTo(LocalCacheState.data(data))
                })
    }

    @Test
    fun resetStateToNone() {
        val data = "foo"
        subject.onNextCache(data)

        val testObserver = subject.asObservable().test()

        subject.resetStateToNone()

        compositeDisposable += testObserver
                .awaitCount(2)
                .assertValues(
                        LocalCacheState.data(data),
                        LocalCacheState.none()
                )
    }

}