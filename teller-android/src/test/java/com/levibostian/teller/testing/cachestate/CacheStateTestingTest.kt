package com.levibostian.teller.testing.cachestate

import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.cachestate.CacheState
import com.levibostian.teller.cachestate.statemachine.CacheStateStateMachine
import com.levibostian.teller.repository.TellerRepository
import com.levibostian.teller.testing.extensions.cache
import com.levibostian.teller.testing.extensions.noCache
import com.levibostian.teller.testing.extensions.none
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class CacheStateTestingTest {

    @Mock lateinit var requirements: TellerRepository.GetCacheRequirements

    // none()
    @Test
    fun `none() expect result to equal what state machine would return`() {
        val fromStateMachine = CacheState.none<String>()
        val testing = CacheState.Testing.none<String>()

        assertThat(fromStateMachine).isEqualTo(testing)
    }

    // noCache()

    @Test
    fun `noCache() expect result to equal what state machine would return`() {
        val fromStateMachine = CacheStateStateMachine.noCacheExists<String>(requirements)
        val testing = CacheState.Testing.noCache<String>(requirements)

        assertThat(fromStateMachine).isEqualTo(testing)
    }

    @Test
    fun `noCache(), fetching - Expect result to equal what state machine would return`() {
        val fromStateMachine = CacheStateStateMachine.noCacheExists<String>(requirements).change()
                .firstFetch()
        val testing = CacheState.Testing.noCache<String>(requirements) {
            fetchingFirstTime()
        }

        assertThat(fromStateMachine).isEqualTo(testing)
    }

    @Test
    fun `noCache(), failed fetching - Expect result to equal what state machine would return`() {
        val failure = RuntimeException("fail")
        val fromStateMachine = CacheStateStateMachine.noCacheExists<String>(requirements).change()
                .firstFetch().change()
                .failFirstFetch(failure)
        val testing = CacheState.Testing.noCache<String>(requirements) {
            failedFirstFetch(failure)
        }

        assertThat(fromStateMachine).isEqualTo(testing)
    }

    @Test
    fun `noCache(), successful first fetch - Expect result to equal what state machine would return`() {
        val timeFetched = Date()
        val fromStateMachine = CacheStateStateMachine.noCacheExists<String>(requirements).change()
                .firstFetch().change()
                .successfulFirstFetch(timeFetched)
        val testing = CacheState.Testing.noCache<String>(requirements) {
            successfulFirstFetch(timeFetched)
        }

        assertThat(fromStateMachine).isEqualTo(testing)
    }

    // cache(), empty

    @Test
    fun `cache(), not giving cache - Expect result to equal what state machine would return - Assume cache is empty`() {
        val timeFetched = Date()
        val fromStateMachine = CacheStateStateMachine.cacheExists<String>(requirements, timeFetched)
                .change()
                .cacheEmpty()
        val testing = CacheState.Testing.cache<String>(requirements, timeFetched)

        assertThat(fromStateMachine).isEqualTo(testing)
    }

    @Test
    fun `cache(), cache empty, fetching - Expect result to equal what state machine would return`() {
        val timeFetched = Date()
        val fromStateMachine = CacheStateStateMachine.cacheExists<String>(requirements, timeFetched).change()
                .cacheEmpty().change()
                .fetchingFreshCache()
        val testing = CacheState.Testing.cache<String>(requirements, timeFetched) {
            fetching()
        }

        assertThat(fromStateMachine).isEqualTo(testing)
    }

    @Test
    fun `cache(), cache empty, failed fetch - Expect result to equal what state machine would return`() {
        val timeFetched = Date()
        val fetchFail = RuntimeException("fail")
        val fromStateMachine = CacheStateStateMachine.cacheExists<String>(requirements, timeFetched).change()
                .cacheEmpty().change()
                .fetchingFreshCache().change()
                .failRefreshCache(fetchFail)
        val testing = CacheState.Testing.cache<String>(requirements, timeFetched) {
            failedFetch(fetchFail)
        }

        assertThat(fromStateMachine).isEqualTo(testing)
    }

    @Test
    fun `cache(), cache empty, successful refresh - Expect result to equal what state machine would return`() {
        val oneDayAgo: Date = Calendar.getInstance().apply {
            add(Calendar.DATE, -1)
        }.time
        val newTimeFetched = Date()
        val fromStateMachine = CacheStateStateMachine.cacheExists<String>(requirements, oneDayAgo).change()
                .cacheEmpty().change()
                .fetchingFreshCache().change()
                .successfulRefreshCache(newTimeFetched)
        val testing = CacheState.Testing.cache<String>(requirements, oneDayAgo) {
            successfulFetch(newTimeFetched)
        }

        assertThat(fromStateMachine).isEqualTo(testing)
    }

    // cache(), cache not empty

    @Test
    fun `cache(), cache - Expect result to equal what state machine would return`() {
        val timeFetched = Date()
        val cache = "cache"
        val fromStateMachine = CacheStateStateMachine.cacheExists<String>(requirements, timeFetched).change()
                .cache(cache)
        val testing = CacheState.Testing.cache<String>(requirements, timeFetched) {
            cache(cache)
        }

        assertThat(fromStateMachine).isEqualTo(testing)
    }

    @Test
    fun `cache(), cache, fetching - Expect result to equal what state machine would return`() {
        val timeFetched = Date()
        val cache = "cache"
        val fromStateMachine = CacheStateStateMachine.cacheExists<String>(requirements, timeFetched).change()
                .cache(cache).change()
                .fetchingFreshCache()
        val testing = CacheState.Testing.cache<String>(requirements, timeFetched) {
            cache(cache)
            fetching()
        }

        assertThat(fromStateMachine).isEqualTo(testing)
    }

    @Test
    fun `cache(), cache, failed fetch - Expect result to equal what state machine would return`() {
        val timeFetched = Date()
        val fetchFail = RuntimeException("fail")
        val cache = "cache"
        val fromStateMachine = CacheStateStateMachine.cacheExists<String>(requirements, timeFetched).change()
                .cache(cache).change()
                .fetchingFreshCache().change()
                .failRefreshCache(fetchFail)
        val testing = CacheState.Testing.cache<String>(requirements, timeFetched) {
            cache(cache)
            failedFetch(fetchFail)
        }

        assertThat(fromStateMachine).isEqualTo(testing)
    }

    @Test
    fun `cache(), cache, successful refresh - Expect result to equal what state machine would return`() {
        val oneDayAgo: Date = Calendar.getInstance().apply {
            add(Calendar.DATE, -1)
        }.time
        val newTimeFetched = Date()
        val cache = "cache"
        val fromStateMachine = CacheStateStateMachine.cacheExists<String>(requirements, oneDayAgo).change()
                .cache(cache).change()
                .fetchingFreshCache().change()
                .successfulRefreshCache(newTimeFetched)
        val testing = CacheState.Testing.cache<String>(requirements, oneDayAgo) {
            cache(cache)
            successfulFetch(newTimeFetched)
        }

        assertThat(fromStateMachine).isEqualTo(testing)
    }

}
