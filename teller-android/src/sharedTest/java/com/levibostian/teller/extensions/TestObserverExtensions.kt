package com.levibostian.teller.extensions

import com.levibostian.teller.TestConstants
import io.reactivex.observers.BaseTestConsumer
import java.util.concurrent.TimeUnit

/**
 * I have found that [BaseTestConsumer.awaitDone] with a short timeout seems to work when you have an TestObserver that you are wanting to wait until it is disposed.
 *
 * Example:
 *
 * ```
 * val fetchTestObserver = observable.test()
 *
 * ...Run function under test...
 *
 * fetchTestObserver
 *   .awaitDispose()
 * assertThat(fetchTestObserver.isDisposed).isTrue()
 * ```
 */
internal fun <T, U: BaseTestConsumer<T, U>> BaseTestConsumer<T, U>.awaitDispose(): U {
    return awaitDone(TestConstants.RX_TEST_OBSERVER_AWAIT_DONE_TIME, TimeUnit.MILLISECONDS)
}

/**
 * Convenient alternative to `.awaitDone(time, timeunit)` entering parameters each time you want to use.
 */
internal fun <T, U: BaseTestConsumer<T, U>> BaseTestConsumer<T, U>.awaitDone(): U {
    return awaitDone(TestConstants.RX_TEST_OBSERVER_AWAIT_DONE_TIME, TimeUnit.MILLISECONDS)
}

/**
 * If a timeout is hit while using [BaseTestConsumer.await].
 */
class AwaitTimeoutException(message: String = ""): Throwable(message)
/**
 * Convenient alternative to `.await()` which does not timeout, and to `.await(time, unit)` which adds boilerplate. This is a way to have a short, default value set for all tests and times out to avoid tests hanging.
 *
 * @throws AwaitTimeoutException if a timeout is hit.
 */
@Suppress("UNCHECKED_CAST")
internal fun <T, U: BaseTestConsumer<T, U>> BaseTestConsumer<T, U>.awaitComplete(): U {
    val result = await(TestConstants.RX_TEST_OBSERVER_AWAIT_COMPLETE_TIME, TimeUnit.MILLISECONDS)

    if (!result) throw AwaitTimeoutException()

    return this as U
}