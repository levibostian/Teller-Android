package com.levibostian.teller.repository.manager


import android.content.SharedPreferences
import android.preference.PreferenceManager

import org.junit.Test
import org.junit.runner.RunWith

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.repository.GetCacheRequirementsTag

import org.junit.Before
import java.util.*

@RunWith(AndroidJUnit4::class)
class TellerTellerRepositoryCacheAgeManagerIntegrationTest {

    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext())

    private lateinit var manager: RepositoryCacheAgeManager

    private val defaultTag: GetCacheRequirementsTag = "tag"
    private val otherTag: GetCacheRequirementsTag = "other_$defaultTag"

    @Before
    fun setup() {
        sharedPreferences.edit().clear().commit()

        manager = TellerRepositoryCacheAgeManager(sharedPreferences)
    }

    @Test
    fun lastSuccessfulFetch_null_clearedManagerCache() {
        assertThat(manager.lastSuccessfulFetch(defaultTag)).isNull()
    }

    @Test
    fun lastSuccessfulFetch_afterUpdateTimeLastFetched_getValueBack() {
        val lastFetchedOneDayAgo: Date = Calendar.getInstance().apply {
            add(Calendar.DATE, -1)
        }.time

        manager.updateLastSuccessfulFetch(defaultTag, lastFetchedOneDayAgo)

        assertThat(manager.lastSuccessfulFetch(defaultTag)!!).isEqualTo(lastFetchedOneDayAgo)

        assertThat(manager.lastSuccessfulFetch(otherTag)).isNull()
    }

}
