package com.levibostian.teller.repository.manager


import android.content.SharedPreferences
import android.preference.PreferenceManager

import org.junit.Test
import org.junit.runner.RunWith

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.repository.GetDataRequirementsTag

import org.junit.Before
import java.util.*

@RunWith(AndroidJUnit4::class)
class TellerOnlineRepositorySyncStateManagerIntegrationTest {

    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext())

    private lateinit var manager: OnlineRepositorySyncStateManager

    private val defaultTag: GetDataRequirementsTag = "tag"
    private val otherTag: GetDataRequirementsTag = "other_$defaultTag"

    @Before
    fun setup() {
        sharedPreferences.edit().clear().commit()

        manager = TellerOnlineRepositorySyncStateManager(sharedPreferences)
    }

    @Test
    fun lastTimeFetchedData_null_clearedManagerData() {
        assertThat(manager.lastTimeFetchedData(defaultTag)).isNull()
    }

    @Test
    fun lastTimeFetchedData_afterUpdateTimeLastFetched_getValueBack() {
        val lastFetchedOneDayAgo: Date = Calendar.getInstance().apply {
            add(Calendar.DATE, -1)
        }.time

        manager.updateAgeOfData(defaultTag, lastFetchedOneDayAgo)

        assertThat(manager.lastTimeFetchedData(defaultTag)!!).isEqualTo(lastFetchedOneDayAgo)

        assertThat(manager.lastTimeFetchedData(otherTag)).isNull()
    }

}
