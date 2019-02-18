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