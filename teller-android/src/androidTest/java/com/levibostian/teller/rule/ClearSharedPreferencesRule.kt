package com.levibostian.teller.rule

import android.content.SharedPreferences
import org.junit.rules.ExternalResource
import org.mockito.MockitoAnnotations

class ClearSharedPreferencesRule(private val sharedPreferences: SharedPreferences): ExternalResource() {

    override fun before() {
        sharedPreferences.edit().clear().commit()
    }

}