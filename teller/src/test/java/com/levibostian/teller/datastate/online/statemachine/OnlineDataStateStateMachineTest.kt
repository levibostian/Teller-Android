package com.levibostian.teller.datastate.online.statemachine

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.datastate.OnlineDataState
import com.levibostian.teller.repository.GetDataRequirementsTag
import com.levibostian.teller.repository.OnlineRepository
import io.reactivex.subjects.PublishSubject
import org.junit.Test
import com.levibostian.teller.util.AssertionUtil.Companion.check
import com.levibostian.teller.util.ConstantsUtil

import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import com.nhaarman.mockito_kotlin.*
import org.junit.Before
import org.mockito.Mockito.`when`
import java.util.*
import kotlin.test.assertFails

@RunWith(MockitoJUnitRunner::class)
class OnlineDataStateStateMachineTest {

    private lateinit var dataState: OnlineDataState<String>
    @Mock private lateinit var dataRequirements: OnlineRepository.GetDataRequirements

    /**
    The tests below follow the pattern below for all of the functions of the state machine:
    1. errorCannotTravelToNode - Testing various states of the state machine going into the function under test that will cause an error.
    2. _setsCorrectProperties - After a successful transition to the state machine node under test, the properties in the returned onlinedatastate are set correctly.
    3. _travelingToNextNode - Going from the state machine node under test to all of the other possible nodes, what paths are valid and not valid?
     */
    @Test
    fun noCacheExists_setsCorrectProperties() {
        dataState = OnlineDataStateStateMachine.noCacheExists(dataRequirements)

        assertThat(dataState.noCacheExists).isTrue()
        assertThat(dataState.fetchingForFirstTime).isFalse()
        assertThat(dataState.cacheData).isNull()
        assertThat(dataState.lastTimeFetched).isNull()
        assertThat(dataState.isFetchingFreshData).isFalse()
        assertThat(dataState.requirements).isEqualTo(dataRequirements)
        assertThat(dataState.stateMachine).isNotNull()
        assertThat(dataState.errorDuringFirstFetch).isNull()
        assertThat(dataState.justCompletedSuccessfulFirstFetch).isFalse()
        assertThat(dataState.errorDuringFetch).isNull()
        assertThat(dataState.justCompletedSuccessfullyFetchingFreshData).isFalse()
    }

    @Test
    fun noCacheExists_travelingToNextNode() {
        dataState = OnlineDataStateStateMachine.noCacheExists(dataRequirements)

        dataState.change().firstFetch()
        assertFails { dataState.change().errorFirstFetch(RuntimeException("")) }
        assertFails { dataState.change().successfulFirstFetch(Date()) }
        assertFails { dataState.change().cacheIsEmpty() }
        assertFails { dataState.change().cachedData("") }
        assertFails { dataState.change().fetchingFreshCache() }
        assertFails { dataState.change().successfulFetchingFreshCache(Date()) }
    }

    @Test
    fun cacheExists_setsCorrectProperties() {
        val lastTimeFetched = Date()
        dataState = OnlineDataStateStateMachine.cacheExists(dataRequirements, lastTimeFetched)

        assertThat(dataState.noCacheExists).isFalse()
        assertThat(dataState.fetchingForFirstTime).isFalse()
        assertThat(dataState.cacheData).isNull()
        assertThat(dataState.lastTimeFetched).isEqualTo(lastTimeFetched)
        assertThat(dataState.isFetchingFreshData).isFalse()
        assertThat(dataState.requirements).isEqualTo(dataRequirements)
        assertThat(dataState.stateMachine).isNotNull()
        assertThat(dataState.errorDuringFirstFetch).isNull()
        assertThat(dataState.justCompletedSuccessfulFirstFetch).isFalse()
        assertThat(dataState.errorDuringFetch).isNull()
        assertThat(dataState.justCompletedSuccessfullyFetchingFreshData).isFalse()
    }

    @Test
    fun cacheExists_travelingToNextNode() {
        val lastTimeFetched = Date()
        dataState = OnlineDataStateStateMachine.cacheExists(dataRequirements, lastTimeFetched)

        assertFails { dataState.change().firstFetch() }
        assertFails { dataState.change().errorFirstFetch(RuntimeException("")) }
        assertFails { dataState.change().successfulFirstFetch(Date()) }
        dataState.change().cacheIsEmpty()
        dataState.change().cachedData("")
        dataState.change().fetchingFreshCache()
        assertFails { dataState.change().successfulFetchingFreshCache(Date()) }
    }

    @Test
    fun firstFetch_errorCannotTravelToNode() {
        dataState = OnlineDataStateStateMachine.cacheExists(dataRequirements, Date())
        assertFails { dataState.change().firstFetch() }
    }

    @Test
    fun firstFetch_setsCorrectProperties() {
        dataState = OnlineDataStateStateMachine.noCacheExists(dataRequirements)
        dataState = dataState.change().firstFetch()

        assertThat(dataState.noCacheExists).isTrue()
        assertThat(dataState.fetchingForFirstTime).isTrue()
        assertThat(dataState.cacheData).isNull()
        assertThat(dataState.lastTimeFetched).isNull()
        assertThat(dataState.isFetchingFreshData).isFalse()
        assertThat(dataState.requirements).isEqualTo(dataRequirements)
        assertThat(dataState.stateMachine).isNotNull()
        assertThat(dataState.errorDuringFirstFetch).isNull()
        assertThat(dataState.justCompletedSuccessfulFirstFetch).isFalse()
        assertThat(dataState.errorDuringFetch).isNull()
        assertThat(dataState.justCompletedSuccessfullyFetchingFreshData).isFalse()
    }

    @Test
    fun firstFetch_travelingToNextNode() {
        dataState = OnlineDataStateStateMachine.noCacheExists(dataRequirements)
        dataState = dataState.change().firstFetch()

        dataState.change().firstFetch()
        dataState.change().errorFirstFetch(RuntimeException(""))
        dataState.change().successfulFirstFetch(Date())
        assertFails { dataState.change().cacheIsEmpty() }
        assertFails { dataState.change().cachedData("") }
        assertFails { dataState.change().fetchingFreshCache() }
        assertFails { dataState.change().successfulFetchingFreshCache(Date()) }
    }

    @Test
    fun errorFirstFetch_errorCannotTravelToNode() {
        dataState = OnlineDataStateStateMachine.cacheExists(dataRequirements, Date())
        assertFails { dataState.change().errorFirstFetch(RuntimeException("")) }

        dataState = OnlineDataStateStateMachine.noCacheExists(dataRequirements)
        assertFails { dataState.change().errorFirstFetch(RuntimeException("")) }
    }

    @Test
    fun errorFirstFetch_setsCorrectProperties() {
        dataState = OnlineDataStateStateMachine.noCacheExists(dataRequirements)
        dataState = dataState.change().firstFetch()
        val fetchFail = RuntimeException("")
        dataState = dataState.change().errorFirstFetch(fetchFail)

        assertThat(dataState.noCacheExists).isTrue()
        assertThat(dataState.fetchingForFirstTime).isFalse()
        assertThat(dataState.cacheData).isNull()
        assertThat(dataState.lastTimeFetched).isNull()
        assertThat(dataState.isFetchingFreshData).isFalse()
        assertThat(dataState.requirements).isEqualTo(dataRequirements)
        assertThat(dataState.stateMachine).isNotNull()
        assertThat(dataState.errorDuringFirstFetch).isEqualTo(fetchFail)
        assertThat(dataState.justCompletedSuccessfulFirstFetch).isFalse()
        assertThat(dataState.errorDuringFetch).isNull()
        assertThat(dataState.justCompletedSuccessfullyFetchingFreshData).isFalse()
    }

    @Test
    fun errorFirstFetch_travelingToNextNode() {
        dataState = OnlineDataStateStateMachine.noCacheExists(dataRequirements)
        dataState = dataState.change().firstFetch()
        val fetchFail = RuntimeException("")
        dataState = dataState.change().errorFirstFetch(fetchFail)

        dataState.change().firstFetch()
        assertFails { dataState.change().errorFirstFetch(RuntimeException("")) }
        assertFails { dataState.change().successfulFirstFetch(Date()) }
        assertFails { dataState.change().cacheIsEmpty() }
        assertFails { dataState.change().cachedData("") }
        assertFails { dataState.change().fetchingFreshCache() }
        assertFails { dataState.change().successfulFetchingFreshCache(Date()) }
    }

    @Test
    fun successfulFirstFetch_errorCannotTravelToNode() {
        dataState = OnlineDataStateStateMachine.cacheExists(dataRequirements, Date())
        assertFails { dataState.change().successfulFirstFetch(Date()) }

        dataState = OnlineDataStateStateMachine.noCacheExists(dataRequirements)
        assertFails { dataState.change().successfulFirstFetch(Date()) }
    }

    @Test
    fun successfulFirstFetch_setsCorrectProperties() {
        dataState = OnlineDataStateStateMachine.noCacheExists(dataRequirements)
        dataState = dataState.change().firstFetch()
        val lastTimeFetched = Date()
        dataState = dataState.change().successfulFirstFetch(lastTimeFetched)

        assertThat(dataState.noCacheExists).isFalse()
        assertThat(dataState.fetchingForFirstTime).isFalse()
        assertThat(dataState.cacheData).isNull()
        assertThat(dataState.lastTimeFetched).isEqualTo(lastTimeFetched)
        assertThat(dataState.isFetchingFreshData).isFalse()
        assertThat(dataState.requirements).isEqualTo(dataRequirements)
        assertThat(dataState.stateMachine).isNotNull()
        assertThat(dataState.errorDuringFirstFetch).isNull()
        assertThat(dataState.justCompletedSuccessfulFirstFetch).isTrue()
        assertThat(dataState.errorDuringFetch).isNull()
        assertThat(dataState.justCompletedSuccessfullyFetchingFreshData).isFalse()
    }

    @Test
    fun successfulFirstFetch_travelingToNextNode() {
        dataState = OnlineDataStateStateMachine.noCacheExists(dataRequirements)
        dataState = dataState.change().firstFetch()
        val lastTimeFetched = Date()
        dataState = dataState.change().successfulFirstFetch(lastTimeFetched)

        assertFails { dataState.change().firstFetch() }
        assertFails { dataState.change().errorFirstFetch(RuntimeException("")) }
        assertFails { dataState.change().successfulFirstFetch(Date()) }
        dataState.change().cacheIsEmpty()
        dataState.change().cachedData("")
        dataState.change().fetchingFreshCache()
        assertFails { dataState.change().successfulFetchingFreshCache(Date()) }
    }

    @Test
    fun cacheIsEmpty_errorCannotTravelToNode() {
        dataState = OnlineDataStateStateMachine.noCacheExists(dataRequirements)
        assertFails { dataState.change().cacheIsEmpty() }
    }

    @Test
    fun cacheIsEmpty_setsCorrectProperties() {
        val lastTimeFetched = Date()
        dataState = OnlineDataStateStateMachine.cacheExists(dataRequirements, lastTimeFetched)
        dataState = dataState.change().fetchingFreshCache()
        dataState = dataState.change().cacheIsEmpty()

        assertThat(dataState.noCacheExists).isFalse()
        assertThat(dataState.fetchingForFirstTime).isFalse()
        assertThat(dataState.cacheData).isNull()
        assertThat(dataState.lastTimeFetched).isEqualTo(lastTimeFetched)
        assertThat(dataState.isFetchingFreshData).isTrue()
        assertThat(dataState.requirements).isEqualTo(dataRequirements)
        assertThat(dataState.stateMachine).isNotNull()
        assertThat(dataState.errorDuringFirstFetch).isNull()
        assertThat(dataState.justCompletedSuccessfulFirstFetch).isFalse()
        assertThat(dataState.errorDuringFetch).isNull()
        assertThat(dataState.justCompletedSuccessfullyFetchingFreshData).isFalse()
    }

    @Test
    fun cacheIsEmpty_travelingToNextNode() {
        val lastTimeFetched = Date()
        dataState = OnlineDataStateStateMachine.cacheExists(dataRequirements, lastTimeFetched)
        dataState = dataState.change().cacheIsEmpty()

        assertFails { dataState.change().firstFetch() }
        assertFails { dataState.change().errorFirstFetch(RuntimeException("")) }
        assertFails { dataState.change().successfulFirstFetch(Date()) }
        dataState.change().cacheIsEmpty()
        dataState.change().cachedData("")
        dataState.change().fetchingFreshCache()
        assertFails { dataState.change().successfulFetchingFreshCache(Date()) }
    }

    @Test
    fun cachedData_errorCannotTravelToNode() {
        dataState = OnlineDataStateStateMachine.noCacheExists(dataRequirements)
        assertFails { dataState.change().cachedData("") }
    }

    @Test
    fun cachedData_setsCorrectProperties() {
        val lastTimeFetched = Date()
        dataState = OnlineDataStateStateMachine.cacheExists(dataRequirements, lastTimeFetched)
        dataState = dataState.change().fetchingFreshCache()
        val cache = "cache"
        dataState = dataState.change().cachedData(cache)

        assertThat(dataState.noCacheExists).isFalse()
        assertThat(dataState.fetchingForFirstTime).isFalse()
        assertThat(dataState.cacheData).isEqualTo(cache)
        assertThat(dataState.lastTimeFetched).isEqualTo(lastTimeFetched)
        assertThat(dataState.isFetchingFreshData).isTrue()
        assertThat(dataState.requirements).isEqualTo(dataRequirements)
        assertThat(dataState.stateMachine).isNotNull()
        assertThat(dataState.errorDuringFirstFetch).isNull()
        assertThat(dataState.justCompletedSuccessfulFirstFetch).isFalse()
        assertThat(dataState.errorDuringFetch).isNull()
        assertThat(dataState.justCompletedSuccessfullyFetchingFreshData).isFalse()
    }

    @Test
    fun cachedData_travelingToNextNode() {
        val lastTimeFetched = Date()
        dataState = OnlineDataStateStateMachine.cacheExists(dataRequirements, lastTimeFetched)
        val cache = "cache"
        dataState = dataState.change().cachedData(cache)

        assertFails { dataState.change().firstFetch() }
        assertFails { dataState.change().errorFirstFetch(RuntimeException("")) }
        assertFails { dataState.change().successfulFirstFetch(Date()) }
        dataState.change().cacheIsEmpty()
        dataState.change().cachedData("")
        dataState.change().fetchingFreshCache()
        assertFails { dataState.change().successfulFetchingFreshCache(Date()) }
    }

    @Test
    fun fetchingFreshCache_errorCannotTravelToNode() {
        dataState = OnlineDataStateStateMachine.noCacheExists(dataRequirements)
        assertFails { dataState.change().fetchingFreshCache() }
    }

    @Test
    fun fetchingFreshCache_setsCorrectProperties() {
        val lastTimeFetched = Date()
        dataState = OnlineDataStateStateMachine.cacheExists(dataRequirements, lastTimeFetched)
        val cache = "cache"
        dataState = dataState.change().cachedData(cache)
        dataState = dataState.change().fetchingFreshCache()

        assertThat(dataState.noCacheExists).isFalse()
        assertThat(dataState.fetchingForFirstTime).isFalse()
        assertThat(dataState.cacheData).isEqualTo(cache)
        assertThat(dataState.lastTimeFetched).isEqualTo(lastTimeFetched)
        assertThat(dataState.isFetchingFreshData).isTrue()
        assertThat(dataState.requirements).isEqualTo(dataRequirements)
        assertThat(dataState.stateMachine).isNotNull()
        assertThat(dataState.errorDuringFirstFetch).isNull()
        assertThat(dataState.justCompletedSuccessfulFirstFetch).isFalse()
        assertThat(dataState.errorDuringFetch).isNull()
        assertThat(dataState.justCompletedSuccessfullyFetchingFreshData).isFalse()
    }

    @Test
    fun fetchingFreshCache_travelingToNextNode() {
        val lastTimeFetched = Date()
        dataState = OnlineDataStateStateMachine.cacheExists(dataRequirements, lastTimeFetched)
        val cache = "cache"
        dataState = dataState.change().cachedData(cache)
        dataState = dataState.change().fetchingFreshCache()

        assertFails { dataState.change().firstFetch() }
        assertFails { dataState.change().errorFirstFetch(RuntimeException("")) }
        assertFails { dataState.change().successfulFirstFetch(Date()) }
        dataState.change().cacheIsEmpty()
        dataState.change().cachedData("")
        dataState.change().fetchingFreshCache()
        dataState.change().successfulFetchingFreshCache(Date())
    }

    @Test
    fun failFetchingFreshCache_errorCannotTravelToNode() {
        dataState = OnlineDataStateStateMachine.noCacheExists(dataRequirements)
        assertFails { dataState.change().failFetchingFreshCache(RuntimeException("")) }

        dataState = OnlineDataStateStateMachine.cacheExists(dataRequirements, Date())
        assertFails { dataState.change().failFetchingFreshCache(RuntimeException("")) }
    }

    @Test
    fun failFetchingFreshCache_setsCorrectProperties() {
        val lastTimeFetched = Date()
        dataState = OnlineDataStateStateMachine.cacheExists(dataRequirements, lastTimeFetched)
        val cache = "cache"
        dataState = dataState.change().fetchingFreshCache()
        dataState = dataState.change().cachedData(cache)
        val fetchFailure = RuntimeException("")
        dataState = dataState.change().failFetchingFreshCache(fetchFailure)

        assertThat(dataState.noCacheExists).isFalse()
        assertThat(dataState.fetchingForFirstTime).isFalse()
        assertThat(dataState.cacheData).isEqualTo(cache)
        assertThat(dataState.lastTimeFetched).isEqualTo(lastTimeFetched)
        assertThat(dataState.isFetchingFreshData).isFalse()
        assertThat(dataState.requirements).isEqualTo(dataRequirements)
        assertThat(dataState.stateMachine).isNotNull()
        assertThat(dataState.errorDuringFirstFetch).isNull()
        assertThat(dataState.justCompletedSuccessfulFirstFetch).isFalse()
        assertThat(dataState.errorDuringFetch).isEqualTo(fetchFailure)
        assertThat(dataState.justCompletedSuccessfullyFetchingFreshData).isFalse()
    }

    @Test
    fun failFetchingFreshCache_travelingToNextNode() {
        val lastTimeFetched = Date()
        dataState = OnlineDataStateStateMachine.cacheExists(dataRequirements, lastTimeFetched)
        val cache = "cache"
        dataState = dataState.change().fetchingFreshCache()
        dataState = dataState.change().cachedData(cache)
        val fetchFailure = RuntimeException("")
        dataState = dataState.change().failFetchingFreshCache(fetchFailure)

        assertFails { dataState.change().firstFetch() }
        assertFails { dataState.change().errorFirstFetch(RuntimeException("")) }
        assertFails { dataState.change().successfulFirstFetch(Date()) }
        dataState.change().cacheIsEmpty()
        dataState.change().cachedData("")
        dataState.change().fetchingFreshCache()
        assertFails { dataState.change().successfulFetchingFreshCache(Date()) }
    }

    @Test
    fun successfulFetchingFreshCache_errorCannotTravelToNode() {
        dataState = OnlineDataStateStateMachine.noCacheExists(dataRequirements)
        assertFails { dataState.change().successfulFetchingFreshCache(Date()) }

        dataState = OnlineDataStateStateMachine.cacheExists(dataRequirements, Date())
        assertFails { dataState.change().successfulFetchingFreshCache(Date()) }
    }

    @Test
    fun successfulFetchingFreshCache_setsCorrectProperties() {
        val lastTimeFetched: Date = Calendar.getInstance().apply {
            add(Calendar.DATE, -1)
        }.time

        dataState = OnlineDataStateStateMachine.cacheExists(dataRequirements, lastTimeFetched)
        val cache = "cache"
        dataState = dataState.change().fetchingFreshCache()
        dataState = dataState.change().cachedData(cache)
        val newTimeFetched = Date()
        dataState = dataState.change().successfulFetchingFreshCache(newTimeFetched)

        assertThat(dataState.noCacheExists).isFalse()
        assertThat(dataState.fetchingForFirstTime).isFalse()
        assertThat(dataState.cacheData).isEqualTo(cache)
        assertThat(dataState.lastTimeFetched).isEqualTo(newTimeFetched)
        assertThat(dataState.isFetchingFreshData).isFalse()
        assertThat(dataState.requirements).isEqualTo(dataRequirements)
        assertThat(dataState.stateMachine).isNotNull()
        assertThat(dataState.errorDuringFirstFetch).isNull()
        assertThat(dataState.justCompletedSuccessfulFirstFetch).isFalse()
        assertThat(dataState.errorDuringFetch).isNull()
        assertThat(dataState.justCompletedSuccessfullyFetchingFreshData).isTrue()
    }

    @Test
    fun successfulFetchingFreshCache_travelingToNextNode() {
        val lastTimeFetched = Date()
        dataState = OnlineDataStateStateMachine.cacheExists(dataRequirements, lastTimeFetched)
        val cache = "cache"
        dataState = dataState.change().fetchingFreshCache()
        dataState = dataState.change().cachedData(cache)
        val newTimeFetched = Date()
        dataState = dataState.change().successfulFetchingFreshCache(newTimeFetched)

        assertFails { dataState.change().firstFetch() }
        assertFails { dataState.change().errorFirstFetch(RuntimeException("")) }
        assertFails { dataState.change().successfulFirstFetch(Date()) }
        dataState.change().cacheIsEmpty()
        dataState.change().cachedData("")
        dataState.change().fetchingFreshCache()
        assertFails { dataState.change().successfulFetchingFreshCache(Date()) }
    }

}