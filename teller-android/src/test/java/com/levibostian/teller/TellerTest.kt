package com.levibostian.teller

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.error.TellerLimitedFunctionalityException
import com.levibostian.teller.util.ConstantsUtil
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertFailsWith

@RunWith(MockitoJUnitRunner::class)
class TellerTest {

    @Mock private lateinit var sharedPreferences: SharedPreferences
    @Mock private lateinit var sharedPreferencesEditor: SharedPreferences.Editor
    @Mock private lateinit var application: Application

    @Before
    fun setup() {
        `when`(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor)
        `when`(sharedPreferencesEditor.clear()).thenReturn(sharedPreferencesEditor)
        whenever(application.getSharedPreferences(ConstantsUtil.NAMESPACE, Context.MODE_PRIVATE)).thenReturn(sharedPreferences)
    }

    @Test
    fun clear_deletesAllData() {
        Teller(sharedPreferences, false).clear()

        verify(sharedPreferencesEditor).clear()
        verify(sharedPreferencesEditor).commit()
    }

    @Test
    fun init_setPropertiesCorrectly() {
        Teller.getInstance(application).apply {
            assertThat(sharedPreferences).isEqualTo(sharedPreferences)
            assertThat(unitTesting).isFalse()
        }
    }

    @Test
    fun initTesting_setPropertiesCorrectly() {
        Teller.getTestingInstance(sharedPreferences).apply {
            assertThat(sharedPreferences).isEqualTo(sharedPreferences)
            assertThat(unitTesting).isFalse()
        }
    }

    @Test
    fun initUnitTesting_setPropertiesCorrectly() {
        Teller.getUnitTestingInstance().apply {
            assertThat(sharedPreferences).isNull()
            assertThat(unitTesting).isTrue()
        }
    }

    @Test
    fun clear_throwExceptionIfInitUnitTesting() {
        assertFailsWith(TellerLimitedFunctionalityException::class) {
            Teller.getUnitTestingInstance().clear()
        }
    }

}