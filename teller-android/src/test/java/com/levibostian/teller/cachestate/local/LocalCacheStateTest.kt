package com.levibostian.teller.cachestate.local

import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.cachestate.LocalCacheState
import com.levibostian.teller.cachestate.listener.LocalCacheStateListener
import com.levibostian.teller.repository.LocalRepository
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class LocalCacheStateTest {

    private lateinit var cacheState: LocalCacheState<String>

    @Mock private lateinit var stateListener: LocalCacheStateListener<String>
    @Mock private lateinit var requirements: LocalRepository.GetCacheRequirements

    @Test
    fun none_setsPropertiesCorrectly() {
        cacheState = LocalCacheState.none()

        assertThat(cacheState.isEmpty).isFalse()
        assertThat(cacheState.cacheData).isNull()
        assertThat(cacheState.requirements).isNull()
    }

    @Test
    fun isEmpty_setsPropertiesCorrectly() {
        cacheState = LocalCacheState.isEmpty(requirements)

        assertThat(cacheState.isEmpty).isTrue()
        assertThat(cacheState.cacheData).isNull()
        assertThat(cacheState.requirements).isEqualTo(requirements)
    }

    @Test
    fun data_setsPropertiesCorrectly() {
        val data = "foo"
        cacheState = LocalCacheState.cache(requirements, data)

        assertThat(cacheState.isEmpty).isFalse()
        assertThat(cacheState.cacheData).isEqualTo(data)
        assertThat(cacheState.requirements).isEqualTo(requirements)
    }

    @Test
    fun deliverState_none_expectNoListenerCallbacks() {
        cacheState = LocalCacheState.none()

        cacheState.deliverState(stateListener)

        verify(stateListener, never()).isEmpty()
        verify(stateListener, never()).cache(anyOrNull())
    }

    @Test
    fun deliverState_isEmpty_expectCallListenerFunctions() {
        cacheState = LocalCacheState.isEmpty(requirements)

        cacheState.deliverState(stateListener)

        verify(stateListener).isEmpty()
        verify(stateListener, never()).cache(anyOrNull())
    }

    @Test
    fun deliverState_dataExists_expectCallListenerFunctions() {
        val data = "foo"
        cacheState = LocalCacheState.cache(requirements, data)

        cacheState.deliverState(stateListener)

        verify(stateListener, never()).isEmpty()
        verify(stateListener).cache(data)
    }

}