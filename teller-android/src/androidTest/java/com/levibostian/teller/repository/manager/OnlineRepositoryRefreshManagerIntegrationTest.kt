package com.levibostian.teller.repository.manager


import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.test.annotation.UiThreadTest

import org.junit.Test
import org.junit.runner.RunWith

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.repository.GetCacheRequirementsTag
import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.teller.rule.MockitoInitRule
import com.levibostian.teller.util.TestUtil.isOnMainThread
import com.levibostian.teller.util.Wait
import io.reactivex.Single

import org.junit.Before
import org.junit.Rule
import org.mockito.Mock
import java.util.*

@RunWith(AndroidJUnit4::class)
class OnlineRepositoryRefreshManagerIntegrationTest {

    @Mock private lateinit var repositoryListener: OnlineRepositoryRefreshManager.Listener

    private val defaultTag: GetCacheRequirementsTag = "defaultTag"

    @get:Rule val mockitoInitMocks = MockitoInitRule(this)

    @Test
    @UiThreadTest // call on UI thread to assert thread changes.
    fun refreshCalledOnBackgroundThread() {
        val wait = Wait.times(1)

        val refreshTask = Single.create<OnlineRepository.FetchResponse<String>> { observer ->
            assertThat(isOnMainThread()).isFalse()
            wait.countDown()
        }

        SharedOnlineRepositoryRefreshManager.refresh(refreshTask, defaultTag, repositoryListener)

        wait.await()
    }

}