package com.levibostian.teller

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.teller.util.ConstantsUtil
import io.reactivex.Observable

class Teller internal constructor(private val context: Context) {

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

    internal val sharedPreferences: SharedPreferences = context.getSharedPreferences(ConstantsUtil.NAMESPACE, Context.MODE_PRIVATE)

    private fun init() {
    }

    /**
     * Deletes all Teller data. Nice to use if the user of your app logs out or during development purposes.
     *
     * This function does not delete any of your cached data. That is up to you to do. This simply removes all memory of ever caching data so the next time you use a Teller repository, a fetch will be performed.
     */
    fun clear() {
        deleteAllData()
    }

    @SuppressLint("ApplySharedPref")
    private fun deleteAllData() {
        sharedPreferences.edit().clear().commit()
    }

}