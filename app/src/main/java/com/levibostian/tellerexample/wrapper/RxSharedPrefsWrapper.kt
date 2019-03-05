package com.levibostian.tellerexample.wrapper

import io.reactivex.Observable

interface RxSharedPrefsWrapper {
    fun observeString(key: String, defaultValue: String): Observable<String>
}