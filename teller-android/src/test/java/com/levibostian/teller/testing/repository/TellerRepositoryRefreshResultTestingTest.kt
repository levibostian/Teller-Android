package com.levibostian.teller.testing.repository

import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.repository.TellerRepository
import com.levibostian.teller.testing.extensions.failure
import com.levibostian.teller.testing.extensions.skipped
import com.levibostian.teller.testing.extensions.success
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class TellerRepositoryRefreshResultTestingTest {

    @Test
    fun `success() - expect to get same as constructor`() {
        val original = TellerRepository.RefreshResult.success()
        val testing = TellerRepository.RefreshResult.Testing.success()

        assertThat(original).isEqualTo(testing)
    }

    @Test
    fun `failure() - expect to get same as constructor`() {
        val failure = RuntimeException("fail")
        val original = TellerRepository.RefreshResult.failure(failure)
        val testing = TellerRepository.RefreshResult.Testing.failure(failure)

        assertThat(original).isEqualTo(testing)
    }

    @Test
    fun `skipped() - expect to get same as constructor`() {
        val skippedReason = TellerRepository.RefreshResult.SkippedReason.CANCELLED
        val original = TellerRepository.RefreshResult.skipped(skippedReason)
        val testing = TellerRepository.RefreshResult.Testing.skipped(skippedReason)

        assertThat(original).isEqualTo(testing)
    }

}