package com.levibostian.teller.cachestate.statemachine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class NoCacheStateMachineTest {

    private lateinit var machine: NoCacheStateMachine

    @Test
    fun noCacheExists_setsCorrectProperties() {
        machine = NoCacheStateMachine.noCacheExists()

        assertThat(machine.state).isEqualTo(NoCacheStateMachine.State.NO_CACHE_EXISTS)
        assertThat(machine.errorDuringFetch).isNull()
    }

    @Test
    fun fetching_setsCorrectProperties() {
        machine = NoCacheStateMachine.noCacheExists().fetching()

        assertThat(machine.state).isEqualTo(NoCacheStateMachine.State.IS_FETCHING)
        assertThat(machine.errorDuringFetch).isNull()
    }

    @Test
    fun failedFetching_setsCorrectProperties() {
        val error = RuntimeException("")
        machine = NoCacheStateMachine.noCacheExists().fetching().failedFetching(error)

        assertThat(machine.state).isEqualTo(NoCacheStateMachine.State.NO_CACHE_EXISTS)
        assertThat(machine.errorDuringFetch).isEqualTo(error)
    }

}