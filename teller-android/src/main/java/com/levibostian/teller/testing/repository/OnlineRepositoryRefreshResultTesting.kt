package com.levibostian.teller.testing.repository

import com.levibostian.teller.repository.TellerRepository

/**
 * Convenient utility to generate instances of [TellerRepository.RefreshResult] used for testing purposes.
 *
 * You can use this class directly, or, use the recommended extension functions in the [TellerRepository.RefreshResult.Testing] object.
 *
 * Intentions of [OnlineRepositoryRefreshResultTesting]:
 * 1. Be able to initialize an instance of [TellerRepository.RefreshResult] with 1 line of code.
 */
class OnlineRepositoryRefreshResultTesting private constructor() {

    companion object {
        fun success(): TellerRepository.RefreshResult = TellerRepository.RefreshResult.success()

        fun failure(error: Throwable): TellerRepository.RefreshResult = TellerRepository.RefreshResult.failure(error)

        fun skipped(reason: TellerRepository.RefreshResult.SkippedReason): TellerRepository.RefreshResult = TellerRepository.RefreshResult.skipped(reason)
    }

}