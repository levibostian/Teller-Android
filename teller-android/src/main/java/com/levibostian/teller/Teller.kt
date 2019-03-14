package com.levibostian.teller

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.levibostian.teller.error.TellerLimitedFunctionalityException
import com.levibostian.teller.extensions.getTellerSharedPreferences
import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.teller.util.ConstantsUtil
import io.reactivex.Observable

class Teller internal constructor(internal val sharedPreferences: SharedPreferences?,
                                  internal val unitTesting: Boolean) {

    companion object {
        private var instance: Teller? = null

        fun init(application: Application) {
            if (instance == null) instance = getInstance(application)
        }

        /**
         * Use to initialize Teller for instrumentation tests.
         *
         * @param sharedPreferences Pass in instance of [SharedPreferences] to use for Teller. This allows you to control if you are using Robolectric, Android instrumentation tests, etc.
         */
        fun initTesting(sharedPreferences: SharedPreferences) {
            if (instance == null) instance = getTestingInstance(sharedPreferences)
        }

        /**
         * Use to initialize Teller for unit tests giving Teller limited functionality.
         */
        fun initUnitTesting() {
            if (instance == null) instance = getUnitTestingInstance()
        }

        internal fun getInstance(application: Application): Teller = Teller(application.getTellerSharedPreferences(), false)
        internal fun getTestingInstance(sharedPreferences: SharedPreferences): Teller = Teller(sharedPreferences, false)
        internal fun getUnitTestingInstance(): Teller = Teller(null, true)

        /**
         * Get singleton instance of [Teller].
         *
         * @throws RuntimeException If you have not called [Teller.Companion.init] yet to initialize singleton instance.
         */
        fun sharedInstance(): Teller {
            if (instance == null) throw RuntimeException("Oh, no! You forgot to call Teller.init()")
            return instance!!
        }

        /**
         * Short hand version of calling [sharedInstance].
         */
        val shared: Teller by lazy { sharedInstance() }
    }

    /**
     * Deletes all Teller data. Nice to use if the user of your app logs out, or delete Teller data during development of your app for testing.
     *
     * This function does not delete any of your cache. That is up to you to do. This simply removes all records of ever fetching a cache so the next time you use a Teller repository, a fetch will be performed.
     */
    fun clear() {
        assertNotLimitedFunctionality()

        deleteAllData()
    }

    internal fun assertNotLimitedFunctionality() {
        if (unitTesting) throw TellerLimitedFunctionalityException("Teller has been initialized for testing. You cannot run this function while unit testing. Execute tests with Robolectric or Android instrumentation tests to execute Teller.")
    }

    @SuppressLint("ApplySharedPref")
    private fun deleteAllData() {
        sharedPreferences!!.edit().clear().commit()
    }

}