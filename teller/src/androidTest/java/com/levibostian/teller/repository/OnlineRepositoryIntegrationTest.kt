package com.levibostian.teller.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.test.annotation.UiThreadTest

import org.junit.Test
import org.junit.runner.RunWith

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.datastate.OnlineDataState
import com.levibostian.teller.datastate.online.statemachine.OnlineDataStateStateMachine
import com.levibostian.teller.extensions.awaitDispose
import com.levibostian.teller.extensions.awaitDone
import com.levibostian.teller.extensions.getTellerSharedPreferences
import com.levibostian.teller.extensions.plusAssign
import com.levibostian.teller.provider.TellerSchedulersProvider
import com.levibostian.teller.repository.manager.OnlineRepositoryRefreshManager
import com.levibostian.teller.repository.manager.OnlineRepositoryRefreshManagerWrapper
import com.levibostian.teller.repository.manager.OnlineRepositorySyncStateManager
import com.levibostian.teller.repository.manager.TellerOnlineRepositorySyncStateManager
import com.levibostian.teller.rule.ClearSharedPreferencesRule
import com.levibostian.teller.rule.MockitoInitRule
import com.levibostian.teller.util.AssertionUtil.Companion.check
import com.levibostian.teller.util.TestUtil.isOnMainThread
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import org.junit.After

import org.junit.Before
import org.junit.Rule
import java.util.*

@RunWith(AndroidJUnit4::class)
class OnlineRepositoryIntegrationTest {

    private val sharedPreferences: SharedPreferences = (ApplicationProvider.getApplicationContext() as Context).getTellerSharedPreferences()

    private lateinit var repository: OnlineRepositoryStub
    private lateinit var requirements: OnlineRepositoryStub.GetRequirements

    private lateinit var compositeDisposable: CompositeDisposable

    private lateinit var syncStateManager: OnlineRepositorySyncStateManager
    private lateinit var refreshManager: OnlineRepositoryRefreshManager
    private val schedulersProvider = TellerSchedulersProvider()

    @get:Rule val mockitoInitMocks = MockitoInitRule(this)
    @get:Rule val clearSharedPreferencesRule = ClearSharedPreferencesRule(sharedPreferences)

    private fun setupDataHasBeenFetchedBefore(dataTooOld: Boolean) {
        val maxAgeOfDateOfRepository = repository.maxAgeOfData.toDate()

        val amountOfTimeToAdd = if (dataTooOld) -1 else 1

        val timeAgo: Date = Calendar.getInstance().apply {
            time = maxAgeOfDateOfRepository
            add(Calendar.MINUTE, amountOfTimeToAdd)
        }.time

        syncStateManager.updateAgeOfData(requirements.tag, timeAgo)
    }

    @Before
    fun setup() {
        compositeDisposable = CompositeDisposable()

        refreshManager = OnlineRepositoryRefreshManagerWrapper()
        syncStateManager = TellerOnlineRepositorySyncStateManager(sharedPreferences)
        requirements = OnlineRepositoryStub.GetRequirements("param")
        repository = OnlineRepositoryStub(syncStateManager, refreshManager, schedulersProvider)
    }

    @After
    fun teardown() {
        repository.dispose() // Very important or else tests pass individually but in suite fail as repo not disposed before next call.
        compositeDisposable.clear()
    }

    @Test
    fun canObserveRepositoryWithoutSettingRequirements_expectFirstStateOfData_noErrors() {
        compositeDisposable += repository.observe()
                .test()
                .awaitCount(1)
                .assertValue(check {
                    assertThat(it).isEqualTo(OnlineDataState.none<String>())
                })
    }

    @Test
    fun neverFetchedDataBefore_setRequirements_fetchBegins() {
        var numberTimesRepositoryAttemptedRefresh = 0
        repository.fetchFreshData_invoke = { numberTimesRepositoryAttemptedRefresh += 1 }

        val fetchFreshData = ReplaySubject.create<OnlineRepository.FetchResponse<String>>()
        val newCache = "newCache"
        fetchFreshData.apply {
            onNext(OnlineRepository.FetchResponse.success(newCache))
            onComplete()
        }
        repository.fetchFreshData_return = fetchFreshData.singleOrError()
        repository.requirements = requirements
        assertThat(numberTimesRepositoryAttemptedRefresh).isEqualTo(1)
    }

    @Test
    fun callObserve_triggersFetchFreshCache() {
        var numberTimesRepositoryAttemptedRefresh = 0
        repository.fetchFreshData_invoke = { numberTimesRepositoryAttemptedRefresh += 1 }

        val fetchFreshData = ReplaySubject.create<OnlineRepository.FetchResponse<String>>()
        val newCache = "newCache"
        fetchFreshData.apply {
            onNext(OnlineRepository.FetchResponse.success(newCache))
            onComplete()
        }
        repository.fetchFreshData_return = fetchFreshData.singleOrError()
        repository.requirements = requirements

        compositeDisposable += repository.observe()
                .test()
                .awaitDone()

        assertThat(numberTimesRepositoryAttemptedRefresh).isEqualTo(2)
    }

    @Test
    @UiThreadTest // call on UI thread to assert thread changes.
    fun saveData_calledOnBackgroundThread() {
        repository.saveData_invoke = {
            assertThat(isOnMainThread()).isFalse()
        }

        val newCache = "newCache"
        val fetchFreshData = ReplaySubject.create<OnlineRepository.FetchResponse<String>>().apply {
            onNext(OnlineRepository.FetchResponse.success(newCache))
            onComplete()
        }
        repository.fetchFreshData_return = fetchFreshData.singleOrError()
        repository.observeCachedData_return = Observable.create { it.onNext(newCache) }

        val testObserver = repository.observe().test()

        repository.requirements = requirements

        // A way to assert that saveData() has been called as we have to observe data after save has been called.
        testObserver
                .awaitDone()
                .dispose()

        assertThat(repository.saveData_count).isEqualTo(1)
    }

    @Test
    // Call on background thread to assert thread changes.
    fun observeCachedData_calledOnUIThread() {
        repository.observeCachedData_invoke = {
            assertThat(isOnMainThread()).isTrue()
        }

        val newCache = "newCache"
        val fetchFreshData = ReplaySubject.create<OnlineRepository.FetchResponse<String>>().apply {
            onNext(OnlineRepository.FetchResponse.success(newCache))
            onComplete()
        }
        repository.fetchFreshData_return = fetchFreshData.singleOrError()
        repository.observeCachedData_return = Observable.never()

        val testObserver = repository.observe().test()

        repository.requirements = requirements

        // A way to assert that saveData() has been called as we have to observe data after save has been called.
        testObserver
                .awaitDone()
                .dispose()

        assertThat(repository.observeCachedData_count).isEqualTo(1)
    }

    @Test
    fun dispose_assertAllTearDownComplete() {
        // Make sure fetch call begins and does not finish
        setupDataHasBeenFetchedBefore(dataTooOld = true)
        val fetchFreshData = PublishSubject.create<OnlineRepository.FetchResponse<String>>()
        repository.fetchFreshData_return = fetchFreshData.singleOrError()

        // Make sure observe cache begins and does not complete.
        val observeCachedData = Observable.create<String> {}
        repository.observeCachedData_return = observeCachedData
        repository.requirements = requirements

        val testObserver = repository.observe().test()
        val fetchTestObserver = fetchFreshData.test()
        val observeCachedDataTestObserver = observeCachedData.test()

        repository.dispose()

        compositeDisposable += testObserver
                .await()
                .assertComplete()

        fetchTestObserver
                .awaitDispose()

        observeCachedDataTestObserver
                .awaitDispose()

        assertThat(fetchTestObserver.isDisposed).isTrue()
        assertThat(observeCachedDataTestObserver.isDisposed).isTrue()
    }

    @Test
    fun setRequirements_currentRefreshGetsCancelled() {
        val fetchFreshData = PublishSubject.create<OnlineRepository.FetchResponse<String>>()
        repository.fetchFreshData_return = fetchFreshData.singleOrError()

        repository.requirements = requirements

        val fetchFreshDataTestObserver = fetchFreshData.test()

        repository.requirements = requirements

        fetchFreshDataTestObserver
                .awaitDispose()

        assertThat(fetchFreshDataTestObserver.isDisposed).isTrue()
    }

    @Test
    fun setRequirements_stopObservingOldCache() {
        setupDataHasBeenFetchedBefore(dataTooOld = false)

        val observeCachedData = Observable.create<String> {}
        repository.observeCachedData_return = observeCachedData
        repository.requirements = requirements

        val observeCachedDataTestObserver = observeCachedData.test()

        repository.requirements = requirements

        observeCachedDataTestObserver
                .awaitDispose()

        assertThat(observeCachedDataTestObserver.isDisposed).isTrue()
    }

    @Test
    fun setRequirementsNull_expectObserveStateOfDataNone() {
        setupDataHasBeenFetchedBefore(dataTooOld = false)
        val expectedEventsSequence = arrayListOf<OnlineDataState<String>>()

        val testObserver = repository.observe().test()

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    add(OnlineDataState.none())
                })

        val cache = "cache"
        repository.observeCachedData_return = ReplaySubject.create<String>().apply {
            onNext(cache)
        }
        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 2)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            OnlineDataStateStateMachine
                                    .cacheExists(requirements, syncStateManager.lastTimeFetchedData(requirements.tag)!!),
                            OnlineDataStateStateMachine
                                    .cacheExists<String>(requirements, syncStateManager.lastTimeFetchedData(requirements.tag)!!).change()
                                    .cachedData(cache)
                    ))
                })

        repository.requirements = null

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            OnlineDataState.none()
                    ))
                })
    }

    @Test
    fun observe_changeRequirementsToNilAndBack_continueObserving() {
        val testObserver = repository.observe()
                .test()

        repository.fetchFreshData_return = Single.never()
        repository.requirements = requirements

        repository.requirements = null

        repository.fetchFreshData_return = Single.never()
        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(6)
                .assertValueSequence(listOf(
                        OnlineDataState.none(),
                        OnlineDataStateStateMachine.noCacheExists(requirements),
                        OnlineDataStateStateMachine.noCacheExists<String>(requirements).change()
                                .firstFetch(),
                        OnlineDataState.none(),
                        OnlineDataStateStateMachine.noCacheExists(requirements),
                        OnlineDataStateStateMachine.noCacheExists<String>(requirements).change()
                                .firstFetch()
                ))
    }

    @Test
    fun cacheExistsButTooOld_setRequirementsBeginRefreshAndObserveNewCacheAfter() {
        setupDataHasBeenFetchedBefore(dataTooOld = true)
        val expectedEventsSequence = arrayListOf<OnlineDataState<String>>()

        val testObserver = repository.observe().test()

        val fetchResponse = "new cache"
        repository.observeCachedData_return = Observable.create { it.onNext(fetchResponse) }

        val fetchCache = ReplaySubject.create<OnlineRepository.FetchResponse<String>>()
        repository.fetchFreshData_return = fetchCache.singleOrError()
        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 4)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            OnlineDataState.none(),
                            OnlineDataStateStateMachine.cacheExists(requirements, syncStateManager.lastTimeFetchedData(requirements.tag)!!),
                            OnlineDataStateStateMachine
                                    .cacheExists<String>(requirements, syncStateManager.lastTimeFetchedData(requirements.tag)!!).change()
                                    .cachedData(fetchResponse),
                            OnlineDataStateStateMachine
                                    .cacheExists<String>(requirements, syncStateManager.lastTimeFetchedData(requirements.tag)!!).change()
                                    .cachedData(fetchResponse).change()
                                    .fetchingFreshCache()
                    ))
                })

        assertThat(repository.observeCachedData_count).isEqualTo(1)

        fetchCache.apply {
            onNext(OnlineRepository.FetchResponse.success(fetchResponse))
            onComplete()
        }

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 2)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            OnlineDataStateStateMachine
                                    .cacheExists<String>(requirements, syncStateManager.lastTimeFetchedData(requirements.tag)!!).change()
                                    .cachedData(fetchResponse).change()
                                    .fetchingFreshCache().change()
                                    .successfulFetchingFreshCache(syncStateManager.lastTimeFetchedData(requirements.tag)!!),
                            OnlineDataStateStateMachine
                                    .cacheExists<String>(requirements, syncStateManager.lastTimeFetchedData(requirements.tag)!!).change()
                                    .cachedData(fetchResponse)
                    ))
                })

        assertThat(repository.observeCachedData_count).isEqualTo(2)
    }

    @Test
    fun failedFirstFetch_doesNotBeginObservingCachedData() {
        val fetchFail = OnlineRepository.FetchResponse.ResponseFail("fail")

        val fetchCache = ReplaySubject.create<OnlineRepository.FetchResponse<String>>()
        repository.fetchFreshData_return = fetchCache.singleOrError()
        repository.requirements = requirements

        fetchCache.apply {
            onNext(OnlineRepository.FetchResponse.fail(fetchFail))
            onComplete()
        }

        compositeDisposable += repository.observe()
                .test()
                .awaitDone()

        assertThat(repository.observeCachedData_count).isEqualTo(0)
    }

//    @Test
//    fun exceptionDuringFetch_exceptionHandled() {
//        val fetchException = RuntimeException("fail")
//
//        var errorThrown = false
//        RxJavaPlugins.setErrorHandler {
//            if (it == fetchException) errorThrown = true
//            else throw it
//        }
//
//        val fetchCache = ReplaySubject.create<OnlineRepository.FetchResponse<String>>().apply {
//            onError(fetchException)
//            onComplete()
//        }
//        repository.fetchFreshData_return = fetchCache.singleOrError()
//
//        val testObserver = repository.observe().test()
//
//        repository.requirements = requirements
//
//        testObserver
//                .awaitDone()
//                .dispose()
//
//        assertThat(errorThrown).isTrue()
//    }

    @Test
    fun successfulFirstFetch_observeProcess() {
        val fetchedCache = "new cache"
        val expectedEventsSequence = arrayListOf<OnlineDataState<String>>()

        val testObserver = repository.observe()
                .test()

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    add(OnlineDataState.none())
                })

        val fetchCache = ReplaySubject.create<OnlineRepository.FetchResponse<String>>()
        repository.fetchFreshData_return = fetchCache.singleOrError()

        repository.observeCachedData_return = Observable.create { it.onNext(fetchedCache) }
        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 2)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            OnlineDataStateStateMachine
                                    .noCacheExists(requirements),
                            OnlineDataStateStateMachine
                                    .noCacheExists<String>(requirements).change()
                                    .firstFetch()
                    ))
                })

        fetchCache.apply {
            onNext(OnlineRepository.FetchResponse.success(fetchedCache))
            onComplete()
        }

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 2)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            OnlineDataStateStateMachine
                                    .noCacheExists<String>(requirements).change()
                                    .firstFetch().change()
                                    .successfulFirstFetch(syncStateManager.lastTimeFetchedData(requirements.tag)!!),
                            OnlineDataStateStateMachine
                                    .cacheExists<String>(requirements, syncStateManager.lastTimeFetchedData(requirements.tag)!!).change()
                                    .cachedData(fetchedCache)
                    ))
                })
    }

    @Test
    fun failRefresh_observeProcess() {
        val fetchFail = OnlineRepository.FetchResponse.ResponseFail("fail")
        val expectedEventsSequence = arrayListOf<OnlineDataState<String>>()

        setupDataHasBeenFetchedBefore(dataTooOld = true)

        val testObserver = repository.observe()
                .test()

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    add(OnlineDataState.none())
                })

        val fetchCache = ReplaySubject.create<OnlineRepository.FetchResponse<String>>()
        repository.fetchFreshData_return = fetchCache.singleOrError()

        val existingCache = "existing cache"
        repository.observeCachedData_return = Observable.create { it.onNext(existingCache) }
        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 3)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            OnlineDataStateStateMachine
                                    .cacheExists(requirements, syncStateManager.lastTimeFetchedData(requirements.tag)!!),
                            OnlineDataStateStateMachine
                                    .cacheExists<String>(requirements, syncStateManager.lastTimeFetchedData(requirements.tag)!!).change()
                                    .cachedData(existingCache),
                            OnlineDataStateStateMachine
                                    .cacheExists<String>(requirements, syncStateManager.lastTimeFetchedData(requirements.tag)!!).change()
                                    .cachedData(existingCache).change()
                                    .fetchingFreshCache()
                    ))
                })

        fetchCache.apply {
            onNext(OnlineRepository.FetchResponse.fail(fetchFail))
            onComplete()
        }

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            OnlineDataStateStateMachine
                                    .cacheExists<String>(requirements, syncStateManager.lastTimeFetchedData(requirements.tag)!!).change()
                                    .cachedData(existingCache).change()
                                    .fetchingFreshCache().change()
                                    .failFetchingFreshCache(fetchFail)
                    ))
                })
    }

    @Test
    fun successfulRefresh_observeProcess() {
        val fetchedCache = "new cache"
        val expectedEventsSequence = arrayListOf<OnlineDataState<String>>()

        setupDataHasBeenFetchedBefore(dataTooOld = true)

        val testObserver = repository.observe()
                .test()

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    add(OnlineDataState.none())
                })

        val fetchCache = ReplaySubject.create<OnlineRepository.FetchResponse<String>>()
        repository.fetchFreshData_return = fetchCache.singleOrError()

        val existingCache = "existing cache"
        repository.observeCachedData_return = Observable.create { it.onNext(existingCache) }
        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 3)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            OnlineDataStateStateMachine
                                    .cacheExists(requirements, syncStateManager.lastTimeFetchedData(requirements.tag)!!),
                            OnlineDataStateStateMachine
                                    .cacheExists<String>(requirements, syncStateManager.lastTimeFetchedData(requirements.tag)!!).change()
                                    .cachedData(existingCache),
                            OnlineDataStateStateMachine
                                    .cacheExists<String>(requirements, syncStateManager.lastTimeFetchedData(requirements.tag)!!).change()
                                    .cachedData(existingCache).change()
                                    .fetchingFreshCache()
                    ))
                })

        fetchCache.apply {
            onNext(OnlineRepository.FetchResponse.success(fetchedCache))
            onComplete()
        }

        repository.observeCachedData_return = Observable.create { it.onNext(fetchedCache) }

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 2)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            OnlineDataStateStateMachine
                                    .cacheExists<String>(requirements, syncStateManager.lastTimeFetchedData(requirements.tag)!!).change()
                                    .cachedData(existingCache).change()
                                    .fetchingFreshCache().change()
                                    .successfulFetchingFreshCache(syncStateManager.lastTimeFetchedData(requirements.tag)!!),
                            OnlineDataStateStateMachine
                                    .cacheExists<String>(requirements, syncStateManager.lastTimeFetchedData(requirements.tag)!!).change()
                                    .cachedData(fetchedCache)
                    ))
                })
    }

}
