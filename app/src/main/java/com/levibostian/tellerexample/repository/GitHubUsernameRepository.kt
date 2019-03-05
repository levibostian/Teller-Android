package com.levibostian.tellerexample.repository

import android.annotation.SuppressLint
import android.content.Context
import android.preference.PreferenceManager
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.levibostian.teller.repository.LocalRepository
import io.reactivex.Completable
import io.reactivex.Observable
import android.content.SharedPreferences
import com.levibostian.tellerexample.service.provider.AppSchedulersProvider
import com.levibostian.tellerexample.service.provider.SchedulersProvider
import com.levibostian.tellerexample.util.DependencyUtil
import com.levibostian.tellerexample.wrapper.RxSharedPrefsWrapper
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers

class GitHubUsernameRepository: LocalRepository<String, GitHubUsernameRepository.GitHubUsernameGetCacheRequirements> {

    companion object {
        internal const val GITHUB_USERNAME_SHARED_PREFS_KEY = "GitHubUsernameRepository_githubUsername_key"
        internal const val GITHUB_USERNAME_SHARED_PREFS_DEFAULT = ""
    }

    private val rxSharedPrefsWrapper: RxSharedPrefsWrapper
    private val sharedPreferences: SharedPreferences
    private val schedulersProvider: SchedulersProvider

    constructor(context: Context): super() {
        rxSharedPrefsWrapper = DependencyUtil.rxSharedPreferences(context)
        sharedPreferences = DependencyUtil.sharedPreferences(context)
        schedulersProvider = AppSchedulersProvider()
    }

    constructor(
            rxSharedPrefsWrapper: RxSharedPrefsWrapper,
            sharedPreferences: SharedPreferences,
            schedulersProvider: SchedulersProvider
    ) {
        this.rxSharedPrefsWrapper = rxSharedPrefsWrapper
        this.sharedPreferences = sharedPreferences
        this.schedulersProvider = schedulersProvider
    }

    /**
     * Use the `LocalRepository` as a regular repository. Add more functions to it to read, edit, delete the GitHub username cache.
     */
    var currentUsernameSaved: String? = null
        get() = sharedPreferences.getString(GITHUB_USERNAME_SHARED_PREFS_KEY, null)
        private set

    /**
     * Save cache to the local Android device.
     * Note: This function is guaranteed to be called on a background thread.
     */
    public override fun saveCache(cache: String, requirements: GitHubUsernameGetCacheRequirements) {
        // In this case, we are using SharedPreferences to save our cache.
        sharedPreferences.edit().putString(GITHUB_USERNAME_SHARED_PREFS_KEY, cache).apply()
    }

    /**
     * Return a RxJava2 `Observable` that continuously queries the local cache and notifies any observers of changes.
     * Note: This function is guaranteed to be called on the main UI thread.
     *
     * Do *not* send `null` in the `Observable` returned from this function. This is a rule set by RxJava and it will throw an exception.
     * If your cache is in an "empty" state, either (1) return an empty value (Example: an empty String, "") or (2) create a POJO with an optional inside of it.
     */
    public override fun observeCache(requirements: GitHubUsernameGetCacheRequirements): Observable<String> {
        // Here, we are using the RxSharedPreferences library to Observe the SharedPreferences
        // Library link: https://github.com/f2prateek/rx-preferences
        return rxSharedPrefsWrapper.observeString(GITHUB_USERNAME_SHARED_PREFS_KEY, GITHUB_USERNAME_SHARED_PREFS_DEFAULT)
                .subscribeOn(schedulersProvider.io())
    }

    /**
     * Help Teller determine is your cache is "empty" or not.
     * Note: This function is guaranteed to be called on the same thread as `observeCache()`.
     *
     * In our example, an empty String (""), is what we qualify as empty. If you have a POJO, List, or any other type, you return back if the `cache` parameter is empty or not.
     */
    public override fun isCacheEmpty(cache: String, requirements: GitHubUsernameGetCacheRequirements): Boolean = cache.isBlank()

    class GitHubUsernameGetCacheRequirements: LocalRepository.GetCacheRequirements

}