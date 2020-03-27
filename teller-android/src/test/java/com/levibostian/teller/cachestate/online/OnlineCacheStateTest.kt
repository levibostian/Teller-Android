package com.levibostian.teller.cachestate.online

import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.cachestate.*
import com.levibostian.teller.cachestate.online.statemachine.OnlineCacheStateStateMachine
import com.levibostian.teller.repository.OnlineRepository
import com.nhaarman.mockito_kotlin.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class OnlineCacheStateTest {

    private lateinit var cacheState: OnlineCacheState<String>
    @Mock private lateinit var cacheRequirements: OnlineRepository.GetCacheRequirements

    @Test
    fun none_setsCorrectProperties() {
        cacheState = OnlineCacheState.none()

        assertThat(cacheState.noCacheExists).isFalse()
        assertThat(cacheState.isFetchingFirstCache).isFalse()
        assertThat(cacheState.cache).isNull()
        assertThat(cacheState.lastSuccessfulFetch).isNull()
        assertThat(cacheState.isFetchingToUpdateCache).isFalse()
        assertThat(cacheState.requirements).isNull()
        assertThat(cacheState.stateMachine).isNull()
        assertThat(cacheState.fetchFirstCacheError).isNull()
        assertThat(cacheState.justSuccessfullyFetchedFirstCache).isFalse()
        assertThat(cacheState.fetchToUpdateCacheError).isNull()
        assertThat(cacheState.justSuccessfullyFetchedToUpdateCache).isFalse()
    }

    @Test
    fun cacheExists_expectCorrectValue() {
        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, Date()).change().cacheEmpty()
        assertThat(cacheState.cacheExists).isTrue()

        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, Date()).change().cache("Cache")
        assertThat(cacheState.cacheExists).isTrue()

        cacheState = OnlineCacheStateStateMachine.noCacheExists<String>(cacheRequirements).change().firstFetch()
        assertThat(cacheState.cacheExists).isFalse()
    }

    @Test
    fun cacheExistsAndEmpty_expectCorrectValue() {
        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, Date()).change().cacheEmpty()
        assertThat(cacheState.cacheExistsAndEmpty).isTrue()

        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, Date()).change().cacheEmpty().change().fetchingFreshCache()
        assertThat(cacheState.cacheExistsAndEmpty).isTrue()

        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, Date()).change().cache("Cache")
        assertThat(cacheState.cacheExistsAndEmpty).isFalse()

        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, Date()).change().cache("Cache").change().cacheEmpty()
        assertThat(cacheState.cacheExistsAndEmpty).isTrue()

        cacheState = OnlineCacheStateStateMachine.noCacheExists<String>(cacheRequirements).change().firstFetch()
        assertThat(cacheState.cacheExistsAndEmpty).isFalse()
    }

    @Test
    fun isFetching_expectCorrectValues() {
        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, Date()).change().cacheEmpty()
        assertThat(cacheState.isFetching).isFalse()

        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, Date()).change().cacheEmpty().change().fetchingFreshCache()
        assertThat(cacheState.isFetching).isTrue()

        cacheState = OnlineCacheStateStateMachine.noCacheExists(cacheRequirements)
        assertThat(cacheState.isFetching).isFalse()

        cacheState = OnlineCacheStateStateMachine.noCacheExists<String>(cacheRequirements).change().firstFetch()
        assertThat(cacheState.isFetching).isTrue()

        cacheState = OnlineCacheStateStateMachine.noCacheExists<String>(cacheRequirements).change().firstFetch().change().successfulFirstFetch(Date())
        assertThat(cacheState.isFetching).isFalse()
    }

    @Test
    fun fetchError_expectCorrectValue() {
        cacheState = OnlineCacheStateStateMachine.noCacheExists(cacheRequirements)
        assertThat(cacheState.fetchError).isNull()

        cacheState = OnlineCacheStateStateMachine.noCacheExists<String>(cacheRequirements).change().firstFetch().change().successfulFirstFetch(Date())
        assertThat(cacheState.fetchError).isNull()

        val fetchFail = IllegalStateException("fail")
        cacheState = OnlineCacheStateStateMachine.noCacheExists<String>(cacheRequirements).change().firstFetch().change().failFirstFetch(fetchFail)
        assertThat(cacheState.fetchError).isEqualTo(fetchFail)

        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, Date()).change().fetchingFreshCache().change().successfulRefreshCache(Date())
        assertThat(cacheState.fetchError).isNull()

        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, Date()).change().fetchingFreshCache().change().failRefreshCache(fetchFail)
        assertThat(cacheState.fetchError).isEqualTo(fetchFail)

        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, Date()).change().fetchingFreshCache().change().failRefreshCache(fetchFail).change().fetchingFreshCache().change().successfulRefreshCache(Date())
        assertThat(cacheState.fetchError).isNull()
    }

    @Test
    fun justSuccessfullyFetchedCache_expectCorrectValue() {
        cacheState = OnlineCacheStateStateMachine.noCacheExists<String>(cacheRequirements).change().firstFetch()
        assertThat(cacheState.justSuccessfullyFetchedCache).isFalse()

        cacheState = OnlineCacheStateStateMachine.noCacheExists<String>(cacheRequirements).change().firstFetch().change().successfulFirstFetch(Date())
        assertThat(cacheState.justSuccessfullyFetchedCache).isTrue()

        cacheState = OnlineCacheStateStateMachine.noCacheExists<String>(cacheRequirements).change().firstFetch().change().successfulFirstFetch(Date()).change().cacheEmpty()
        assertThat(cacheState.justSuccessfullyFetchedCache).isFalse()

        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, Date()).change().fetchingFreshCache()
        assertThat(cacheState.justSuccessfullyFetchedCache).isFalse()

        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, Date()).change().fetchingFreshCache().change().successfulRefreshCache(Date())
        assertThat(cacheState.justSuccessfullyFetchedCache).isTrue()

        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, Date()).change().fetchingFreshCache().change().successfulRefreshCache(Date()).change().cacheEmpty()
        assertThat(cacheState.justSuccessfullyFetchedCache).isFalse()
    }

    @Test
    fun convert_givenCache_expectConvertToNewType() {
        val givenCache = "1"
        val expectedCache = 1

        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, Date()).change().fetchingFreshCache().change().cache(givenCache)

        val newCacheState = cacheState.convert { oldData ->
            oldData!!.toInt()
        }

        assertThat(newCacheState.cache).isEqualTo(expectedCache)

        assertThat(cacheState.noCacheExists).isEqualTo(newCacheState.noCacheExists)
        assertThat(cacheState.isFetchingFirstCache).isEqualTo(newCacheState.isFetchingFirstCache)
        // assertThat(cacheState.cache).isNull()
        assertThat(cacheState.lastSuccessfulFetch).isEqualTo(newCacheState.lastSuccessfulFetch)
        assertThat(cacheState.isFetchingToUpdateCache).isEqualTo(newCacheState.isFetchingToUpdateCache)
        assertThat(cacheState.requirements).isEqualTo(newCacheState.requirements)
        // assertThat(cacheState.stateMachine).isNull()
        assertThat(cacheState.fetchFirstCacheError).isEqualTo(newCacheState.fetchFirstCacheError)
        assertThat(cacheState.justSuccessfullyFetchedFirstCache).isEqualTo(newCacheState.justSuccessfullyFetchedFirstCache)
        assertThat(cacheState.fetchToUpdateCacheError).isEqualTo(newCacheState.fetchToUpdateCacheError)
        assertThat(cacheState.justSuccessfullyFetchedToUpdateCache).isEqualTo(newCacheState.justSuccessfullyFetchedToUpdateCache)
    }

    @Test
    fun whenNoCache_noCache_expectCallbacks() {
        cacheState = OnlineCacheStateStateMachine.noCacheExists(cacheRequirements)

        val callback = mock<WhenNoCacheCallback>()

        cacheState.whenNoCache(callback)

        verify(callback).invoke(false, null)
    }

    @Test
    fun whenNoCache_fetchingForFirstTime_expectCallbacks() {
        cacheState = OnlineCacheStateStateMachine.noCacheExists<String>(cacheRequirements).change().firstFetch()

        val callback = mock<WhenNoCacheCallback>()
        cacheState.whenNoCache(callback)

        verify(callback).invoke(true, null)
    }

    @Test
    fun whenNoCache_finishedFirstFetch_expectNoCallbacks() {
        val timeFetched = Date()
        cacheState = OnlineCacheStateStateMachine.noCacheExists<String>(cacheRequirements).change().firstFetch().change().successfulFirstFetch(timeFetched)

        val callback = mock<WhenNoCacheCallback>()
        cacheState.whenNoCache(callback)

        verify(callback, never()).invoke(any(), anyOrNull())
    }

    @Test
    fun whenNoCache_errorFirstFetch_expectCallbacks() {
        val error = RuntimeException("")
        cacheState = OnlineCacheStateStateMachine.noCacheExists<String>(cacheRequirements).change().firstFetch().change().failFirstFetch(error)

        val callback = mock<WhenNoCacheCallback>()
        cacheState.whenNoCache(callback)

        verify(callback).invoke(false, error)
    }

    @Test
    fun whenNoCache_cacheExists_expectCallbacks() {
        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, Date()).change().cacheEmpty()

        val callback = mock<WhenNoCacheCallback>()
        cacheState.whenNoCache(callback)

        verify(callback, never()).invoke(any(), anyOrNull())
    }

    @Test
    fun whenCache_cacheEmpty_expectCallbacks() {
        val fetched = Date()
        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, fetched).change().cacheEmpty()

        val callback = mock<WhenCacheCallback<String>>()
        cacheState.whenCache(callback)

        verify(callback).invoke(null, fetched, false, false, null)
    }

    @Test
    fun whenCache_cacheData_expectCallbacks() {
        val data = "foo"
        val dataFetched = Date()
        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, dataFetched).change().cache(data)

        val callback = mock<WhenCacheCallback<String>>()
        cacheState.whenCache(callback)

        verify(callback).invoke(data, dataFetched, false, false, null)
    }

    @Test
    fun whenCache_noCache_expectNoCallbacks() {
        cacheState = OnlineCacheStateStateMachine.noCacheExists<String>(cacheRequirements).change().firstFetch()

        val callback = mock<WhenCacheCallback<String>>()
        cacheState.whenCache(callback)

        verify(callback, never()).invoke(anyOrNull(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun whenCache_firstFetchSuccessful_expectNoCallbacks() {
        val timeFetched = Date()
        cacheState = OnlineCacheStateStateMachine.noCacheExists<String>(cacheRequirements).change().firstFetch().change().successfulFirstFetch(timeFetched)

        val callback = mock<WhenCacheCallback<String>>()
        cacheState.whenCache(callback)

        verify(callback).invoke(null, timeFetched, false, true, null)
    }

    @Test
    fun whenCache_fetchingFreshCache_expectCallbacks() {
        val lastFetch = Date()
        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, lastFetch).change().fetchingFreshCache()

        val callback = mock<WhenCacheCallback<String>>()
        cacheState.whenCache(callback)

        verify(callback).invoke(null, lastFetch, true, false, null)
    }

    @Test
    fun whenCache_failFetch_expectCallbacks() {
        val error = RuntimeException()
        val fetched = Date()
        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, fetched).change().fetchingFreshCache().change().failRefreshCache(error)

        val callback = mock<WhenCacheCallback<String>>()
        cacheState.whenCache(callback)

        verify(callback).invoke(null, fetched, false, false, error)
    }

    @Test
    fun whenCache_successfulFetch_expectCallbacks() {
        val timeFetched = Date()
        cacheState = OnlineCacheStateStateMachine.cacheExists<String>(cacheRequirements, Date()).change().fetchingFreshCache().change().successfulRefreshCache(timeFetched)

        val callback = mock<WhenCacheCallback<String>>()
        cacheState.whenCache(callback)

        verify(callback).invoke(null, timeFetched, false, true, null)
    }

}