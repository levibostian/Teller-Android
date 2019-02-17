package com.levibostian.teller.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.test.annotation.UiThreadTest

import org.junit.Test
import org.junit.runner.RunWith

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.datastate.LocalDataState
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
class LocalRepositoryIntegrationTest {

    private val sharedPreferences: SharedPreferences = (ApplicationProvider.getApplicationContext() as Context).getTellerSharedPreferences()

    private lateinit var repository: LocalRepositoryStub
    private lateinit var requirements: LocalRepositoryStub.GetRequirements

    private lateinit var compositeDisposable: CompositeDisposable

    private val schedulersProvider = TellerSchedulersProvider()

    @get:Rule val mockitoInitMocks = MockitoInitRule(this)
    @get:Rule val clearSharedPreferencesRule = ClearSharedPreferencesRule(sharedPreferences)

    @Before
    fun setup() {
        compositeDisposable = CompositeDisposable()

        requirements = LocalRepositoryStub.GetRequirements()
        repository = LocalRepositoryStub(sharedPreferences, schedulersProvider)
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
                    assertThat(it).isEqualTo(LocalDataState.none<String>())
                })
    }

    @Test
    fun setRequirements_observeExistingCache() {
        val expectedEventsSequence = arrayListOf<LocalDataState<String>>()

        val testObserver = repository.observe().test()

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    add(LocalDataState.none())
                })

        val cache = "cache"
        repository.newCache(cache, requirements)

        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 2)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            LocalDataState.data(cache),
                            LocalDataState.data(cache)
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
        val expectedEventsSequence = arrayListOf<LocalDataState<String>>()

        val testObserver = repository.observe().test()

        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 2)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            LocalDataState.none(),
                            LocalDataState.isEmpty()
                    ))
                })

        val observeCachedDataTestObserver = repository.currentObserveCache_observable!!.test()

        repository.dispose()

        compositeDisposable += testObserver
                .await()
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
        val expectedEventsSequence = arrayListOf<LocalDataState<String>>()

        val testObserver = repository.observe().test()

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    add(LocalDataState.none())
                })

        val cache = "cache"
        repository.newCache(cache, requirements)
        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 2)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            LocalDataState.data(cache),
                            LocalDataState.data(cache)
                    ))
                })

        repository.requirements = null

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            LocalDataState.none()
                    ))
                })
    }

    @Test
    fun observe_cacheExists_expectReceiveCache() {
        val cache = "cache"
        repository.newCache(cache, requirements)

        val expectedEventsSequence = arrayListOf<LocalDataState<String>>()

        val testObserver = repository.observe().test()

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    add(LocalDataState.none())
                })

        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            LocalDataState.data(cache)
                    ))
                })
    }

    @Test
    fun observe_cacheEmpty_expectReceiveCache() {
        val cache = ""
        repository.newCache(cache, requirements)

        val expectedEventsSequence = arrayListOf<LocalDataState<String>>()

        val testObserver = repository.observe().test()

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    add(LocalDataState.none())
                })

        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 2)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            LocalDataState.isEmpty(),
                            LocalDataState.isEmpty()
                    ))
                })
    }

    @Test
    fun saveNewCache_observeNewCacheData() {
        val oldCache = "old cache"
        repository.newCache(oldCache, requirements)

        val expectedEventsSequence = arrayListOf<LocalDataState<String>>()

        val testObserver = repository.observe().test()

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    add(LocalDataState.none())
                })

        repository.requirements = requirements

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 2)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            LocalDataState.data(oldCache),
                            LocalDataState.data(oldCache)
                    ))
                })

        val newCache = "new cache"
        repository.newCache(newCache, requirements)

        compositeDisposable += testObserver
                .awaitCount(expectedEventsSequence.size + 1)
                .assertValueSequence(expectedEventsSequence.apply {
                    addAll(listOf(
                            LocalDataState.data(newCache)
                    ))
                })
    }

}
