package com.levibostian.tellerexample.util

import android.content.SharedPreferences
import com.levibostian.teller.Teller
import com.levibostian.teller.cachestate.LocalCacheState
import com.levibostian.teller.repository.LocalRepository
import com.levibostian.tellerexample.model.db.AppDatabase
import com.nhaarman.mockito_kotlin.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class DataDestroyerUtilTest {

    @Mock private lateinit var teller: Teller
    @Mock private lateinit var database: AppDatabase
    @Mock private lateinit var sharedPreferences: SharedPreferences

    private lateinit var dataDestroyerUtil: DataDestroyerUtil

    @Before
    fun setUp() {
        dataDestroyerUtil = DataDestroyerUtil(teller, database, sharedPreferences)
    }

    @Test
    fun `deleteTellerData() - Tells Teller to clear`() {
        dataDestroyerUtil.deleteTellerData()

        verify(teller).clear()
    }

}