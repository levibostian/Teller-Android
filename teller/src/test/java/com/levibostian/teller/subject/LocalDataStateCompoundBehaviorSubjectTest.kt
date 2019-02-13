package com.levibostian.teller.subject


import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.datastate.LocalDataState
import com.levibostian.teller.extensions.plusAssign
import com.levibostian.teller.repository.GetDataRequirementsTag
import com.levibostian.teller.repository.OnlineRepository
import io.reactivex.subjects.PublishSubject
import org.junit.Test
import com.levibostian.teller.util.AssertionUtil.Companion.check
import com.levibostian.teller.util.ConstantsUtil

import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import com.nhaarman.mockito_kotlin.*
import io.reactivex.disposables.CompositeDisposable
import org.junit.After
import org.junit.Before
import org.mockito.Mockito.`when`
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class LocalDataStateCompoundBehaviorSubjectTest {

    private lateinit var subject: LocalDataStateCompoundBehaviorSubject<String>

    private lateinit var compositeDisposable: CompositeDisposable

    @Before
    fun setup() {
        compositeDisposable = CompositeDisposable()
        subject = LocalDataStateCompoundBehaviorSubject()
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
                    assertThat(it).isEqualTo(LocalDataState.none<String>())
                })
    }

    @Test
    fun onNextEmpty_receive1Event() {
        subject.onNextEmpty()

        compositeDisposable += subject.asObservable()
                .test()
                .awaitCount(1)
                .assertValue(check {
                    assertThat(it).isEqualTo(LocalDataState.isEmpty<String>())
                })
    }

    @Test
    fun onNextEmpty_receive2Events() {
        val testObserver = subject.asObservable().test()

        subject.onNextEmpty()

        compositeDisposable += testObserver
                .awaitCount(2)
                .assertValues(
                        LocalDataState.none(),
                        LocalDataState.isEmpty()
                )
    }

    @Test
    fun multipleObservers() {
        val testObserver1 = subject.asObservable().test()

        subject.onNextEmpty()

        val testObserver2 = subject.asObservable().test()

        val data = "foo"
        subject.onNextData(data)

        val expectedSeriesOfEvents = listOf(
                LocalDataState.none(),
                LocalDataState.isEmpty(),
                LocalDataState.data(data)
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
        subject.onNextData(data)

        compositeDisposable += subject.asObservable()
                .test()
                .awaitCount(1)
                .assertValue(check {
                    assertThat(it).isEqualTo(LocalDataState.data(data))
                })
    }

    @Test
    fun resetStateToNone() {
        val data = "foo"
        subject.onNextData(data)

        val testObserver = subject.asObservable().test()

        subject.resetStateToNone()

        compositeDisposable += testObserver
                .awaitCount(2)
                .assertValues(
                        LocalDataState.data(data),
                        LocalDataState.none()
                )
    }

}