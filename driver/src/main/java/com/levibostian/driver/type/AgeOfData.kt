package com.levibostian.driver.type

import java.util.*

/**
 * Custom data class to represent a length of time. This is created because Duration is Java 8 only (requires API 26 on Android). I also did not want to bring in a library for this one very small need so I decided to create my own thing.
 */
data class AgeOfData(val time: Int, val unit: Unit) {

    enum class Unit {
        SECONDS,
        MINUTES,
        HOURS,
        DAYS,
        MONTHS,
        YEARS
    }

    fun toDate(): Date {
        val now = Calendar.getInstance()

        when (unit) {
            Unit.SECONDS -> { now.add(Calendar.SECOND, -time) }
            Unit.MINUTES -> { now.add(Calendar.MINUTE, -time) }
            Unit.HOURS -> { now.add(Calendar.HOUR, -time) }
            Unit.DAYS -> { now.add(Calendar.DAY_OF_MONTH, -time) }
            Unit.MONTHS -> { now.add(Calendar.MONTH, -time) }
            Unit.YEARS -> { now.add(Calendar.YEAR, -time) }
        }

        return now.time
    }

}