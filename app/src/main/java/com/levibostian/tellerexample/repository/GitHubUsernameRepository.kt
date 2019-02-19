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

class GitHubUsernameRepository(private val context: Context): LocalRepository<String, GitHubUsernameRepository.GitHubUsernameGetCacheRequirements>() {

    private val githubUsernameSharedPrefsKey = "${this::class.java.simpleName}_githubUsername_key"
    private val rxSharedPreferences: RxSharedPreferences = RxSharedPreferences.create(PreferenceManager.getDefaultSharedPreferences(context))
    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    /**
     * Use the `LocalRepository` as a regular repository. Add more functions to it to read, edit, delete the GitHub username data type.
     */
    var currentUsernameSaved: String? = null
        get() = sharedPreferences.getString(githubUsernameSharedPrefsKey, null)
        private set

    /**
     * Save cache data to the local Android device.
     * Note: This function is guaranteed to be called on a background thread.
     */
    override fun saveCache(cache: String, requirements: GitHubUsernameGetCacheRequirements) {
        // In this case, we are using SharedPreferences to save our data.
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(githubUsernameSharedPrefsKey, cache).apply()
    }

    /**
     * Return a RxJava2 `Observable` that continuously queries the locally cached data and notifies any observers of changes.
     * Note: This function is guaranteed to be called on the main UI thread.
     *
     * Do *not* send `null` in the `Observable` returned from this function. This is a rule set by RxJava and it will throw an exception.
     * If your cache data is in an "empty" state, either (1) return an empty value (Example: an empty String, "") or (2) create a POJO with an optional inside of it.
     */
    override fun observeCache(requirements: GitHubUsernameGetCacheRequirements): Observable<String> {
        // Here, we are using the RxSharedPreferences library to Observe the SharedPreferences
        // Library link: https://github.com/f2prateek/rx-preferences
        return rxSharedPreferences.getString(githubUsernameSharedPrefsKey, "")
                .asObservable()
                .filter { it.isNotBlank() }
                .subscribeOn(Schedulers.io())
    }

    /**
     * Help Teller determine is your cached data is "empty" or not.
     * Note: This function is guaranteed to be called on the same thread as `observeCache()`.
     *
     * In our example, an empty String (""), is what we qualify as empty. If you have a POJO, List, or any other data type, you return back if the `cache` parameter is empty or not.
     */
    override fun isCacheEmpty(cache: String, requirements: GitHubUsernameGetCacheRequirements): Boolean = cache.isBlank()

    class GitHubUsernameGetCacheRequirements: LocalRepository.GetCacheRequirements

}