package com.levibostian.teller.testing.repository

import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.Teller
import com.levibostian.teller.truth.DateSubject.Companion.assertThat
import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.teller.repository.OnlineRepositoryCache
import com.levibostian.teller.repository.OnlineRepositoryFetchResponse
import com.levibostian.teller.repository.manager.OnlineRepositoryCacheAgeManager
import com.levibostian.teller.testing.extensions.failure
import com.levibostian.teller.testing.extensions.skipped
import com.levibostian.teller.testing.extensions.success
import com.levibostian.teller.truth.DateSubject
import com.levibostian.teller.type.Age
import com.nhaarman.mockito_kotlin.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyString
import org.mockito.junit.MockitoJUnitRunner
import java.util.*
import kotlin.math.max

@RunWith(MockitoJUnitRunner::class)
class OnlineRepositoryTestingTest {

    @Mock private lateinit var requirements: OnlineRepository.GetCacheRequirements
    @Mock private lateinit var repository: OnlineRepository<OnlineRepositoryCache, OnlineRepository.GetCacheRequirements, OnlineRepositoryFetchResponse>
    @Mock private lateinit var cacheAgeManager: OnlineRepositoryCacheAgeManager

    private lateinit var onlineRepositoryTesting: OnlineRepositoryTesting

    @Before
    fun setup() {
        onlineRepositoryTesting = OnlineRepositoryTesting(cacheAgeManager)
    }

    @Test
    fun `initState(), no cache - expect set state`() {
        onlineRepositoryTesting.initState(repository, requirements) {
            noCache()
        }

        verifyZeroInteractions(repository)
        verifyZeroInteractions(requirements)
    }

    @Test
    fun `initState(), cache empty - expect set state, default last fetched too old`() {
        val maxAgeOfCache = Age(1, Age.Unit.HOURS)
        repository.maxAgeOfCache = maxAgeOfCache

        onlineRepositoryTesting.initState(repository, requirements) {
            cacheEmpty()
        }

        val setLastFetchedCaptor = argumentCaptor<Date>()
        verify(cacheAgeManager).updateLastSuccessfulFetch(eq(requirements.tag), setLastFetchedCaptor.capture())
        assertThat(setLastFetchedCaptor.firstValue).isNewerThan(maxAgeOfCache.toDate())

        verify(repository, never()).saveCacheSyncCurrentThread(anyString(), eq(requirements))
    }

    @Test
    fun `initState(), cache - expect set state, default last fetched too old`() {
        val maxAgeOfCache = Age(1, Age.Unit.HOURS)
        repository.maxAgeOfCache = maxAgeOfCache

        val cache = "cache"
        onlineRepositoryTesting.initState(repository, requirements) {
            cache(cache)
        }

        val setLastFetchedCaptor = argumentCaptor<Date>()
        verify(cacheAgeManager).updateLastSuccessfulFetch(eq(requirements.tag), setLastFetchedCaptor.capture())
        assertThat(setLastFetchedCaptor.firstValue).isNewerThan(maxAgeOfCache.toDate())

        verify(repository).saveCacheSyncCurrentThread(cache, requirements)
    }

    @Test
    fun `initState(), cache too old - expect set state`() {
        val maxAgeOfCache = Age(1, Age.Unit.HOURS)
        `when`(repository.maxAgeOfCache).thenReturn(maxAgeOfCache)

        onlineRepositoryTesting.initState(repository, requirements) {
            cacheEmpty {
                cacheTooOld()
            }
        }

        val setLastFetchedCaptor = argumentCaptor<Date>()
        verify(cacheAgeManager).updateLastSuccessfulFetch(eq(requirements.tag), setLastFetchedCaptor.capture())
        assertThat(setLastFetchedCaptor.firstValue).isOlderThan(maxAgeOfCache.toDate())

        verify(repository, never()).saveCacheSyncCurrentThread(anyString(), eq(requirements))
    }

    @Test
    fun `initState(), cache not too old - expect set state`() {
        val maxAgeOfCache = Age(1, Age.Unit.HOURS)
        repository.maxAgeOfCache = maxAgeOfCache

        onlineRepositoryTesting.initState(repository, requirements) {
            cacheEmpty {
                cacheNotTooOld()
            }
        }

        val setLastFetchedCaptor = argumentCaptor<Date>()
        verify(cacheAgeManager).updateLastSuccessfulFetch(eq(requirements.tag), setLastFetchedCaptor.capture())
        assertThat(setLastFetchedCaptor.firstValue).isNewerThan(maxAgeOfCache.toDate())

        verify(repository, never()).saveCacheSyncCurrentThread(anyString(), eq(requirements))
    }

    /**
     * Commenting out until [OnlineRepositoryTesting.CacheExistsDsl.lastFetched] compiles successfully.
     */
//    @Test
//    fun `initState(), set last fetched - expect set state`() {
//        val maxAgeOfCache = Age(1, Age.Unit.HOURS)
//        `when`(repository.maxAgeOfCache).thenReturn(maxAgeOfCache)
//
//        val lastSuccessfulFetch = Calendar.getInstance().apply {
//            add(Calendar.DATE, -100)
//        }.time
//
//        onlineRepositoryTesting.initState(repository, requirements) {
//            cacheExistsAndEmpty {
//                lastFetched(lastSuccessfulFetch)
//            }
//        }
//
//        val setLastFetchedCaptor = argumentCaptor<Date>()
//        verify(cacheAgeManager).updateLastSuccessfulFetch(eq(requirements.tag), setLastFetchedCaptor.capture())
//        assertThat(setLastFetchedCaptor.firstValue).isEqualTo(lastSuccessfulFetch)
//
//        verify(repository, never()).saveCacheSyncCurrentThread(ArgumentMatchers.anyString(), eq(requirements))
//    }

}