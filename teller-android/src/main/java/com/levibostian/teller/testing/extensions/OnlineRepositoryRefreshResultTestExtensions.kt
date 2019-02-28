package com.levibostian.teller.testing.extensions

import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.teller.testing.repository.OnlineRepositoryRefreshResultTesting

fun OnlineRepository.RefreshResult.Testing.success(): OnlineRepository.RefreshResult = OnlineRepositoryRefreshResultTesting.success()

fun OnlineRepository.RefreshResult.Testing.failure(error: Throwable): OnlineRepository.RefreshResult = OnlineRepositoryRefreshResultTesting.failure(error)

fun OnlineRepository.RefreshResult.Testing.skipped(reason: OnlineRepository.RefreshResult.SkippedReason): OnlineRepository.RefreshResult = OnlineRepositoryRefreshResultTesting.skipped(reason)