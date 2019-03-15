package com.levibostian.tellerexample.wrapper

import android.content.SharedPreferences
import com.f2prateek.rx.preferences2.RxSharedPreferences
import io.reactivex.Observable

class AppRxSharedPrefsWrapper(private val rxSharedPrefs: RxSharedPreferences): RxSharedPrefsWrapper {

    override fun observeString(key: String, defaultValue: String): Observable<String> {
        return rxSharedPrefs.getString(key, defaultValue).asObservable()
    }

}