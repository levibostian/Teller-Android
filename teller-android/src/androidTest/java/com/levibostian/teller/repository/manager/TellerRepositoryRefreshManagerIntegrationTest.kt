package com.levibostian.teller.repository.manager


import androidx.test.annotation.UiThreadTest

import org.junit.Test
import org.junit.runner.RunWith

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.repository.GetCacheRequirementsTag
import com.levibostian.teller.repository.TellerRepository
import com.levibostian.teller.rule.MockitoInitRule
import com.levibostian.teller.util.TestUtil.isOnMainThread
import com.levibostian.teller.util.Wait
import io.reactivex.Single

import org.junit.Rule
import org.mockito.Mock

@RunWith(AndroidJUnit4::class)
class TellerRepositoryRefreshManagerIntegrationTest {

    @Mock private lateinit var repositoryListener: RepositoryRefreshManager.Listener

    private val defaultTag: GetCacheRequirementsTag = "defaultTag"

    @get:Rule val mockitoInitMocks = MockitoInitRule(this)

    @Test
    @UiThreadTest // call on UI thread to assert thread changes.
    fun refreshCalledOnBackgroundThread() {
        val wait = Wait.times(1)

        val refreshTask = Single.create<TellerRepository.FetchResponse<String>> { observer ->
            assertThat(isOnMainThread()).isFalse()
            wait.countDown()
        }

        SharedRepositoryRefreshManager.refresh(refreshTask, defaultTag, repositoryListener)

        wait.await()
    }

}