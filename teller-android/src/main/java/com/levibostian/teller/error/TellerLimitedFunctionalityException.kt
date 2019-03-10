package com.levibostian.teller.error

/**
 * Thrown when Teller is initialized for unit testing use, but a function is called on Teller it cannot perform with this limited functionality mode.
 */
class TellerLimitedFunctionalityException(message: String): Throwable(message)