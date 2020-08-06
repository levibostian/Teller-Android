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
import com.levibostian.teller.cachestate.CacheState
import com.levibostian.teller.cachestate.statemachine.CacheStateStateMachine
import com.levibostian.teller.extensions.*
import com.levibostian.teller.provider.TellerSchedulersProvider
import com.levibostian.teller.repository.manager.RepositoryRefreshManager
import com.levibostian.teller.repository.manager.RepositoryRefreshManagerWrapper
import com.levibostian.teller.repository.manager.RepositoryCacheAgeManager
import com.levibostian.teller.repository.manager.TellerRepositoryCacheAgeManager
import com.levibostian.teller.rule.ClearSharedPreferencesRule
import com.levibostian.teller.rule.MockitoInitRule
import com.levibostian.teller.testing.extensions.initState
import com.levibostian.teller.testing.extensions.noCache
import com.levibostian.teller.testing.extensions.none
import com.levibostian.teller.util.AssertionUtil.Companion.check
import com.levibostian.teller.util.TellerTaskExecutor
import com.levibostian.teller.util.TestUtil.isOnMainThread
import com.levibostian.teller.util.Wait
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import org.junit.After

import org.junit.Before
import org.junit.Rule

@RunWith(AndroidJUnit4::class)
class TellerRepositoryIntegrationTest {

    private val sharedPreferences: SharedPreferences = (ApplicationProvider.getApplicationContext() as Context).getTellerSharedPreferences()

    private lateinit var repository: TellerRepositoryStub
    private lateinit var requirements: TellerRepositoryStub.GetRequirements

    private lateinit var compositeDisposable: CompositeDisposable

    private lateinit var cacheAgeManager: RepositoryCacheAgeManager
    private lateinit var refreshManager: RepositoryRefreshManager
    private val schedulersProvider = TellerSchedulersProvider()
    private val taskExecutor = TellerTaskExecutor()
    private val refreshManagerListener = object : RepositoryRefreshManager.Listener {
        override fun refreshBegin(tag: GetCacheRequirementsTag) {
            repository.refreshBegin(tag)
        }
        override fun <RefreshResponseType: DataSourceFetchResponse> refreshComplete(tag: GetCacheRequirementsTag, response: TellerRepository.FetchResponse<RefreshResponseType>) {
            repository.refreshComplete(tag, response as TellerRepository.FetchResponse<String>)
        }
    }

    private val teller = Teller.getTestingInstance(sharedPreferences)

    @get:Rule val mockitoInitMocks = MockitoInitRule(this)
    @get:Rule val clearSharedPreferencesRule = ClearSharedPreferencesRule(sharedPreferences)

    private fun setupCacheHasBeenFetchedBefore(cacheTooOld: Boolean, existingCache: String?) {
        TellerRepository.Testing.initState(repository, requirements) {
            if (existingCache != null) {
                cache(existingCache) {
                    if (cacheTooOld) cacheTooOld()
                    else cacheNotTooOld()
                }
            } else {
                cacheEmpty {
                    if (cacheTooOld) cacheTooOld()
                    else cacheNotTooOld()
                }
            }
        }
    }

    @Before
    fun setup() {
        Teller.initTesting(sharedPreferences)
        compositeDisposable = CompositeDisposable()

        refreshManager = RepositoryRefreshManagerWrapper()
        cacheAgeManager = TellerRepositoryCacheAgeManager(sharedPreferences)
        /**
         * *Important:* Each requirements instance should have the same tag for each test (unless a test is choosing to perform a test that requires a different tag) so that way we can use this test class as a way to see what it is like running multiple instances of the same [TellerRepository] at the same time. Bugs have been caught by having all tests use the same tag!
         */
        requirements = TellerRepositoryStub.GetRequirements("param")
        repository = TellerRepositoryStub(sharedPreferences, cacheAgeManager, refreshManager, schedulersProvider, taskExecutor, refreshManagerListener, teller)
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
                    assertThat(it).isEqualTo(CacheState.none<String>())
                })
    }

    @Test
    fun neverFetchedCacheBefore_setRequirements_fetchBegins() {
        val wait = Wait.times(1)
        repository.fetchFreshCache_invoke = {
            wait.countDown()
        }

        val fetchFreshCache = ReplaySubject.create<TellerRepository.FetchResponse<String>>()
        val newCache = "newCache"
        fetchFreshCache.apply {
            onNext(TellerRepository.FetchResponse.success(newCache))
            onComplete()
        }
        repository.fetchFreshCache_return = fetchFreshCache.singleOrError()
        repository.requirements = requirements

        wait.await()
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
        val fetchFreshCache = ReplaySubject.create<TellerRepository.FetchResponse<String>>().apply {
            onNext(TellerRepository.FetchResponse.success(newCache))
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
        val fetchFreshCache = ReplaySubject.create<TellerRepository.FetchResponse<String>>().apply {
            onNext(TellerRepository.FetchResponse.success(newCache))
            onComplete()
        }
        repository.fetchFreshCache_return = fetchFreshCache.singleOrError()

        repository.requirements = requirements

        wait.await()
    }

    @Test
    // Call on background thread to assert thread changes.
    fun observeCache_calledOnUIThread() {
        val wait = Wait.times(1)

        setupCacheHasBeenFetchedBefore(cacheTooOld = false, existingCache = null)

        repository.observeCache_invoke = {
            assertThat(isOnMainThread()).isTrue()
            wait.countDown()
        }

        repository.requirements = requirements

        wait.await()
    }

    @Test
    fun dispose_assertAllTearDownComplete() {
        // Make sure fetch call begins and does not finish
        setupCacheHasBeenFetchedBefore(cacheTooOld = true, existingCache = null)
        val fetchFreshCache = ReplaySubject.create<TellerRepository.FetchResponse<String>>()
        repository.fetchFreshCache_return = fetchFreshCache.singleOrError()
        val wait = Wait.times(1)
        repository.requirements = requirements

        val testObserver = repository.observe().test()
        val fetchTestObserver = fetchFreshCache.test()
        repository.observeCache_invoke = {
            wait.countDown()
        }
        wait.await()
        val observeCacheTestObserver = repository.currentObserveCache_observable!!.test()

        repository.dispose()

        compositeDisposable += testObserver
                .awaitComplete()
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
        val fetchFreshCache = PublishSubject.create<TellerRepository.FetchResponse<String>>()
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
        val wait = Wait.times(1)
        setupCacheHasBeenFetchedBefore(cacheTooOld = false, existingCache = null)

        repository.requirements = requirements

        repository.requirements = requirements

        repository.observeCache_invoke = {
            wait.countDown()
        }
        wait.await()
        val observeCacheTestObserver = repository.currentObserveCache_observable!!.test()

        observeCacheTestObserver
                .awaitDispose()

        assertThat(observeCacheTestObserver.isDisposed).isTrue()
    }

    @Test
    fun setRequirementsNull_expectObserveStateOfCacheNone() {
        val cache = "cache"

        setupCacheHasBeenFetchedBefore(cacheTooOld = false, existingCache = cache)
        val expectedEventsSequence = arrayListOf<CacheState<String>>()

        val testObserver = repository.observe().test()

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    add(CacheState.none())
                })

        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 2)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            CacheStateStateMachine
                                    .cacheExists(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!),
                            CacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(cache)
                    ))
                })

        repository.requirements = null

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            CacheState.none()
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
                        CacheState.none(),
                        CacheStateStateMachine.noCacheExists(requirements),
                        CacheStateStateMachine.noCacheExists<String>(requirements).change()
                                .firstFetch(),
                        CacheState.none(),
                        CacheStateStateMachine.noCacheExists(requirements),
                        CacheStateStateMachine.noCacheExists<String>(requirements).change()
                                .firstFetch()
                ))
    }

    @Test
    fun cacheExistsButTooOld_setRequirementsBeginRefreshAndObserveNewCacheAfter() {
        val existingCache = "existing cache"
        setupCacheHasBeenFetchedBefore(cacheTooOld = true, existingCache = existingCache)
        val expectedEventsSequence = arrayListOf<CacheState<String>>()

        val testObserver = repository.observe().test()

        val fetchCache = ReplaySubject.create<TellerRepository.FetchResponse<String>>()
        repository.fetchFreshCache_return = fetchCache.singleOrError()
        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 4)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            CacheState.none(),
                            CacheStateStateMachine.cacheExists(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!),
                            CacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(existingCache),
                            CacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(existingCache).change()
                                    .fetchingFreshCache()
                    ))
                })

        assertThat(repository.observeCache_results).hasSize(1)

        val fetchResponse = "new cache"
        fetchCache.apply {
            onNext(TellerRepository.FetchResponse.success(fetchResponse))
            onComplete()
        }

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 2)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            CacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(existingCache).change()
                                    .fetchingFreshCache().change()
                                    .successfulRefreshCache(cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!),
                            CacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(fetchResponse)
                    ))
                })

        assertThat(repository.observeCache_results).hasSize(2)
    }

    @Test
    fun failedFirstFetch_doesNotBeginObservingCache() {
        val fetchFail = RuntimeException("fail")

        val fetchCache = ReplaySubject.create<TellerRepository.FetchResponse<String>>()
        repository.fetchFreshCache_return = fetchCache.singleOrError()
        repository.requirements = requirements

        fetchCache.apply {
            onNext(TellerRepository.FetchResponse.fail(fetchFail))
            onComplete()
        }

        compositeDisposable += repository.observe()
                .test()
                .awaitDone()

        assertThat(repository.observeCache_results).hasSize(0)
    }

    @Test
    fun successfulFirstFetch_observeProcess() {
        val fetchedCache = "new cache"
        val expectedEventsSequence = arrayListOf<CacheState<String>>()

        val testObserver = repository.observe()
                .test()

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    add(CacheState.Testing.none())
                })

        val fetchCache = ReplaySubject.create<TellerRepository.FetchResponse<String>>()
        repository.fetchFreshCache_return = fetchCache.singleOrError()

        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 2)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            CacheState.Testing.noCache(requirements),
                            CacheState.Testing.noCache(requirements) {
                                fetchingFirstTime()
                            }
                    ))
                })

        fetchCache.apply {
            onNext(TellerRepository.FetchResponse.success(fetchedCache))
            onComplete()
        }

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 2)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            CacheStateStateMachine
                                    .noCacheExists<String>(requirements).change()
                                    .firstFetch().change()
                                    .successfulFirstFetch(cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!),
                            CacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(fetchedCache)
                    ))
                })
    }

    @Test
    fun failRefresh_observeProcess() {
        val fetchFail = RuntimeException("fail")
        val existingCache = "existing cache"
        val expectedEventsSequence = arrayListOf<CacheState<String>>()

        setupCacheHasBeenFetchedBefore(cacheTooOld = true, existingCache = existingCache)

        val testObserver = repository.observe()
                .test()

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    add(CacheState.none())
                })

        val fetchCache = ReplaySubject.create<TellerRepository.FetchResponse<String>>()
        repository.fetchFreshCache_return = fetchCache.singleOrError()

        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 3)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            CacheStateStateMachine
                                    .cacheExists(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!),
                            CacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(existingCache),
                            CacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(existingCache).change()
                                    .fetchingFreshCache()
                    ))
                })

        fetchCache.apply {
            onNext(TellerRepository.FetchResponse.fail(fetchFail))
            onComplete()
        }

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            CacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(existingCache).change()
                                    .fetchingFreshCache().change()
                                    .failRefreshCache(fetchFail)
                    ))
                })
    }

    @Test
    fun successfulRefresh_observeProcess() {
        val existingCache = "existing cache"
        val expectedEventsSequence = arrayListOf<CacheState<String>>()

        setupCacheHasBeenFetchedBefore(cacheTooOld = true, existingCache = existingCache)

        val testObserver = repository.observe()
                .test()

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    add(CacheState.none())
                })

        val fetchCache = ReplaySubject.create<TellerRepository.FetchResponse<String>>()
        repository.fetchFreshCache_return = fetchCache.singleOrError()

        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 3)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            CacheStateStateMachine
                                    .cacheExists(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!),
                            CacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(existingCache),
                            CacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(existingCache).change()
                                    .fetchingFreshCache()
                    ))
                })

        fetchCache.apply {
            onNext(TellerRepository.FetchResponse.success(existingCache)) // returning existing as cannot change what observable in stub is returning.
            onComplete()
        }

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 2)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            CacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(existingCache).change()
                                    .fetchingFreshCache().change()
                                    .successfulRefreshCache(cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!),
                            CacheStateStateMachine
                                    .cacheExists<String>(requirements, cacheAgeManager.lastSuccessfulFetch(requirements.tag)!!).change()
                                    .cache(existingCache)
                    ))
                })
    }

}
