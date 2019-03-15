package com.levibostian.teller.testing.repository

import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.teller.testing.extensions.failure
import com.levibostian.teller.testing.extensions.skipped
import com.levibostian.teller.testing.extensions.success
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class OnlineRepositoryRefreshResultTestingTest {

    @Test
    fun `success() - expect to get same as constructor`() {
        val original = OnlineRepository.RefreshResult.success()
        val testing = OnlineRepository.RefreshResult.Testing.success()

        assertThat(original).isEqualTo(testing)
    }

    @Test
    fun `failure() - expect to get same as constructor`() {
        val failure = RuntimeException("fail")
        val original = OnlineRepository.RefreshResult.failure(failure)
        val testing = OnlineRepository.RefreshResult.Testing.failure(failure)

        assertThat(original).isEqualTo(testing)
    }

    @Test
    fun `skipped() - expect to get same as constructor`() {
        val skippedReason = OnlineRepository.RefreshResult.SkippedReason.CANCELLED
        val original = OnlineRepository.RefreshResult.skipped(skippedReason)
        val testing = OnlineRepository.RefreshResult.Testing.skipped(skippedReason)

        assertThat(original).isEqualTo(testing)
    }

}