package com.levibostian.teller.util

import io.reactivex.functions.Predicate

internal class AndroidTestingAssertionUtil {

    companion object {

        /**
         * Used in observable.test().assertValue() to assert the value using Truth.
         *
         * AssertValue is weird in that it returns a boolean, but if not done correctly you will get this generic error:
         * java.lang.AssertionError: Value not present (latch = 0, values = 1, errors = 0, completions = 1)
         *
         * ...which, as we can see, a value *is* present (see: values=1)
         *
         * So, we use Truth instead. This static function makes it easy.
         */
        fun <T> check(consumer: (T) -> Unit): Predicate<T> {
            return Predicate { t ->
                consumer.invoke(t)
                true
            }
        }
    }

}