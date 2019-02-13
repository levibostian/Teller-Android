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