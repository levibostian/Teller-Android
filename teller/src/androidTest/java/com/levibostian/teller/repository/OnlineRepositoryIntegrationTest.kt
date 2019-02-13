package com.levibostian.teller.repository

import android.content.SharedPreferences
import android.preference.PreferenceManager

import org.junit.Test
import org.junit.runner.RunWith

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.provider.SchedulersProvider
import com.levibostian.teller.provider.TellerSchedulersProvider
import com.levibostian.teller.repository.GetDataRequirementsTag
import com.levibostian.teller.repository.manager.OnlineRepositoryRefreshManager
import com.levibostian.teller.repository.manager.OnlineRepositorySyncStateManager
import com.levibostian.teller.repository.manager.TellerOnlineRepositorySyncStateManager

import org.junit.Before
import org.mockito.Mock
import java.util.*

@RunWith(AndroidJUnit4::class)
class OnlineRepositoryIntegrationTest {

    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext())

    private lateinit var repository: OnlineRepositoryStub

    private val syncStateManager = TellerOnlineRepositorySyncStateManager()
    @Mock private lateinit var refreshManager: OnlineRepositoryRefreshManager
    private val schedulersProvider = TellerSchedulersProvider()

    @Before
    fun setup() {
        sharedPreferences.edit().clear().commit()
        repository = OnlineRepositoryStub(syncStateManager, refreshManager, schedulersProvider)
    }

    @Test
    fun lastTimeFetchedData_null_clearedManagerData() {
        assertThat(repository.maxAgeOfData).isEqualTo(repository.maxAgeOfData)
    }

}
