package com.levibostian.teller.util

import com.levibostian.teller.TestConstants
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tiny companion wrapper for [CountDownLatch] to make testing async code easy.
 */
class Wait private constructor(number: Int) {

    private val countdown = CountDownLatch(number)
    val count: Long
        get() = countdown.count

    companion object {
        fun times(times: Int): Wait = Wait(times)
    }

    fun notDone(): Boolean = count > 0

    fun done(): Boolean = !notDone()

    fun countDown() {
        countdown.countDown()
    }

    fun await() {
        countdown.await(TestConstants.ASYNC_INSTRUMENTATION_TESTS_WAIT_TIME, TimeUnit.MILLISECONDS)
    }

}