package com.levibostian.teller.testing

import androidx.test.annotation.UiThreadTest

import org.junit.Test
import org.junit.runner.RunWith

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.repository.TellerRepository
import com.levibostian.teller.repository.manager.RepositoryCacheAgeManager
import com.levibostian.teller.rule.MockitoInitRule
import com.levibostian.teller.testing.repository.OnlineRepositoryTesting
import com.levibostian.teller.type.Age
import com.levibostian.teller.util.TestUtil
import com.levibostian.teller.util.Wait
import com.nhaarman.mockito_kotlin.whenever

import org.junit.Before
import org.junit.Rule
import org.mockito.Mock

@RunWith(AndroidJUnit4::class)
class TellerRepositoryTestingTest {

    @Mock private lateinit var repository: TellerRepository<String, TellerRepository.GetCacheRequirements, String>
    @Mock private lateinit var requirements: TellerRepository.GetCacheRequirements

    @get:Rule val mockitoInitMocks = MockitoInitRule(this)

    @Mock private lateinit var cacheAgeManager: RepositoryCacheAgeManager

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
