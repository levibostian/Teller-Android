package com.levibostian.teller.cachestate.local

import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.cachestate.LocalCacheState
import com.levibostian.teller.cachestate.listener.LocalCacheStateListener
import org.junit.Test

import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import com.nhaarman.mockito_kotlin.*

@RunWith(MockitoJUnitRunner::class)
class LocalCacheStateTest {

    private lateinit var cacheState: LocalCacheState<String>
    @Mock private lateinit var stateListener: LocalCacheStateListener<String>

    @Test
    fun none_setsPropertiesCorrectly() {
        cacheState = LocalCacheState.none()

        assertThat(cacheState.isEmpty).isFalse()
        assertThat(cacheState.cacheData).isNull()
    }

    @Test
    fun isEmpty_setsPropertiesCorrectly() {
        cacheState = LocalCacheState.isEmpty()

        assertThat(cacheState.isEmpty).isTrue()
        assertThat(cacheState.cacheData).isNull()
    }

    @Test
    fun data_setsPropertiesCorrectly() {
        val data = "foo"
        cacheState = LocalCacheState.data(data)

        assertThat(cacheState.isEmpty).isFalse()
        assertThat(cacheState.cacheData).isEqualTo(data)
    }

    @Test
    fun deliverState_none_expectNoListenerCallbacks() {
        cacheState = LocalCacheState.none()

        cacheState.deliverState(stateListener)

        verify(stateListener, never()).isEmpty()
        verify(stateListener, never()).data(anyOrNull())
    }

    @Test
    fun deliverState_isEmpty_expectCallListenerFunctions() {
        cacheState = LocalCacheState.isEmpty()

        cacheState.deliverState(stateListener)

        verify(stateListener).isEmpty()
        verify(stateListener, never()).data(anyOrNull())
    }

    @Test
    fun deliverState_dataExists_expectCallListenerFunctions() {
        val data = "foo"
        cacheState = LocalCacheState.data(data)

        cacheState.deliverState(stateListener)

        verify(stateListener, never()).isEmpty()
        verify(stateListener).data(data)
    }

}