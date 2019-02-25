package com.levibostian.teller.cachestate.online.statemachine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class FetchingFreshCacheStateMachineTest {

    private lateinit var machine: FetchingFreshCacheStateMachine

    @Test
    fun notFetching_setsCorrectProperties() {
        val lastTimeFetched = Date()
        machine = FetchingFreshCacheStateMachine.notFetching(lastTimeFetched)

        assertThat(machine.state).isEqualTo(FetchingFreshCacheStateMachine.State.NOT_FETCHING)
        assertThat(machine.errorDuringFetch).isNull()
        assertThat(machine.lastTimeFetched).isEqualTo(lastTimeFetched)
    }

    @Test
    fun fetching_setsCorrectProperties() {
        val lastTimeFetched = Date()
        machine = FetchingFreshCacheStateMachine.notFetching(lastTimeFetched).fetching()

        assertThat(machine.state).isEqualTo(FetchingFreshCacheStateMachine.State.IS_FETCHING)
        assertThat(machine.errorDuringFetch).isNull()
        assertThat(machine.lastTimeFetched).isEqualTo(lastTimeFetched)
    }

    @Test
    fun failedFetching_setsCorrectProperties() {
        val lastTimeFetched = Date()
        val error = RuntimeException("")
        machine = FetchingFreshCacheStateMachine.notFetching(lastTimeFetched).fetching().failedFetching(error)

        assertThat(machine.state).isEqualTo(FetchingFreshCacheStateMachine.State.NOT_FETCHING)
        assertThat(machine.errorDuringFetch).isEqualTo(error)
        assertThat(machine.lastTimeFetched).isEqualTo(lastTimeFetched)
    }

    @Test
    fun successfulFetch_setsCorrectProperties() {
        val lastTimeFetched = Date()
        val newTimeFetched: Date = Calendar.getInstance().apply {
            time = lastTimeFetched
            add(Calendar.MINUTE, 1)
        }.time
        machine = FetchingFreshCacheStateMachine.notFetching(lastTimeFetched).fetching().successfulFetch(newTimeFetched)

        assertThat(machine.state).isEqualTo(FetchingFreshCacheStateMachine.State.NOT_FETCHING)
        assertThat(machine.errorDuringFetch).isNull()
        assertThat(machine.lastTimeFetched).isEqualTo(newTimeFetched)
    }

}