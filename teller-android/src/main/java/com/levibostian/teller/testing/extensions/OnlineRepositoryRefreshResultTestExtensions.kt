package com.levibostian.teller.testing.extensions

import com.levibostian.teller.repository.TellerRepository
import com.levibostian.teller.testing.repository.OnlineRepositoryRefreshResultTesting

fun TellerRepository.RefreshResult.Testing.success(): TellerRepository.RefreshResult = OnlineRepositoryRefreshResultTesting.success()

fun TellerRepository.RefreshResult.Testing.failure(error: Throwable): TellerRepository.RefreshResult = OnlineRepositoryRefreshResultTesting.failure(error)

fun TellerRepository.RefreshResult.Testing.skipped(reason: TellerRepository.RefreshResult.SkippedReason): TellerRepository.RefreshResult = OnlineRepositoryRefreshResultTesting.skipped(reason)