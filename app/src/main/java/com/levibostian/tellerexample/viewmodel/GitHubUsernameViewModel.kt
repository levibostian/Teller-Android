package com.levibostian.tellerexample.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import com.f2prateek.rx.preferences2.RxSharedPreferences
import io.reactivex.BackpressureStrategy

class GitHubUsernameViewModel: ViewModel() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var rxSharedPreferences: RxSharedPreferences

    private val usernameKey = "githubUsernameKey"

    fun init(context: Context) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        this.rxSharedPreferences = RxSharedPreferences.create(sharedPreferences)
    }

    fun setUsername(username: String) {
        sharedPreferences.edit().putString(usernameKey, username).apply()
    }

    fun observeUsername(): LiveData<String> {
        return LiveDataReactiveStreams.fromPublisher(rxSharedPreferences.getString(usernameKey).asObservable().toFlowable(BackpressureStrategy.LATEST))
    }

}
