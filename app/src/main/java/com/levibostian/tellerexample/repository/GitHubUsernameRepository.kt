package com.levibostian.tellerexample.repository

import android.annotation.SuppressLint
import android.content.Context
import android.preference.PreferenceManager
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.levibostian.teller.repository.LocalRepository
import io.reactivex.Completable
import io.reactivex.Observable
import android.content.SharedPreferences
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers

class GitHubUsernameRepository(private val context: Context): LocalRepository<String, GitHubUsernameRepository.GetRequirements>() {

    private val githubUsernameSharedPrefsKey = "${this::class.java.simpleName}_githubUsername_key"
    private val rxSharedPreferences: RxSharedPreferences = RxSharedPreferences.create(PreferenceManager.getDefaultSharedPreferences(context))
    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    var currentUsernameSaved: String? = null
        get() = sharedPreferences.getString(githubUsernameSharedPrefsKey, null)
        private set

    // Save data to a cache. In this case, we are using SharedPreferences to save our data.
    override fun saveCache(cache: String, requirements: GetRequirements) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(githubUsernameSharedPrefsKey, cache).apply()
    }

    // Using RxJava2 Observables, you query your cached data.
    override fun observeCache(requirements: GetRequirements): Observable<String> {
        return rxSharedPreferences.getString(githubUsernameSharedPrefsKey, "")
                .asObservable()
                .filter { it.isNotBlank() }
                .subscribeOn(Schedulers.io())
    }

    // Help Teller to determine if data is empty or not. Teller uses this when parsing the cache to determine if a data set is empty or not.
    override fun isCacheEmpty(cache: String, requirements: GetRequirements): Boolean = cache.isBlank()

    class GetRequirements: LocalRepository.GetCacheRequirements

}