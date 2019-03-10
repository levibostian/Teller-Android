package com.levibostian.teller.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.test.annotation.UiThreadTest

import org.junit.Test
import org.junit.runner.RunWith

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.Teller
import com.levibostian.teller.cachestate.OnlineCacheState
import com.levibostian.teller.cachestate.online.statemachine.OnlineCacheStateStateMachine
import com.levibostian.teller.extensions.awaitDispose
import com.levibostian.teller.extensions.awaitDone
import com.levibostian.teller.extensions.getTellerSharedPreferences
import com.levibostian.teller.extensions.plusAssign
import com.levibostian.teller.provider.TellerSchedulersProvider
import com.levibostian.teller.repository.manager.OnlineRepositoryRefreshManager
import com.levibostian.teller.repository.manager.OnlineRepositoryRefreshManagerWrapper
import com.levibostian.teller.repository.manager.OnlineRepositoryCacheAgeManager
import com.levibostian.teller.repository.manager.TellerOnlineRepositoryCacheAgeManager
import com.levibostian.teller.rule.ClearSharedPreferencesRule
import com.levibostian.teller.rule.MockitoInitRule
import com.levibostian.teller.util.AssertionUtil.Companion.check
import com.levibostian.teller.util.TellerTaskExecutor
import com.levibostian.teller.util.TestUtil.isOnMainThread
import com.levibostian.teller.util.Wait
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

    private lateinit var cacheAgeManager: OnlineRepositoryCacheAgeManager
    private lateinit var refreshManager: OnlineRepositoryRefreshManager
    private val schedulersProvider = TellerSchedulersProvider()
    private val taskExecutor = TellerTaskExecutor()
    private val refreshManagerListener = object : OnlineRepositoryRefreshManager.Listener {
        override fun refreshBegin(tag: GetCacheRequirementsTag) {
            repository.refreshBegin(tag)
        }
        override fun <RefreshResponseType: OnlineRepositoryFetchResponse> refreshComplete(tag: GetCacheRequirementsTag, response: OnlineRepository.FetchResponse<RefreshResponseType>) {
            repository.refreshComplete(tag, response as OnlineRepository.FetchResponse<String>)
        }
    }

    private val teller = Teller.getTestingInstance(sharedPreferences)

    @get:Rule val mockitoInitMocks = MockitoInitRule(this)
    @get:Rule val clearSharedPreferencesRule = ClearSharedPreferencesRule(sharedPreferences)

    private fun setupCacheHasBeenFetchedBefore(cacheTooOld: Boolean) {
        val maxAgeOfDateOfRepository = repository.maxAgeOfCache.toDate()

        val amountOfTimeToAdd = if (cacheTooOld) -1 else 1

        val timeAgo: Date = Calendar.getInstance().apply {
            time = maxAgeOfDateOfRepository
            add(Calendar.MINUTE, amountOfTimeToAdd)
        }.time

        cacheAgeManager.updateLastSuccessfulFetch(requirements.tag, timeAgo)
    }

    @Before
    fun setup() {
        compositeDisposable = CompositeDisposable()

        refreshManager = OnlineRepositoryRefreshManagerWrapper()
        cacheAgeManager = TellerOnlineRepositoryCacheAgeManager(sharedPreferences)
        requirements = OnlineRepositoryStub.GetRequirements("param")
        repository = OnlineRepositoryStub(cacheAgeManager, refreshManager, schedulersProvider, taskExecutor, refreshManagerListener, teller)
    }

    @After
    fun teardown() {
        repository.dispose() // Very important or else tests pass individually but in suite fail as repo not disposed before next call.
        compositeDisposable.clear()
    }

    @Test
    fun canObserveRepositoryWithoutSettingRequirements_expectFirstStateOfCache_noErrors() {
        compositeDisposable += repository.observe()
                .test()
                .awaitCount(1)
                .assertValue(check {
                    assertThat(it).isEqualTo(OnlineCacheState.none<String>())
                })
    }

    @Test
    fun neverFetchedCacheBefore_setRequirements_fetchBegins() {
        var numberTimesRepositoryAttemptedRefresh = 0
        repository.fetchFreshCache_invoke = { numberTimesRepositoryAttemptedRefresh += 1 }

        val fetchFreshCache = ReplaySubject.create<OnlineRepository.FetchResponse<String>>()
        val newCache = "newCache"
        fetchFreshCache.apply {
            onNext(OnlineRepository.FetchResponse.success(newCache))
            onComplete()
        }
        repository.fetchFreshCache_return = fetchFreshCache.singleOrError()
        repository.requirements = requirements
        assertThat(numberTimesRepositoryAttemptedRefresh).isEqualTo(1)
    }

    @Test
    fun callObserve_triggersFetchFreshCache() {
        // Must use 2 waits to prevent flaky test. Assert fetch happens when setting requirements first, then run function under test.
        val firstFetchWait = Wait.times(1)
        val secondFetchWait = Wait.times(2)

        repository.fetchFreshCache_invoke = {
            firstFetchWait.countDown()
            secondFetchWait.countDown()
        }

        val newCache = "newCache"
        val fetchFreshCache = ReplaySubject.create<OnlineRepository.FetchResponse<String>>().apply {
            onNext(OnlineRepository.FetchResponse.success(newCache))
            onComplete()
        }
        repository.fetchFreshCache_return = fetchFreshCache.singleOrError()
        repository.requirements = requirements

        firstFetchWait.await()

        compositeDisposable += repository.observe()
                .test()
                .awaitDone()

        secondFetchWait.await()
    }

    @Test
    @UiThreadTest // call on UI thread to assert thread changes.
    fun saveCache_calledOnBackgroundThread() {
        val wait = Wait.times(1)

        repository.saveCache_invoke = {
            assertThat(isOnMainThread()).isFalse()
            wait.countDown()
        }

        val newCache = "newCache"
        val fetchFreshCache = ReplaySubject.create<OnlineRepository.FetchResponse<String>>().apply {
            onNext(OnlineRepository.FetchResponse.success(newCache))
            onComplete()
        }
        repository.fetchFreshCache_return = fetchFreshCache.singleOrError()
        repository.observeCache_return = Observable.create { it.onNext(newCache) }

        repository.requirements = requirements

        wait.await()

        assertThat(repository.saveCache_count).isEqualTo(1)
    }

    @Test
    // Call on background thread to assert thread changes.
    fun observeCache_calledOnUIThread() {
        val wait = Wait.times(1)

        setupCacheHasBeenFetchedBefore(cacheTooOld = false)

        repository.observeCache_invoke = {
            assertThat(isOnMainThread()).isTrue()
            wait.countDown()
        }

        repository.observeCache_return = Observable.never()

        repository.requirements = requirements

        wait.await()

        assertThat(repository.observeCache_count).isEqualTo(1)
    }

    @Test
    fun dispose_assertAllTearDownComplete() {
        // Make sure fetch call begins and does not finish
        setupCacheHasBeenFetchedBefore(cacheTooOld = true)
        val fetchFreshCache = PublishSubject.create<OnlineRepository.FetchResponse<String>>()
        repository.fetchFreshCache_return = fetchFreshCache.singleOrError()

        // Make sure observe cache begins and does not complete.
        val observeCache = Observable.create<String> {}
        repository.observeCache_return = observeCache
        repository.requirements = requirements

        val testObserver = repository.observe().test()
        val fetchTestObserver = fetchFreshCache.test()
        val observeCacheTestObserver = observeCache.test()

        repository.dispose()

        compositeDisposable += testObserver
                .await()
                .assertComplete()

        fetchTestObserver
                .awaitDispose()

        observeCacheTestObserver
                .awaitDispose()

        assertThat(fetchTestObserver.isDisposed).isTrue()
        assertThat(observeCacheTestObserver.isDisposed).isTrue()
    }

    @Test
    fun setRequirements_currentRefreshGetsCancelled() {
        val fetchFreshCache = PublishSubject.create<OnlineRepository.FetchResponse<String>>()
        repository.fetchFreshCache_return = fetchFreshCache.singleOrError()

        repository.requirements = requirements

        val fetchFreshCacheTestObserver = fetchFreshCache.test()

        repository.requirements = requirements

        fetchFreshCacheTestObserver
                .awaitDispose()

        assertThat(fetchFreshCacheTestObserver.isDisposed).isTrue()
    }

    @Test
    fun setRequirements_stopObservingOldCache() {
        setupCacheHasBeenFetchedBefore(cacheTooOld = false)

        val observeCache = Observable.create<String> {}
        repository.observeCache_return = observeCache
        repository.requirements = requirements

        val observeCacheTestObserver = observeCache.test()

        repository.requirements = requirements

        observeCacheTestObserver
                .awaitDispose()

        assertThat(observeCacheTestObserver.isDisposed).isTrue()
    }

    @Test
    fun setRequirementsNull_expectObserveStateOfCacheNone() {
        setupCacheHasBeenFetchedBefore(cacheTooOld = false)
        val expectedEventsSequence = arrayListOf<OnlineCacheState<String>>()

        val testObserver = repository.observe().test()

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    add(OnlineCacheState.none())
                })

        val cache = "cache"
        repository.observeCache_return = ReplaySubject.create<String>().apply {
            onNext(cache)
        }
        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 2)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            OnlineCacheStateStateMachine
                                    .cacheExists(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!),
                            OnlineCacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(cache)
                    ))
                })

        repository.requirements = null

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            OnlineCacheState.none()
                    ))
                })
    }

    @Test
    fun observe_changeRequirementsToNilAndBack_continueObserving() {
        val testObserver = repository.observe()
                .test()

        repository.fetchFreshCache_return = Single.never()
        repository.requirements = requirements

        repository.requirements = null

        repository.fetchFreshCache_return = Single.never()
        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(6)
                .assertValueSequence(listOf(
                        OnlineCacheState.none(),
                        OnlineCacheStateStateMachine.noCacheExists(requirements),
                        OnlineCacheStateStateMachine.noCacheExists<String>(requirements).change()
                                .firstFetch(),
                        OnlineCacheState.none(),
                        OnlineCacheStateStateMachine.noCacheExists(requirements),
                        OnlineCacheStateStateMachine.noCacheExists<String>(requirements).change()
                                .firstFetch()
                ))
    }

    @Test
    fun cacheExistsButTooOld_setRequirementsBeginRefreshAndObserveNewCacheAfter() {
        setupCacheHasBeenFetchedBefore(cacheTooOld = true)
        val expectedEventsSequence = arrayListOf<OnlineCacheState<String>>()

        val testObserver = repository.observe().test()

        val fetchResponse = "new cache"
        repository.observeCache_return = Observable.create { it.onNext(fetchResponse) }

        val fetchCache = ReplaySubject.create<OnlineRepository.FetchResponse<String>>()
        repository.fetchFreshCache_return = fetchCache.singleOrError()
        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 4)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            OnlineCacheState.none(),
                            OnlineCacheStateStateMachine.cacheExists(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!),
                            OnlineCacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(fetchResponse),
                            OnlineCacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(fetchResponse).change()
                                    .fetchingFreshCache()
                    ))
                })

        assertThat(repository.observeCache_count).isEqualTo(1)

        fetchCache.apply {
            onNext(OnlineRepository.FetchResponse.success(fetchResponse))
            onComplete()
        }

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 2)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            OnlineCacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(fetchResponse).change()
                                    .fetchingFreshCache().change()
                                    .successfulRefreshCache(cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!),
                            OnlineCacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(fetchResponse)
                    ))
                })

        assertThat(repository.observeCache_count).isEqualTo(2)
    }

    @Test
    fun failedFirstFetch_doesNotBeginObservingCache() {
        val fetchFail = RuntimeException("fail")

        val fetchCache = ReplaySubject.create<OnlineRepository.FetchResponse<String>>()
        repository.fetchFreshCache_return = fetchCache.singleOrError()
        repository.requirements = requirements

        fetchCache.apply {
            onNext(OnlineRepository.FetchResponse.fail(fetchFail))
            onComplete()
        }

        compositeDisposable += repository.observe()
                .test()
                .awaitDone()

        assertThat(repository.observeCache_count).isEqualTo(0)
    }

    @Test
    fun successfulFirstFetch_observeProcess() {
        val fetchedCache = "new cache"
        val expectedEventsSequence = arrayListOf<OnlineCacheState<String>>()

        val testObserver = repository.observe()
                .test()

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    add(OnlineCacheState.none())
                })

        val fetchCache = ReplaySubject.create<OnlineRepository.FetchResponse<String>>()
        repository.fetchFreshCache_return = fetchCache.singleOrError()

        repository.observeCache_return = Observable.create { it.onNext(fetchedCache) }
        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 2)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            OnlineCacheStateStateMachine
                                    .noCacheExists(requirements),
                            OnlineCacheStateStateMachine
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
                            OnlineCacheStateStateMachine
                                    .noCacheExists<String>(requirements).change()
                                    .firstFetch().change()
                                    .successfulFirstFetch(cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!),
                            OnlineCacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(fetchedCache)
                    ))
                })
    }

    @Test
    fun failRefresh_observeProcess() {
        val fetchFail = RuntimeException("fail")
        val expectedEventsSequence = arrayListOf<OnlineCacheState<String>>()

        setupCacheHasBeenFetchedBefore(cacheTooOld = true)

        val testObserver = repository.observe()
                .test()

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    add(OnlineCacheState.none())
                })

        val fetchCache = ReplaySubject.create<OnlineRepository.FetchResponse<String>>()
        repository.fetchFreshCache_return = fetchCache.singleOrError()

        val existingCache = "existing cache"
        repository.observeCache_return = Observable.create { it.onNext(existingCache) }
        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 3)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            OnlineCacheStateStateMachine
                                    .cacheExists(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!),
                            OnlineCacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(existingCache),
                            OnlineCacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(existingCache).change()
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
                            OnlineCacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(existingCache).change()
                                    .fetchingFreshCache().change()
                                    .failRefreshCache(fetchFail)
                    ))
                })
    }

    @Test
    fun successfulRefresh_observeProcess() {
        val expectedEventsSequence = arrayListOf<OnlineCacheState<String>>()

        setupCacheHasBeenFetchedBefore(cacheTooOld = true)

        val testObserver = repository.observe()
                .test()

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    add(OnlineCacheState.none())
                })

        val fetchCache = ReplaySubject.create<OnlineRepository.FetchResponse<String>>()
        repository.fetchFreshCache_return = fetchCache.singleOrError()

        val existingCache = "existing cache"
        repository.observeCache_return = Observable.create { it.onNext(existingCache) }
        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 3)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            OnlineCacheStateStateMachine
                                    .cacheExists(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!),
                            OnlineCacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(existingCache),
                            OnlineCacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(existingCache).change()
                                    .fetchingFreshCache()
                    ))
                })

        fetchCache.apply {
            onNext(OnlineRepository.FetchResponse.success(existingCache)) // returning existing as cannot change what observable in stub is returning.
            onComplete()
        }

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 2)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            OnlineCacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(existingCache).change()
                                    .fetchingFreshCache().change()
                                    .successfulRefreshCache(cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!),
                            OnlineCacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(existingCache)
                    ))
                })
    }

}
