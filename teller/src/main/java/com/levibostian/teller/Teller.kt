package com.levibostian.teller

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.levibostian.teller.repository.OnlineRepository
import io.reactivex.Completable
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers

class Teller private constructor(private val context: Context) {

    companion object {
        private var instance: Teller? = null

        @JvmStatic fun init(application: Application) {
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
        @JvmStatic fun sharedInstance(): Teller {
            if (instance == null) throw RuntimeException("Oh, no! You forgot to call Teller.init()")
            return instance!!
        }

        /**
         * Short hand version of calling [sharedInstance].
         */
        @JvmStatic val shared: Teller by lazy { sharedInstance() }
    }

    internal val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private fun init() {
    }

    /**
     * Run [OnlineRepository.sync] on a list of [OnlineRepository] at one time.
     */
    fun sync(repositories: List<OnlineRepository<*, *, *>>, forceSync: Boolean): Completable {
        var syncs: Completable = Completable.complete()
        for (repository in repositories) {
            syncs = syncs.andThen(repository.sync(forceSync))
        }
        return syncs.subscribeOn(Schedulers.io())
    }

}