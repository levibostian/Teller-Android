package com.levibostian.teller.testing.cachestate

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.cachestate.LocalCacheState
import com.levibostian.teller.cachestate.OnlineCacheState
import com.levibostian.teller.cachestate.listener.LocalCacheStateListener
import com.levibostian.teller.cachestate.online.statemachine.OnlineCacheStateStateMachine
import com.levibostian.teller.repository.GetCacheRequirementsTag
import com.levibostian.teller.repository.LocalRepository
import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.teller.testing.extensions.cache
import com.levibostian.teller.testing.extensions.isEmpty
import com.levibostian.teller.testing.extensions.noCache
import com.levibostian.teller.testing.extensions.none
import org.junit.Test

import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import com.nhaarman.mockito_kotlin.*
import java.util.*
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class LocalCacheStateTestingTest {

    @Mock lateinit var requirements: LocalRepository.GetCacheRequirements

    @Test
    fun `none() expect result to equal internal generation of LocalCacheState`() {
        val fromStateMachine = LocalCacheState.none<String>()
        val testing = LocalCacheState.Testing.none<String>()

        assertThat(fromStateMachine).isEqualTo(testing)
    }

    @Test
    fun `isEmpty() - expect result to equal internal generation of LocalCacheState`() {
        val fromStateMachine = LocalCacheState.isEmpty<String>(requirements)
        val testing = LocalCacheState.Testing.isEmpty<String>(requirements)

        assertThat(fromStateMachine).isEqualTo(testing)
    }

    @Test
    fun `cache() - expect result to equal internal generation of LocalCacheState`() {
        val cache = "cache"
        val fromStateMachine = LocalCacheState.cache(requirements, cache)
        val testing = LocalCacheState.Testing.cache(requirements, cache)

        assertThat(fromStateMachine).isEqualTo(testing)
    }

}
