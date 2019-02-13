package com.levibostian.teller.datastate.online.statemachine

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
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