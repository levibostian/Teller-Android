package com.levibostian.teller.testing.repository

import com.levibostian.teller.repository.OnlineRepository

/**
 * Convenient utility to generate instances of [OnlineRepository.RefreshResult] used for testing purposes.
 *
 * Intentions of [OnlineRepositoryRefreshResultTesting]:
 * 1. Be able to initialize an instance of [OnlineRepository.RefreshResult] with 1 line of code.
 */
class OnlineRepositoryRefreshResultTesting {

    companion object {
        fun success(): OnlineRepository.RefreshResult = OnlineRepository.RefreshResult.success()

        fun failure(error: Throwable): OnlineRepository.RefreshResult = OnlineRepository.RefreshResult.failure(error)

        fun skipped(reason: OnlineRepository.RefreshResult.SkippedReason): OnlineRepository.RefreshResult = OnlineRepository.RefreshResult.skipped(reason)
    }

}