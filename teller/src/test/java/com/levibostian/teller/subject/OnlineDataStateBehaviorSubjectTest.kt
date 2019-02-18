package com.levibostian.teller.subject


import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.datastate.LocalDataState
import com.levibostian.teller.datastate.OnlineDataState
import com.levibostian.teller.datastate.online.statemachine.OnlineDataStateStateMachine
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
import java.util.*
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class OnlineDataStateBehaviorSubjectTest {

    private lateinit var subject: OnlineDataStateBehaviorSubject<String>
    @Mock private lateinit var requirements: OnlineRepository.GetDataRequirements

    private lateinit var compositeDisposable: CompositeDisposable

    @Before
    fun setup() {
        compositeDisposable = CompositeDisposable()
        subject = OnlineDataStateBehaviorSubject()
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
                    assertThat(it).isEqualTo(OnlineDataState.none<String>())
                })
    }

    @Test
    fun resetStateToNone_receiveNoDataState() {
        subject.resetStateToNone()

        compositeDisposable += subject.asObservable()
                .test()
                .awaitCount(1)
                .assertValue(check {
                    assertThat(it).isEqualTo(OnlineDataState.none<String>())
                })
    }

    @Test
    fun resetToNoCacheState_receiveCorrectDataState() {
        subject.resetToNoCacheState(requirements)

        compositeDisposable += subject.asObservable()
                .test()
                .awaitCount(1)
                .assertValue(check {
                    assertThat(it).isEqualTo(OnlineDataStateStateMachine.noCacheExists<String>(requirements))
                })
    }

    @Test
    fun resetToCacheState_receiveCorrectDataState() {
        val lastTimeFetched = Date()
        subject.resetToCacheState(requirements, lastTimeFetched)

        compositeDisposable += subject.asObservable()
                .test()
                .awaitCount(1)
                .assertValue(check {
                    assertThat(it).isEqualTo(OnlineDataStateStateMachine.cacheExists<String>(requirements, lastTimeFetched))
                })
    }

    @Test
    fun changeState_sendsResultToSubject() {
        subject.resetToNoCacheState(requirements)

        val testObserver = subject.asObservable().test()

        subject.changeState { it.firstFetch() }

        compositeDisposable += testObserver
                .awaitCount(2)
                .assertValues(
                        OnlineDataStateStateMachine.noCacheExists(requirements),
                        OnlineDataStateStateMachine.noCacheExists<String>(requirements).change().firstFetch())
    }

    @Test
    fun multipleObservers_receiveDifferentNumberOfEvents() {
        subject.resetStateToNone()
        subject.resetToNoCacheState(requirements)
        subject.changeState { it.firstFetch() }

        val testObserver1 = subject.asObservable().test()

        val fetched = Date()
        subject.changeState { it.successfulFirstFetch(fetched) }

        val testObserver2 = subject.asObservable().test()

        val data = "foo"
        subject.changeState { it.cachedData(data) }

        val expectedSeriesOfEvents = listOf(
                OnlineDataStateStateMachine.noCacheExists<String>(requirements).change()
                        .firstFetch(),
                OnlineDataStateStateMachine.noCacheExists<String>(requirements).change()
                        .firstFetch().change()
                        .successfulFirstFetch(fetched),
                OnlineDataStateStateMachine.cacheExists<String>(requirements, fetched).change()
                        .cachedData(data)
        )

        compositeDisposable += testObserver1
                .awaitCount(expectedSeriesOfEvents.size)
                .assertValueSequence(expectedSeriesOfEvents)

        compositeDisposable += testObserver2
                .awaitCount(expectedSeriesOfEvents.size - 1)
                .assertValueSequence(expectedSeriesOfEvents.subList(1, expectedSeriesOfEvents.size))
    }

}