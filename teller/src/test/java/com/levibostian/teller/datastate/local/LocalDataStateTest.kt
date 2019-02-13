package com.levibostian.teller.datastate.local

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.datastate.LocalDataState
import com.levibostian.teller.datastate.OnlineDataState
import com.levibostian.teller.datastate.listener.LocalDataStateListener
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
class LocalDataStateTest {

    private lateinit var dataState: LocalDataState<String>
    @Mock private lateinit var stateListener: LocalDataStateListener<String>

    @Test
    fun none_setsPropertiesCorrectly() {
        dataState = LocalDataState.none()

        assertThat(dataState.isEmpty).isFalse()
        assertThat(dataState.cacheData).isNull()
    }

    @Test
    fun isEmpty_setsPropertiesCorrectly() {
        dataState = LocalDataState.isEmpty()

        assertThat(dataState.isEmpty).isTrue()
        assertThat(dataState.cacheData).isNull()
    }

    @Test
    fun data_setsPropertiesCorrectly() {
        val data = "foo"
        dataState = LocalDataState.data(data)

        assertThat(dataState.isEmpty).isFalse()
        assertThat(dataState.cacheData).isEqualTo(data)
    }

    @Test
    fun deliverState_none_expectNoListenerCallbacks() {
        dataState = LocalDataState.none()

        dataState.deliverState(stateListener)

        verify(stateListener, never()).isEmpty()
        verify(stateListener, never()).data(anyOrNull())
    }

    @Test
    fun deliverState_isEmpty_expectCallListenerFunctions() {
        dataState = LocalDataState.isEmpty()

        dataState.deliverState(stateListener)

        verify(stateListener).isEmpty()
        verify(stateListener, never()).data(anyOrNull())
    }

    @Test
    fun deliverState_dataExists_expectCallListenerFunctions() {
        val data = "foo"
        dataState = LocalDataState.data(data)

        dataState.deliverState(stateListener)

        verify(stateListener, never()).isEmpty()
        verify(stateListener).data(data)
    }

}