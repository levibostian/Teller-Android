package com.levibostian.teller

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.levibostian.teller.repository.OnlineRepository
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
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
     *
     * Pretty simple. This function converts a [List] into an [Observable]. You need to subscribe to it for the list to run.
     *
     * @return An [Observable] that will run [OnlineRepository.sync] on the list of [repositories] parameter. Note: The [Observable] returned does *not* run on a particular thread. You need to specify that.
     */
    fun sync(repositories: List<OnlineRepository<*, *, *>>, forceSync: Boolean): Observable<OnlineRepository.SyncResult> {
        var syncs: Observable<OnlineRepository.SyncResult> = Observable.empty()
        for (repository in repositories) {
            syncs = syncs.concatWith(repository.sync(forceSync))
        }
        return syncs
    }

}