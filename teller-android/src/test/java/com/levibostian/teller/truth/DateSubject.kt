package com.levibostian.teller.truth

import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth
import java.util.*

/**
 * Custom extension to [Truth.assertThat].
 *
 * Use: assertThat(date).isNewerThan(otherDate)
 *
 * (after `import com.levibostian.teller.truth.DateSubject.Companion.assertThat` to import static function.
 */
class DateSubject(metaData: FailureMetadata, private val actual: Date): Subject(metaData, actual) {

    companion object {
        private val factory = object : Subject.Factory<DateSubject, Date> {
            override fun createSubject(metadata: FailureMetadata, actual: Date): DateSubject {
                return DateSubject(metadata, actual)
            }
        }

        fun dates(): Subject.Factory<DateSubject, Date> = factory
        fun assertThat(actual: Date): DateSubject = Truth.assertAbout(dates()).that(actual)
    }

    fun isNewerThan(expected: Date) {
        check("date").that(actual.time).isGreaterThan(expected.time)
    }

    fun isOlderThan(expected: Date) {
        check("date").that(actual.time).isLessThan(expected.time)
    }

}