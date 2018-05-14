package com.levibostian.driver

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class Driver private constructor(private val context: Context) {

    companion object {
        private var instance: Driver? = null

        @JvmStatic fun init(application: Application) {
            if (instance == null) {
                instance = Driver(application)
                instance!!.init()
            }
        }

        /**
         * Get singleton instance of [Driver].
         *
         * @throws RuntimeException If you have not called [Driver.Companion.init] yet to initialize singleton instance.
         */
        @JvmStatic fun sharedInstance(): Driver {
            if (instance == null) throw RuntimeException("Sorry, you must call Driver.init() first.")
            return instance!!
        }

        /**
         * Short hand version of calling [sharedInstance].
         */
        @JvmStatic val shared: Driver by lazy { sharedInstance() }
    }

    internal val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private fun init() {
    }

}