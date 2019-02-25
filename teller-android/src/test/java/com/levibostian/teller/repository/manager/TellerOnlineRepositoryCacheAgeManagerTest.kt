package com.levibostian.teller.repository.manager


import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.type.Age
import com.nhaarman.mockito_kotlin.verify
import org.junit.Test

import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class TellerOnlineRepositoryCacheAgeManagerTest {

    @Mock private lateinit var sharedPreferences: SharedPreferences
    @Mock private lateinit var sharedPreferencesEditor: SharedPreferences.Editor

    private lateinit var manager: TellerOnlineRepositoryCacheAgeManager

    private val defaultTag = "tag"

    @Before
    fun setup() {
        `when`(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor)

        manager = TellerOnlineRepositoryCacheAgeManager(sharedPreferences)
    }

    @Test
    fun isCacheTooOld_neverFetchedCacheBefore_returnTrue() {
        `when`(sharedPreferences.getLong(defaultTag, TellerOnlineRepositoryCacheAgeManager.INVALID_LAST_FETCHED_VALUE)).thenReturn(TellerOnlineRepositoryCacheAgeManager.INVALID_LAST_FETCHED_VALUE)

        assertThat(manager.isCacheTooOld(defaultTag, Age(1, Age.Unit.HOURS))).isTrue()
    }

    @Test
    fun isCacheTooOld_lastFetchedTimeNotTooOld_returnFalse() {
        val lastFetchedOneDayAgo: Date = Calendar.getInstance().apply {
            add(Calendar.DATE, -1)
            add(Calendar.MINUTE, 1) // make the last fetched time not too old yet.
        }.time

        `when`(sharedPreferences.getLong(defaultTag, TellerOnlineRepositoryCacheAgeManager.INVALID_LAST_FETCHED_VALUE)).thenReturn(lastFetchedOneDayAgo.time)

        assertThat(manager.isCacheTooOld(defaultTag, Age(1, Age.Unit.DAYS))).isFalse()
    }

    @Test
    fun isCacheTooOld_lastFetchedTimeTooOld_returnTrue() {
        val lastFetchedOneDayAgo: Date = Calendar.getInstance().apply {
            add(Calendar.DATE, -1)
            add(Calendar.MINUTE, -1) // make the last fetched time too old.
        }.time

        `when`(sharedPreferences.getLong(defaultTag, TellerOnlineRepositoryCacheAgeManager.INVALID_LAST_FETCHED_VALUE)).thenReturn(lastFetchedOneDayAgo.time)

        assertThat(manager.isCacheTooOld(defaultTag, Age(1, Age.Unit.DAYS))).isTrue()
    }

    @Test
    fun updateLastSuccessfulFetch_newDateGetsSaved() {
        val lastFetchedOneDayAgo: Date = Calendar.getInstance().apply {
            add(Calendar.DATE, -1)
        }.time

        `when`(sharedPreferencesEditor.putLong(anyString(), anyLong())).thenReturn(sharedPreferencesEditor)

        manager.updateLastSuccessfulFetch(defaultTag, lastFetchedOneDayAgo)

        verify(sharedPreferences).edit()
        verify(sharedPreferencesEditor).putLong(defaultTag, lastFetchedOneDayAgo.time)
        verify(sharedPreferencesEditor).commit()
    }

    @Test
    fun updateLastSuccessfulFetch_neverFetchedCacheBefore_returnFalse() {
        `when`(sharedPreferences.getLong(defaultTag, TellerOnlineRepositoryCacheAgeManager.INVALID_LAST_FETCHED_VALUE)).thenReturn(TellerOnlineRepositoryCacheAgeManager.INVALID_LAST_FETCHED_VALUE)

        assertThat(manager.hasSuccessfullyFetchedCache(defaultTag)).isFalse()
    }

    @Test
    fun hasSuccessfullyFetchedCache_hasFetchedBefore_returnTrue() {
        `when`(sharedPreferences.getLong(defaultTag, TellerOnlineRepositoryCacheAgeManager.INVALID_LAST_FETCHED_VALUE)).thenReturn(Date().time)

        assertThat(manager.hasSuccessfullyFetchedCache(defaultTag)).isTrue()
    }

}