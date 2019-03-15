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
import com.levibostian.teller.cachestate.LocalCacheState
import com.levibostian.teller.extensions.*
import com.levibostian.teller.provider.TellerSchedulersProvider
import com.levibostian.teller.rule.ClearSharedPreferencesRule
import com.levibostian.teller.rule.MockitoInitRule
import com.levibostian.teller.util.AssertionUtil.Companion.check
import com.levibostian.teller.util.TellerTaskExecutor
import com.levibostian.teller.util.TestUtil.isOnMainThread
import io.reactivex.disposables.CompositeDisposable
import org.junit.After

import org.junit.Before
import org.junit.Rule

@RunWith(AndroidJUnit4::class)
class LocalRepositoryIntegrationTest {

    private val sharedPreferences: SharedPreferences = (ApplicationProvider.getApplicationContext() as Context).getTellerSharedPreferences()

    private lateinit var repository: LocalRepositoryStub
    private lateinit var requirements: LocalRepositoryStub.GetRequirements

    private lateinit var compositeDisposable: CompositeDisposable

    private val schedulersProvider = TellerSchedulersProvider()
    private val taskExecutor = TellerTaskExecutor()
    private val teller = Teller.getTestingInstance(sharedPreferences)

    @get:Rule val mockitoInitMocks = MockitoInitRule(this)
    @get:Rule val clearSharedPreferencesRule = ClearSharedPreferencesRule(sharedPreferences)

    @Before
    fun setup() {
        compositeDisposable = CompositeDisposable()

        requirements = LocalRepositoryStub.GetRequirements()
        repository = LocalRepositoryStub(sharedPreferences, schedulersProvider, taskExecutor, teller)
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
                    assertThat(it).isEqualTo(LocalCacheState.none<String>())
                })
    }

    @Test
    fun setRequirements_observeExistingCache() {
        val cache = "cache"
        repository.setExistingCache(cache)

        val expectedEventsSequence = arrayListOf<LocalCacheState<String>>()

        val testObserver = repository.observe().test()

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    add(LocalCacheState.none())
                })

        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            LocalCacheState.cache(requirements, cache)
                    ))
                })
    }

    @Test
    @UiThreadTest // call on UI thread to assert thread changes.
    fun saveData_calledOnBackgroundThread() {
        repository.saveCache_invoke = {
            assertThat(isOnMainThread()).isFalse()
        }

        val testObserver = repository.observe().test()

        val cache = "cache"
        repository.newCache(cache, requirements)

        repository.requirements = requirements

        testObserver
                .awaitDone()
                .dispose()

        assertThat(repository.saveCache_count).isEqualTo(1)
    }

    @Test
    // Call on background thread to assert thread changes.
    fun observeCachedData_calledOnUIThread() {
        repository.observeCachedData_invoke = {
            assertThat(isOnMainThread()).isTrue()
        }

        val testObserver = repository.observe().test()

        repository.requirements = requirements

        testObserver
                .awaitDone()
                .dispose()

        assertThat(repository.observeCachedData_count).isEqualTo(1)
    }

    @Test
    fun dispose_assertAllTearDownComplete() {
        val expectedEventsSequence = arrayListOf<LocalCacheState<String>>()

        val testObserver = repository.observe().test()

        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 2)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            LocalCacheState.none(),
                            LocalCacheState.isEmpty(requirements)
                    ))
                })

        val observeCachedDataTestObserver = repository.currentObserveCache_observable!!.test()

        repository.dispose()

        compositeDisposable += testObserver
                .awaitComplete()
                .assertComplete()

        observeCachedDataTestObserver
                .awaitDispose()

        assertThat(observeCachedDataTestObserver.isDisposed).isTrue()
    }

    @Test
    fun setRequirements_stopObservingOldCache() {
        repository.requirements = requirements

        val testObserver = repository.observe().test()

        testObserver
                .awaitDone()
                .dispose()

        val observeCachedDataTestObserver = repository.currentObserveCache_observable!!.test()

        repository.requirements = requirements

        observeCachedDataTestObserver
                .awaitDispose()

        assertThat(observeCachedDataTestObserver.isDisposed).isTrue()
    }

    @Test
    fun setRequirementsNull_expectObserveStateOfDataNone() {
        val cache = "cache"
        repository.setExistingCache(cache)

        val expectedEventsSequence = arrayListOf<LocalCacheState<String>>()

        val testObserver = repository.observe().test()

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    add(LocalCacheState.none())
                })

        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            LocalCacheState.cache(requirements, cache)
                    ))
                })

        repository.requirements = null

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            LocalCacheState.none()
                    ))
                })
    }

    @Test
    fun observe_cacheExists_expectReceiveCache() {
        val cache = "cache"
        repository.setExistingCache(cache)

        val expectedEventsSequence = arrayListOf<LocalCacheState<String>>()

        val testObserver = repository.observe().test()

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    add(LocalCacheState.none())
                })

        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            LocalCacheState.cache(requirements, cache)
                    ))
                })
    }

    @Test
    fun observe_cacheEmpty_expectReceiveCache() {
        val cache = ""
        repository.setExistingCache(cache)

        val expectedEventsSequence = arrayListOf<LocalCacheState<String>>()

        val testObserver = repository.observe().test()

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    add(LocalCacheState.none())
                })

        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            LocalCacheState.isEmpty(requirements)
                    ))
                })
    }

    @Test
    fun saveNewCache_observeNewCacheData() {
        val oldCache = "old cache"
        repository.setExistingCache(oldCache)

        val expectedEventsSequence = arrayListOf<LocalCacheState<String>>()

        val testObserver = repository.observe().test()

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    add(LocalCacheState.none())
                })

        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            LocalCacheState.cache(requirements, oldCache)
                    ))
                })

        val newCache = "new cache"
        repository.newCache(newCache, requirements)

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            LocalCacheState.cache(requirements, newCache)
                    ))
                })
    }

}
