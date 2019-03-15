package com.levibostian.teller.cachestate.local

import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.cachestate.LocalCacheState
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

    @Mock private lateinit var requirements: LocalRepository.GetCacheRequirements

    @Test
    fun none_setsPropertiesCorrectly() {
        cacheState = LocalCacheState.none()

        assertThat(cacheState.cache).isNull()
        assertThat(cacheState.requirements).isNull()
    }

    @Test
    fun isEmpty_setsPropertiesCorrectly() {
        cacheState = LocalCacheState.isEmpty(requirements)

        assertThat(cacheState.cache).isNull()
        assertThat(cacheState.requirements).isEqualTo(requirements)
    }

    @Test
    fun data_setsPropertiesCorrectly() {
        val data = "foo"
        cacheState = LocalCacheState.cache(requirements, data)

        assertThat(cacheState.cache).isEqualTo(data)
        assertThat(cacheState.requirements).isEqualTo(requirements)
    }

}