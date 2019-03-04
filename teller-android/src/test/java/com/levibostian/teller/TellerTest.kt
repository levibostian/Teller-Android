package com.levibostian.teller

import android.content.Context
import android.content.SharedPreferences
import com.levibostian.teller.util.ConstantsUtil
import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class TellerTest {

    @Mock private lateinit var context: Context
    @Mock private lateinit var sharedPreferences: SharedPreferences
    @Mock private lateinit var sharedPreferencesEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        `when`(context.getSharedPreferences(ConstantsUtil.NAMESPACE, Context.MODE_PRIVATE)).thenReturn(sharedPreferences)
        `when`(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor)
    }

    @Test
    fun clear_deletesAllData() {
        `when`(sharedPreferencesEditor.clear()).thenReturn(sharedPreferencesEditor)

        Teller(context).clear()

        verify(sharedPreferencesEditor).clear()
        verify(sharedPreferencesEditor).commit()
    }

}