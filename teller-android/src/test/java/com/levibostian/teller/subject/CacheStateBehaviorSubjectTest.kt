package com.levibostian.teller.subject


import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.cachestate.CacheState
import com.levibostian.teller.cachestate.statemachine.CacheStateStateMachine
import com.levibostian.teller.extensions.plusAssign
import com.levibostian.teller.repository.TellerRepository
import com.levibostian.teller.repository.TellerRepositoryStub
import com.levibostian.teller.util.AssertionUtil.Companion.check
import io.reactivex.disposables.CompositeDisposable
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class CacheStateBehaviorSubjectTest {

    private lateinit var subject: CacheStateBehaviorSubject<String>
    @Mock private lateinit var requirements: TellerRepository.GetCacheRequirements

    private lateinit var compositeDisposable: CompositeDisposable

    @Before
    fun setup() {
        compositeDisposable = CompositeDisposable()
        subject = CacheStateBehaviorSubject()
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
                    assertThat(it).isEqualTo(CacheState.none<String>())
                })
    }

    @Test
    fun resetStateToNone_receiveNoDataState() {
        subject.resetStateToNone()

        compositeDisposable += subject.asObservable()
                .test()
                .awaitCount(1)
                .assertValue(check {
                    assertThat(it).isEqualTo(CacheState.none<String>())
                })
    }

    @Test
    fun resetToNoCacheState_receiveCorrectDataState() {
        subject.resetToNoCacheState(requirements)

        compositeDisposable += subject.asObservable()
                .test()
                .awaitCount(1)
                .assertValue(check {
                    assertThat(it).isEqualTo(CacheStateStateMachine.noCacheExists<String>(requirements))
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
                    assertThat(it).isEqualTo(CacheStateStateMachine.cacheExists<String>(requirements, lastTimeFetched))
                })
    }

    @Test
    fun changeState_sendsResultToSubject() {
        subject.resetToNoCacheState(requirements)

        val testObserver = subject.asObservable().test()

        subject.changeState(requirements) { it.firstFetch() }

        compositeDisposable += testObserver
                .awaitCount(2)
                .assertValues(
                        CacheStateStateMachine.noCacheExists(requirements),
                        CacheStateStateMachine.noCacheExists<String>(requirements).change().firstFetch())
    }

    @Test
    fun changeState_givenNoExistingRequirements_expectIgnoreRequest() {
        subject.changeState(requirements) { it.firstFetch() }

        assertThat(subject.currentState!!.isFetchingFirstCache).isFalse()
    }

    @Test
    fun changeState_givenExistingRequirementsTagsDontMatch_expectIgnoreRequest() {
        subject.resetToNoCacheState(TellerRepositoryStub.GetRequirements("tag1"))
        subject.changeState(TellerRepositoryStub.GetRequirements("tag2")) { it.firstFetch() }
        assertThat(subject.currentState!!.isFetchingFirstCache).isFalse()

        subject.changeState(TellerRepositoryStub.GetRequirements("tag1")) { it.firstFetch() }
        assertThat(subject.currentState!!.isFetchingFirstCache).isTrue()
    }

    @Test
    fun multipleObservers_receiveDifferentNumberOfEvents() {
        subject.resetStateToNone()
        subject.resetToNoCacheState(requirements)
        subject.changeState(requirements) { it.firstFetch() }

        val testObserver1 = subject.asObservable().test()

        val fetched = Date()
        subject.changeState(requirements) { it.successfulFirstFetch(fetched) }

        val testObserver2 = subject.asObservable().test()

        val data = "foo"
        subject.changeState(requirements) { it.cache(data) }

        val expectedSeriesOfEvents = listOf(
                CacheStateStateMachine.noCacheExists<String>(requirements).change()
                        .firstFetch(),
                CacheStateStateMachine.noCacheExists<String>(requirements).change()
                        .firstFetch().change()
                        .successfulFirstFetch(fetched),
                CacheStateStateMachine.cacheExists<String>(requirements, fetched).change()
                        .cache(data)
        )

        compositeDisposable += testObserver1
                .awaitCount(expectedSeriesOfEvents.size)
                .assertValueSequence(expectedSeriesOfEvents)

        compositeDisposable += testObserver2
                .awaitCount(expectedSeriesOfEvents.size - 1)
                .assertValueSequence(expectedSeriesOfEvents.subList(1, expectedSeriesOfEvents.size))
    }

}