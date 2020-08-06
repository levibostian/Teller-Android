package com.levibostian.teller.cachestate.statemachine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class CacheStateMachineTestTest {

    private lateinit var machine: CacheStateMachine<String>

    @Test
    fun cacheEmpty_setsCorrectProperties() {
        machine = CacheStateMachine.cacheEmpty()

        assertThat(machine.state).isEqualTo(CacheStateMachine.State.CACHE_EMPTY)
        assertThat(machine.cache).isNull()
    }

    @Test
    fun cacheExists_setsCorrectProperties() {
        val cache = "cache"
        machine = CacheStateMachine.cacheExists(cache)

        assertThat(machine.state).isEqualTo(CacheStateMachine.State.CACHE_NOT_EMPTY)
        assertThat(machine.cache).isEqualTo(cache)
    }

}