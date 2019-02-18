package com.levibostian.teller.repository.manager


import android.content.SharedPreferences
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.type.AgeOfData
import com.nhaarman.mockito_kotlin.verify
import org.junit.Test

import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class TellerOnlineRepositorySyncStateManagerTest {

    @Mock private lateinit var sharedPreferences: SharedPreferences
    @Mock private lateinit var sharedPreferencesEditor: SharedPreferences.Editor

    private lateinit var manager: TellerOnlineRepositorySyncStateManager

    private val defaultTag = "tag"

    @Before
    fun setup() {
        `when`(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor)

        manager = TellerOnlineRepositorySyncStateManager(sharedPreferences)
    }

    @Test
    fun isDataTooOld_neverFetchedDataBefore_returnTrue() {
        `when`(sharedPreferences.getLong(defaultTag, TellerOnlineRepositorySyncStateManager.INVALID_LAST_FETCHED_VALUE)).thenReturn(TellerOnlineRepositorySyncStateManager.INVALID_LAST_FETCHED_VALUE)

        assertThat(manager.isDataTooOld(defaultTag, AgeOfData(1, AgeOfData.Unit.HOURS))).isTrue()
    }

    @Test
    fun isDataTooOld_lastFetchedTimeNotTooOld_returnFalse() {
        val lastFetchedOneDayAgo: Date = Calendar.getInstance().apply {
            add(Calendar.DATE, -1)
            add(Calendar.MINUTE, 1) // make the last fetched time not too old yet.
        }.time

        `when`(sharedPreferences.getLong(defaultTag, TellerOnlineRepositorySyncStateManager.INVALID_LAST_FETCHED_VALUE)).thenReturn(lastFetchedOneDayAgo.time)

        assertThat(manager.isDataTooOld(defaultTag, AgeOfData(1, AgeOfData.Unit.DAYS))).isFalse()
    }

    @Test
    fun isDataTooOld_lastFetchedTimeTooOld_returnTrue() {
        val lastFetchedOneDayAgo: Date = Calendar.getInstance().apply {
            add(Calendar.DATE, -1)
            add(Calendar.MINUTE, -1) // make the last fetched time too old.
        }.time

        `when`(sharedPreferences.getLong(defaultTag, TellerOnlineRepositorySyncStateManager.INVALID_LAST_FETCHED_VALUE)).thenReturn(lastFetchedOneDayAgo.time)

        assertThat(manager.isDataTooOld(defaultTag, AgeOfData(1, AgeOfData.Unit.DAYS))).isTrue()
    }

    @Test
    fun updateAgeOfData_newDateGetsSaved() {
        val lastFetchedOneDayAgo: Date = Calendar.getInstance().apply {
            add(Calendar.DATE, -1)
        }.time

        `when`(sharedPreferencesEditor.putLong(anyString(), anyLong())).thenReturn(sharedPreferencesEditor)

        manager.updateAgeOfData(defaultTag, lastFetchedOneDayAgo)

        verify(sharedPreferences).edit()
        verify(sharedPreferencesEditor).putLong(defaultTag, lastFetchedOneDayAgo.time)
        verify(sharedPreferencesEditor).commit()
    }

    @Test
    fun hasEverFetchedData_neverFetchedDataBefore_returnFalse() {
        `when`(sharedPreferences.getLong(defaultTag, TellerOnlineRepositorySyncStateManager.INVALID_LAST_FETCHED_VALUE)).thenReturn(TellerOnlineRepositorySyncStateManager.INVALID_LAST_FETCHED_VALUE)

        assertThat(manager.hasEverFetchedData(defaultTag)).isFalse()
    }

    @Test
    fun hasEverFetchedData_hasFetchedBefore_returnTrue() {
        `when`(sharedPreferences.getLong(defaultTag, TellerOnlineRepositorySyncStateManager.INVALID_LAST_FETCHED_VALUE)).thenReturn(Date().time)

        assertThat(manager.hasEverFetchedData(defaultTag)).isTrue()
    }

}