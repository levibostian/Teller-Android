package com.levibostian.teller.testing

import android.content.Context
import android.content.SharedPreferences
import androidx.test.annotation.UiThreadTest

import org.junit.Test
import org.junit.runner.RunWith

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.cachestate.OnlineCacheState
import com.levibostian.teller.cachestate.online.statemachine.OnlineCacheStateStateMachine
import com.levibostian.teller.extensions.awaitDispose
import com.levibostian.teller.extensions.awaitDone
import com.levibostian.teller.extensions.getTellerSharedPreferences
import com.levibostian.teller.extensions.plusAssign
import com.levibostian.teller.provider.TellerSchedulersProvider
import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.teller.repository.OnlineRepositoryStub
import com.levibostian.teller.repository.manager.OnlineRepositoryRefreshManager
import com.levibostian.teller.repository.manager.OnlineRepositoryRefreshManagerWrapper
import com.levibostian.teller.repository.manager.OnlineRepositoryCacheAgeManager
import com.levibostian.teller.repository.manager.TellerOnlineRepositoryCacheAgeManager
import com.levibostian.teller.rule.ClearSharedPreferencesRule
import com.levibostian.teller.rule.MockitoInitRule
import com.levibostian.teller.testing.repository.OnlineRepositoryTesting
import com.levibostian.teller.type.Age
import com.levibostian.teller.util.AssertionUtil.Companion.check
import com.levibostian.teller.util.TellerTaskExecutor
import com.levibostian.teller.util.TestUtil
import com.levibostian.teller.util.TestUtil.isOnMainThread
import com.levibostian.teller.util.Wait
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import org.junit.After

import org.junit.Before
import org.junit.Rule
import org.mockito.Mock
import java.util.*

@RunWith(AndroidJUnit4::class)
class OnlineRepositoryTestingTest {

    @Mock private lateinit var repository: OnlineRepository<String, OnlineRepository.GetCacheRequirements, String>
    @Mock private lateinit var requirements: OnlineRepository.GetCacheRequirements

    @get:Rule val mockitoInitMocks = MockitoInitRule(this)

    @Mock private lateinit var cacheAgeManager: OnlineRepositoryCacheAgeManager

    private lateinit var onlineRepositoryTesting: OnlineRepositoryTesting

    @Before
    fun setup() {
        onlineRepositoryTesting = OnlineRepositoryTesting(cacheAgeManager)
    }

    @Test
    @UiThreadTest // Make sure to run on UI thread to test we save on background thread.
    fun initStateAsync_cache_notTooOld_expectSaveCacheBackgroundThread() {
        val wait = Wait.times(2)
        val cache = "cache"

        whenever(repository.saveCacheSyncCurrentThread(cache, requirements)).thenAnswer {
            assertThat(TestUtil.isOnMainThread()).isFalse()

            wait.countDown()
        }

        val maxAgeOfCache = Age(1, Age.Unit.HOURS)
        repository.maxAgeOfCache = maxAgeOfCache

        onlineRepositoryTesting.initStateAsync(repository, requirements, {
            cache(cache) {
                cacheNotTooOld()
            }
        }, {
            wait.countDown()
        })

        wait.await()
    }

}
