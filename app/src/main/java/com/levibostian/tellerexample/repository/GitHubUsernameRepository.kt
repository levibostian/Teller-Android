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

class GitHubUsernameRepository(private val context: Context): LocalRepository<String>() {

    private val githubUsernameSharedPrefsKey = "${this::class.java.simpleName}_githubUsername_key"
    private val rxSharedPreferences: RxSharedPreferences = RxSharedPreferences.create(PreferenceManager.getDefaultSharedPreferences(context))

    override fun saveData(data: String) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(githubUsernameSharedPrefsKey, data).apply()
    }

    override fun observeData(): Observable<String> {
        return rxSharedPreferences.getString(githubUsernameSharedPrefsKey, "")
                .asObservable()
                .filter { it.isNotBlank() }
                .subscribeOn(Schedulers.io())
    }

    override fun isDataEmpty(data: String): Boolean = data.isBlank()

}