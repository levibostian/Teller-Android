package com.levibostian.teller.testing.repository

import com.levibostian.teller.truth.DateSubject.Companion.assertThat
import com.levibostian.teller.repository.TellerRepository
import com.levibostian.teller.repository.RepositoryCache
import com.levibostian.teller.repository.DataSourceFetchResponse
import com.levibostian.teller.repository.manager.RepositoryCacheAgeManager
import com.levibostian.teller.type.Age
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.argumentCaptor
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class TellerRepositoryTestingTest {

    @Mock private lateinit var requirements: TellerRepository.GetCacheRequirements
    @Mock private lateinit var repository: TellerRepository<RepositoryCache, TellerRepository.GetCacheRequirements, DataSourceFetchResponse>
    @Mock private lateinit var cacheAgeManager: RepositoryCacheAgeManager

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

        verifyNoInteractions(repository)
        verifyNoInteractions(requirements)
    }

    @Test
    fun `initState(), cache empty - expect set state, default last fetched too old`() {
        val maxAgeOfCache = Age(1, Age.Unit.HOURS)
        repository.maxAgeOfCache = maxAgeOfCache

        onlineRepositoryTesting.initState(repository, requirements) {
            cacheEmpty()
        }

        argumentCaptor<Date> {
            verify(cacheAgeManager).updateLastSuccessfulFetch(eq(requirements.tag), capture())
            assertThat(firstValue).isNewerThan(maxAgeOfCache.toDate())
        }

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