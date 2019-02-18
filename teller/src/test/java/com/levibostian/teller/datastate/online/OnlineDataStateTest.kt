package com.levibostian.teller.datastate.online

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.datastate.OnlineDataState
import com.levibostian.teller.datastate.listener.OnlineDataStateCacheListener
import com.levibostian.teller.datastate.listener.OnlineDataStateFetchingListener
import com.levibostian.teller.datastate.listener.OnlineDataStateNoCacheStateListener
import com.levibostian.teller.datastate.online.statemachine.OnlineDataStateStateMachine
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

@RunWith(MockitoJUnitRunner::class)
class OnlineDataStateTest {

    private lateinit var dataState: OnlineDataState<String>
    @Mock private lateinit var dataRequirements: OnlineRepository.GetDataRequirements
    @Mock private lateinit var cacheStateListener: OnlineDataStateCacheListener<String>
    @Mock private lateinit var noCacheStateListener: OnlineDataStateNoCacheStateListener
    @Mock private lateinit var fetchingFreshCacheListener: OnlineDataStateFetchingListener

    @Test
    fun none_setsCorrectProperties() {
        dataState = OnlineDataState.none()

        assertThat(dataState.noCacheExists).isFalse()
        assertThat(dataState.fetchingForFirstTime).isFalse()
        assertThat(dataState.cacheData).isNull()
        assertThat(dataState.lastTimeFetched).isNull()
        assertThat(dataState.isFetchingFreshData).isFalse()
        assertThat(dataState.requirements).isNull()
        assertThat(dataState.stateMachine).isNull()
        assertThat(dataState.errorDuringFirstFetch).isNull()
        assertThat(dataState.justCompletedSuccessfulFirstFetch).isFalse()
        assertThat(dataState.errorDuringFetch).isNull()
        assertThat(dataState.justCompletedSuccessfullyFetchingFreshData).isFalse()
    }

    @Test
    fun deliverCacheState_cacheEmpty_expectCallListenerFunctions() {
        val fetched = Date()
        dataState = OnlineDataStateStateMachine.cacheExists<String>(dataRequirements, fetched).change().cacheIsEmpty()

        dataState.deliverCacheState(cacheStateListener)

        verify(cacheStateListener).cacheEmpty(fetched)
        verify(cacheStateListener, never()).cacheData(anyOrNull(), any())
    }

    @Test
    fun deliverCacheState_cacheData_expectCallListenerFunctions() {
        val data = "foo"
        val dataFetched = Date()
        dataState = OnlineDataStateStateMachine.cacheExists<String>(dataRequirements, dataFetched).change().cachedData(data)

        dataState.deliverCacheState(cacheStateListener)

        verify(cacheStateListener, never()).cacheEmpty(any())
        verify(cacheStateListener).cacheData(data, dataFetched)
    }

    @Test
    fun deliverCacheState_noCache_expectNoListenerCalls() {
        dataState = OnlineDataStateStateMachine.noCacheExists<String>(dataRequirements).change().firstFetch()

        dataState.deliverCacheState(cacheStateListener)

        verify(cacheStateListener, never()).cacheEmpty(any())
        verify(cacheStateListener, never()).cacheData(anyOrNull(), any())
    }

    @Test
    fun deliverNoCacheState_noCache_expectCallListenerFunctions() {
        dataState = OnlineDataStateStateMachine.noCacheExists(dataRequirements)

        dataState.deliverNoCacheState(noCacheStateListener)

        verify(noCacheStateListener).noCache()
        verify(noCacheStateListener, never()).firstFetch()
        verify(noCacheStateListener, never()).finishedFirstFetch(anyOrNull())
    }

    @Test
    fun deliverNoCacheState_fetchingForFirstTime_expectCallListenerFunctions() {
        dataState = OnlineDataStateStateMachine.noCacheExists<String>(dataRequirements).change().firstFetch()

        dataState.deliverNoCacheState(noCacheStateListener)

        verify(noCacheStateListener).noCache()
        verify(noCacheStateListener).firstFetch()
        verify(noCacheStateListener, never()).finishedFirstFetch(anyOrNull())
    }

    @Test
    fun deliverNoCacheState_finishedFirstFetch_expectCallListenerFunctions() {
        val timeFetched = Date()
        dataState = OnlineDataStateStateMachine.noCacheExists<String>(dataRequirements).change().firstFetch().change().successfulFirstFetch(timeFetched)

        dataState.deliverNoCacheState(noCacheStateListener)

        verify(noCacheStateListener, never()).noCache()
        verify(noCacheStateListener, never()).firstFetch()
        verify(noCacheStateListener).finishedFirstFetch(null)
    }

    @Test
    fun deliverNoCacheState_errorFirstFetch_expectCallListenerFunctions() {
        val error = RuntimeException("")
        dataState = OnlineDataStateStateMachine.noCacheExists<String>(dataRequirements).change().firstFetch().change().errorFirstFetch(error)

        dataState.deliverNoCacheState(noCacheStateListener)

        verify(noCacheStateListener).noCache()
        verify(noCacheStateListener, never()).firstFetch()
        verify(noCacheStateListener).finishedFirstFetch(error)
    }

    @Test
    fun deliverNoCacheState_cacheExists_expectNoListenerCalls() {
        dataState = OnlineDataStateStateMachine.cacheExists<String>(dataRequirements, Date()).change().cacheIsEmpty()

        dataState.deliverNoCacheState(noCacheStateListener)

        verify(noCacheStateListener, never()).noCache()
        verify(noCacheStateListener, never()).firstFetch()
        verify(noCacheStateListener, never()).finishedFirstFetch(anyOrNull())
    }

    @Test
    fun deliverFetchingFreshCacheState_fetchingFreshCache_expectCallListenerFunctions() {
        dataState = OnlineDataStateStateMachine.cacheExists<String>(dataRequirements, Date()).change().fetchingFreshCache()

        dataState.deliverFetchingFreshCacheState(fetchingFreshCacheListener)

        verify(fetchingFreshCacheListener).fetching()
        verify(fetchingFreshCacheListener, never()).finishedFetching(anyOrNull())
    }

    @Test
    fun deliverFetchingFreshCacheState_finishedFetchingFreshCache_expectCallListenerFunctions() {
        val error = RuntimeException()
        dataState = OnlineDataStateStateMachine.cacheExists<String>(dataRequirements, Date()).change().fetchingFreshCache().change().failFetchingFreshCache(error)

        dataState.deliverFetchingFreshCacheState(fetchingFreshCacheListener)

        verify(fetchingFreshCacheListener, never()).fetching()
        verify(fetchingFreshCacheListener).finishedFetching(error)
    }

    @Test
    fun deliverFetchingFreshCacheState_notFetchingFreshData_expectNoListenerCalls() {
        dataState = OnlineDataStateStateMachine.cacheExists<String>(dataRequirements, Date()).change().cacheIsEmpty()

        dataState.deliverFetchingFreshCacheState(fetchingFreshCacheListener)

        verify(fetchingFreshCacheListener, never()).fetching()
        verify(fetchingFreshCacheListener, never()).finishedFetching(anyOrNull())
    }

}