package com.levibostian.teller

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.levibostian.teller.extensions.getTellerSharedPreferences
import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.teller.util.ConstantsUtil
import io.reactivex.Observable

class Teller internal constructor(context: Context) {

    companion object {
        private var instance: Teller? = null

        @JvmStatic
        fun init(application: Application) {
            if (instance == null) {
                instance = Teller(application)
                instance!!.init()
            }
        }

        /**
         * Get singleton instance of [Teller].
         *
         * @throws RuntimeException If you have not called [Teller.Companion.init] yet to initialize singleton instance.
         */
        @JvmStatic
        fun sharedInstance(): Teller {
            if (instance == null) throw RuntimeException("Oh, no! You forgot to call Teller.init()")
            return instance!!
        }

        /**
         * Short hand version of calling [sharedInstance].
         */
        @JvmStatic
        val shared: Teller by lazy { sharedInstance() }
    }

    internal val sharedPreferences: SharedPreferences = context.getTellerSharedPreferences()

    private fun init() {
    }

    /**
     * Deletes all Teller data. Nice to use if the user of your app logs out, or delete Teller data during development of your app for testing.
     *
     * This function does not delete any of your cache. That is up to you to do. This simply removes all records of ever fetching a cache so the next time you use a Teller repository, a fetch will be performed.
     */
    fun clear() {
        deleteAllData()
    }

    @SuppressLint("ApplySharedPref")
    private fun deleteAllData() {
        sharedPreferences.edit().clear().commit()
    }

}