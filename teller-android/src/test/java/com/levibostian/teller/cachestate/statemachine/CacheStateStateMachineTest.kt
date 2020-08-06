package com.levibostian.teller.cachestate.statemachine

import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.cachestate.CacheState
import com.levibostian.teller.repository.TellerRepository
import org.junit.Test

import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.util.*
import kotlin.test.assertFails

@RunWith(MockitoJUnitRunner::class)
class CacheStateStateMachineTest {

    private lateinit var cacheState: CacheState<String>
    @Mock private lateinit var cacheRequirements: TellerRepository.GetCacheRequirements

    /**
    The tests below follow the pattern below for all of the functions of the state machine:
    1. errorCannotTravelToNode - Testing various states of the state machine going into the function under test that will cause an error.
    2. _setsCorrectProperties - After a successful transition to the state machine node under test, the properties in the returned onlinedatastate are set correctly.
    3. _travelingToNextNode - Going from the state machine node under test to all of the other possible nodes, what paths are valid and not valid?
     */
    @Test
    fun noCacheExists_setsCorrectProperties() {
        cacheState = CacheStateStateMachine.noCacheExists(cacheRequirements)

        assertThat(cacheState.noCacheExists).isTrue()
        assertThat(cacheState.isFetchingFirstCache).isFalse()
        assertThat(cacheState.cache).isNull()
        assertThat(cacheState.lastSuccessfulFetch).isNull()
        assertThat(cacheState.isFetchingToUpdateCache).isFalse()
        assertThat(cacheState.requirements).isEqualTo(cacheRequirements)
        assertThat(cacheState.stateMachine).isNotNull()
        assertThat(cacheState.fetchFirstCacheError).isNull()
        assertThat(cacheState.justSuccessfullyFetchedFirstCache).isFalse()
        assertThat(cacheState.fetchToUpdateCacheError).isNull()
        assertThat(cacheState.justSuccessfullyFetchedToUpdateCache).isFalse()
    }

    @Test
    fun noCacheExists_travelingToNextNode() {
        cacheState = CacheStateStateMachine.noCacheExists(cacheRequirements)

        cacheState.change().firstFetch()
        assertFails { cacheState.change().failFirstFetch(RuntimeException("")) }
        assertFails { cacheState.change().successfulFirstFetch(Date()) }
        assertFails { cacheState.change().cacheEmpty() }
        assertFails { cacheState.change().cache("") }
        assertFails { cacheState.change().fetchingFreshCache() }
        assertFails { cacheState.change().successfulRefreshCache(Date()) }
    }

    @Test
    fun cacheExists_setsCorrectProperties() {
        val lastTimeFetched = Date()
        cacheState = CacheStateStateMachine.cacheExists(cacheRequirements, lastTimeFetched)

        assertThat(cacheState.noCacheExists).isFalse()
        assertThat(cacheState.isFetchingFirstCache).isFalse()
        assertThat(cacheState.cache).isNull()
        assertThat(cacheState.lastSuccessfulFetch).isEqualTo(lastTimeFetched)
        assertThat(cacheState.isFetchingToUpdateCache).isFalse()
        assertThat(cacheState.requirements).isEqualTo(cacheRequirements)
        assertThat(cacheState.stateMachine).isNotNull()
        assertThat(cacheState.fetchFirstCacheError).isNull()
        assertThat(cacheState.justSuccessfullyFetchedFirstCache).isFalse()
        assertThat(cacheState.fetchToUpdateCacheError).isNull()
        assertThat(cacheState.justSuccessfullyFetchedToUpdateCache).isFalse()
    }

    @Test
    fun cacheExists_travelingToNextNode() {
        val lastTimeFetched = Date()
        cacheState = CacheStateStateMachine.cacheExists(cacheRequirements, lastTimeFetched)

        assertFails { cacheState.change().firstFetch() }
        assertFails { cacheState.change().failFirstFetch(RuntimeException("")) }
        assertFails { cacheState.change().successfulFirstFetch(Date()) }
        cacheState.change().cacheEmpty()
        cacheState.change().cache("")
        cacheState.change().fetchingFreshCache()
        assertFails { cacheState.change().successfulRefreshCache(Date()) }
    }

    @Test
    fun firstFetch_errorCannotTravelToNode() {
        cacheState = CacheStateStateMachine.cacheExists(cacheRequirements, Date())
        assertFails { cacheState.change().firstFetch() }
    }

    @Test
    fun firstFetch_setsCorrectProperties() {
        cacheState = CacheStateStateMachine.noCacheExists(cacheRequirements)
        cacheState = cacheState.change().firstFetch()

        assertThat(cacheState.noCacheExists).isTrue()
        assertThat(cacheState.isFetchingFirstCache).isTrue()
        assertThat(cacheState.cache).isNull()
        assertThat(cacheState.lastSuccessfulFetch).isNull()
        assertThat(cacheState.isFetchingToUpdateCache).isFalse()
        assertThat(cacheState.requirements).isEqualTo(cacheRequirements)
        assertThat(cacheState.stateMachine).isNotNull()
        assertThat(cacheState.fetchFirstCacheError).isNull()
        assertThat(cacheState.justSuccessfullyFetchedFirstCache).isFalse()
        assertThat(cacheState.fetchToUpdateCacheError).isNull()
        assertThat(cacheState.justSuccessfullyFetchedToUpdateCache).isFalse()
    }

    @Test
    fun firstFetch_travelingToNextNode() {
        cacheState = CacheStateStateMachine.noCacheExists(cacheRequirements)
        cacheState = cacheState.change().firstFetch()

        cacheState.change().firstFetch()
        cacheState.change().failFirstFetch(RuntimeException(""))
        cacheState.change().successfulFirstFetch(Date())
        assertFails { cacheState.change().cacheEmpty() }
        assertFails { cacheState.change().cache("") }
        assertFails { cacheState.change().fetchingFreshCache() }
        assertFails { cacheState.change().successfulRefreshCache(Date()) }
    }

    @Test
    fun errorFirstFetch_errorCannotTravelToNode() {
        cacheState = CacheStateStateMachine.cacheExists(cacheRequirements, Date())
        assertFails { cacheState.change().failFirstFetch(RuntimeException("")) }

        cacheState = CacheStateStateMachine.noCacheExists(cacheRequirements)
        assertFails { cacheState.change().failFirstFetch(RuntimeException("")) }
    }

    @Test
    fun errorFirstFetch_setsCorrectProperties() {
        cacheState = CacheStateStateMachine.noCacheExists(cacheRequirements)
        cacheState = cacheState.change().firstFetch()
        val fetchFail = RuntimeException("")
        cacheState = cacheState.change().failFirstFetch(fetchFail)

        assertThat(cacheState.noCacheExists).isTrue()
        assertThat(cacheState.isFetchingFirstCache).isFalse()
        assertThat(cacheState.cache).isNull()
        assertThat(cacheState.lastSuccessfulFetch).isNull()
        assertThat(cacheState.isFetchingToUpdateCache).isFalse()
        assertThat(cacheState.requirements).isEqualTo(cacheRequirements)
        assertThat(cacheState.stateMachine).isNotNull()
        assertThat(cacheState.fetchFirstCacheError).isEqualTo(fetchFail)
        assertThat(cacheState.justSuccessfullyFetchedFirstCache).isFalse()
        assertThat(cacheState.fetchToUpdateCacheError).isNull()
        assertThat(cacheState.justSuccessfullyFetchedToUpdateCache).isFalse()
    }

    @Test
    fun errorFirstFetch_travelingToNextNode() {
        cacheState = CacheStateStateMachine.noCacheExists(cacheRequirements)
        cacheState = cacheState.change().firstFetch()
        val fetchFail = RuntimeException("")
        cacheState = cacheState.change().failFirstFetch(fetchFail)

        cacheState.change().firstFetch()
        assertFails { cacheState.change().failFirstFetch(RuntimeException("")) }
        assertFails { cacheState.change().successfulFirstFetch(Date()) }
        assertFails { cacheState.change().cacheEmpty() }
        assertFails { cacheState.change().cache("") }
        assertFails { cacheState.change().fetchingFreshCache() }
        assertFails { cacheState.change().successfulRefreshCache(Date()) }
    }

    @Test
    fun successfulFirstFetch_errorCannotTravelToNode() {
        cacheState = CacheStateStateMachine.cacheExists(cacheRequirements, Date())
        assertFails { cacheState.change().successfulFirstFetch(Date()) }

        cacheState = CacheStateStateMachine.noCacheExists(cacheRequirements)
        assertFails { cacheState.change().successfulFirstFetch(Date()) }
    }

    @Test
    fun successfulFirstFetch_setsCorrectProperties() {
        cacheState = CacheStateStateMachine.noCacheExists(cacheRequirements)
        cacheState = cacheState.change().firstFetch()
        val lastTimeFetched = Date()
        cacheState = cacheState.change().successfulFirstFetch(lastTimeFetched)

        assertThat(cacheState.noCacheExists).isFalse()
        assertThat(cacheState.isFetchingFirstCache).isFalse()
        assertThat(cacheState.cache).isNull()
        assertThat(cacheState.lastSuccessfulFetch).isEqualTo(lastTimeFetched)
        assertThat(cacheState.isFetchingToUpdateCache).isFalse()
        assertThat(cacheState.requirements).isEqualTo(cacheRequirements)
        assertThat(cacheState.stateMachine).isNotNull()
        assertThat(cacheState.fetchFirstCacheError).isNull()
        assertThat(cacheState.justSuccessfullyFetchedFirstCache).isTrue()
        assertThat(cacheState.fetchToUpdateCacheError).isNull()
        assertThat(cacheState.justSuccessfullyFetchedToUpdateCache).isFalse()
    }

    @Test
    fun successfulFirstFetch_travelingToNextNode() {
        cacheState = CacheStateStateMachine.noCacheExists(cacheRequirements)
        cacheState = cacheState.change().firstFetch()
        val lastTimeFetched = Date()
        cacheState = cacheState.change().successfulFirstFetch(lastTimeFetched)

        assertFails { cacheState.change().firstFetch() }
        assertFails { cacheState.change().failFirstFetch(RuntimeException("")) }
        assertFails { cacheState.change().successfulFirstFetch(Date()) }
        cacheState.change().cacheEmpty()
        cacheState.change().cache("")
        cacheState.change().fetchingFreshCache()
        assertFails { cacheState.change().successfulRefreshCache(Date()) }
    }

    @Test
    fun cacheIsEmpty_errorCannotTravelToNode() {
        cacheState = CacheStateStateMachine.noCacheExists(cacheRequirements)
        assertFails { cacheState.change().cacheEmpty() }
    }

    @Test
    fun cacheIsEmpty_setsCorrectProperties() {
        val lastTimeFetched = Date()
        cacheState = CacheStateStateMachine.cacheExists(cacheRequirements, lastTimeFetched)
        cacheState = cacheState.change().fetchingFreshCache()
        cacheState = cacheState.change().cacheEmpty()

        assertThat(cacheState.noCacheExists).isFalse()
        assertThat(cacheState.isFetchingFirstCache).isFalse()
        assertThat(cacheState.cache).isNull()
        assertThat(cacheState.lastSuccessfulFetch).isEqualTo(lastTimeFetched)
        assertThat(cacheState.isFetchingToUpdateCache).isTrue()
        assertThat(cacheState.requirements).isEqualTo(cacheRequirements)
        assertThat(cacheState.stateMachine).isNotNull()
        assertThat(cacheState.fetchFirstCacheError).isNull()
        assertThat(cacheState.justSuccessfullyFetchedFirstCache).isFalse()
        assertThat(cacheState.fetchToUpdateCacheError).isNull()
        assertThat(cacheState.justSuccessfullyFetchedToUpdateCache).isFalse()
    }

    @Test
    fun cacheIsEmpty_travelingToNextNode() {
        val lastTimeFetched = Date()
        cacheState = CacheStateStateMachine.cacheExists(cacheRequirements, lastTimeFetched)
        cacheState = cacheState.change().cacheEmpty()

        assertFails { cacheState.change().firstFetch() }
        assertFails { cacheState.change().failFirstFetch(RuntimeException("")) }
        assertFails { cacheState.change().successfulFirstFetch(Date()) }
        cacheState.change().cacheEmpty()
        cacheState.change().cache("")
        cacheState.change().fetchingFreshCache()
        assertFails { cacheState.change().successfulRefreshCache(Date()) }
    }

    @Test
    fun cachedData_errorCannotTravelToNode() {
        cacheState = CacheStateStateMachine.noCacheExists(cacheRequirements)
        assertFails { cacheState.change().cache("") }
    }

    @Test
    fun cachedData_setsCorrectProperties() {
        val lastTimeFetched = Date()
        cacheState = CacheStateStateMachine.cacheExists(cacheRequirements, lastTimeFetched)
        cacheState = cacheState.change().fetchingFreshCache()
        val cache = "cache"
        cacheState = cacheState.change().cache(cache)

        assertThat(cacheState.noCacheExists).isFalse()
        assertThat(cacheState.isFetchingFirstCache).isFalse()
        assertThat(cacheState.cache).isEqualTo(cache)
        assertThat(cacheState.lastSuccessfulFetch).isEqualTo(lastTimeFetched)
        assertThat(cacheState.isFetchingToUpdateCache).isTrue()
        assertThat(cacheState.requirements).isEqualTo(cacheRequirements)
        assertThat(cacheState.stateMachine).isNotNull()
        assertThat(cacheState.fetchFirstCacheError).isNull()
        assertThat(cacheState.justSuccessfullyFetchedFirstCache).isFalse()
        assertThat(cacheState.fetchToUpdateCacheError).isNull()
        assertThat(cacheState.justSuccessfullyFetchedToUpdateCache).isFalse()
    }

    @Test
    fun cachedData_travelingToNextNode() {
        val lastTimeFetched = Date()
        cacheState = CacheStateStateMachine.cacheExists(cacheRequirements, lastTimeFetched)
        val cache = "cache"
        cacheState = cacheState.change().cache(cache)

        assertFails { cacheState.change().firstFetch() }
        assertFails { cacheState.change().failFirstFetch(RuntimeException("")) }
        assertFails { cacheState.change().successfulFirstFetch(Date()) }
        cacheState.change().cacheEmpty()
        cacheState.change().cache("")
        cacheState.change().fetchingFreshCache()
        assertFails { cacheState.change().successfulRefreshCache(Date()) }
    }

    @Test
    fun fetchingFreshCache_errorCannotTravelToNode() {
        cacheState = CacheStateStateMachine.noCacheExists(cacheRequirements)
        assertFails { cacheState.change().fetchingFreshCache() }
    }

    @Test
    fun fetchingFreshCache_setsCorrectProperties() {
        val lastTimeFetched = Date()
        cacheState = CacheStateStateMachine.cacheExists(cacheRequirements, lastTimeFetched)
        val cache = "cache"
        cacheState = cacheState.change().cache(cache)
        cacheState = cacheState.change().fetchingFreshCache()

        assertThat(cacheState.noCacheExists).isFalse()
        assertThat(cacheState.isFetchingFirstCache).isFalse()
        assertThat(cacheState.cache).isEqualTo(cache)
        assertThat(cacheState.lastSuccessfulFetch).isEqualTo(lastTimeFetched)
        assertThat(cacheState.isFetchingToUpdateCache).isTrue()
        assertThat(cacheState.requirements).isEqualTo(cacheRequirements)
        assertThat(cacheState.stateMachine).isNotNull()
        assertThat(cacheState.fetchFirstCacheError).isNull()
        assertThat(cacheState.justSuccessfullyFetchedFirstCache).isFalse()
        assertThat(cacheState.fetchToUpdateCacheError).isNull()
        assertThat(cacheState.justSuccessfullyFetchedToUpdateCache).isFalse()
    }

    @Test
    fun fetchingFreshCache_travelingToNextNode() {
        val lastTimeFetched = Date()
        cacheState = CacheStateStateMachine.cacheExists(cacheRequirements, lastTimeFetched)
        val cache = "cache"
        cacheState = cacheState.change().cache(cache)
        cacheState = cacheState.change().fetchingFreshCache()

        assertFails { cacheState.change().firstFetch() }
        assertFails { cacheState.change().failFirstFetch(RuntimeException("")) }
        assertFails { cacheState.change().successfulFirstFetch(Date()) }
        cacheState.change().cacheEmpty()
        cacheState.change().cache("")
        cacheState.change().fetchingFreshCache()
        cacheState.change().successfulRefreshCache(Date())
    }

    @Test
    fun failFetchingFreshCache_errorCannotTravelToNode() {
        cacheState = CacheStateStateMachine.noCacheExists(cacheRequirements)
        assertFails { cacheState.change().failRefreshCache(RuntimeException("")) }

        cacheState = CacheStateStateMachine.cacheExists(cacheRequirements, Date())
        assertFails { cacheState.change().failRefreshCache(RuntimeException("")) }
    }

    @Test
    fun failFetchingFreshCache_setsCorrectProperties() {
        val lastTimeFetched = Date()
        cacheState = CacheStateStateMachine.cacheExists(cacheRequirements, lastTimeFetched)
        val cache = "cache"
        cacheState = cacheState.change().fetchingFreshCache()
        cacheState = cacheState.change().cache(cache)
        val fetchFailure = RuntimeException("")
        cacheState = cacheState.change().failRefreshCache(fetchFailure)

        assertThat(cacheState.noCacheExists).isFalse()
        assertThat(cacheState.isFetchingFirstCache).isFalse()
        assertThat(cacheState.cache).isEqualTo(cache)
        assertThat(cacheState.lastSuccessfulFetch).isEqualTo(lastTimeFetched)
        assertThat(cacheState.isFetchingToUpdateCache).isFalse()
        assertThat(cacheState.requirements).isEqualTo(cacheRequirements)
        assertThat(cacheState.stateMachine).isNotNull()
        assertThat(cacheState.fetchFirstCacheError).isNull()
        assertThat(cacheState.justSuccessfullyFetchedFirstCache).isFalse()
        assertThat(cacheState.fetchToUpdateCacheError).isEqualTo(fetchFailure)
        assertThat(cacheState.justSuccessfullyFetchedToUpdateCache).isFalse()
    }

    @Test
    fun failFetchingFreshCache_travelingToNextNode() {
        val lastTimeFetched = Date()
        cacheState = CacheStateStateMachine.cacheExists(cacheRequirements, lastTimeFetched)
        val cache = "cache"
        cacheState = cacheState.change().fetchingFreshCache()
        cacheState = cacheState.change().cache(cache)
        val fetchFailure = RuntimeException("")
        cacheState = cacheState.change().failRefreshCache(fetchFailure)

        assertFails { cacheState.change().firstFetch() }
        assertFails { cacheState.change().failFirstFetch(RuntimeException("")) }
        assertFails { cacheState.change().successfulFirstFetch(Date()) }
        cacheState.change().cacheEmpty()
        cacheState.change().cache("")
        cacheState.change().fetchingFreshCache()
        assertFails { cacheState.change().successfulRefreshCache(Date()) }
    }

    @Test
    fun successfulFetchingFreshCache_errorCannotTravelToNode() {
        cacheState = CacheStateStateMachine.noCacheExists(cacheRequirements)
        assertFails { cacheState.change().successfulRefreshCache(Date()) }

        cacheState = CacheStateStateMachine.cacheExists(cacheRequirements, Date())
        assertFails { cacheState.change().successfulRefreshCache(Date()) }
    }

    @Test
    fun successfulFetchingFreshCache_setsCorrectProperties() {
        val lastTimeFetched: Date = Calendar.getInstance().apply {
            add(Calendar.DATE, -1)
        }.time

        cacheState = CacheStateStateMachine.cacheExists(cacheRequirements, lastTimeFetched)
        val cache = "cache"
        cacheState = cacheState.change().fetchingFreshCache()
        cacheState = cacheState.change().cache(cache)
        val newTimeFetched = Date()
        cacheState = cacheState.change().successfulRefreshCache(newTimeFetched)

        assertThat(cacheState.noCacheExists).isFalse()
        assertThat(cacheState.isFetchingFirstCache).isFalse()
        assertThat(cacheState.cache).isEqualTo(cache)
        assertThat(cacheState.lastSuccessfulFetch).isEqualTo(newTimeFetched)
        assertThat(cacheState.isFetchingToUpdateCache).isFalse()
        assertThat(cacheState.requirements).isEqualTo(cacheRequirements)
        assertThat(cacheState.stateMachine).isNotNull()
        assertThat(cacheState.fetchFirstCacheError).isNull()
        assertThat(cacheState.justSuccessfullyFetchedFirstCache).isFalse()
        assertThat(cacheState.fetchToUpdateCacheError).isNull()
        assertThat(cacheState.justSuccessfullyFetchedToUpdateCache).isTrue()
    }

    @Test
    fun successfulFetchingFreshCache_travelingToNextNode() {
        val lastTimeFetched = Date()
        cacheState = CacheStateStateMachine.cacheExists(cacheRequirements, lastTimeFetched)
        val cache = "cache"
        cacheState = cacheState.change().fetchingFreshCache()
        cacheState = cacheState.change().cache(cache)
        val newTimeFetched = Date()
        cacheState = cacheState.change().successfulRefreshCache(newTimeFetched)

        assertFails { cacheState.change().firstFetch() }
        assertFails { cacheState.change().failFirstFetch(RuntimeException("")) }
        assertFails { cacheState.change().successfulFirstFetch(Date()) }
        cacheState.change().cacheEmpty()
        cacheState.change().cache("")
        cacheState.change().fetchingFreshCache()
        assertFails { cacheState.change().successfulRefreshCache(Date()) }
    }

}