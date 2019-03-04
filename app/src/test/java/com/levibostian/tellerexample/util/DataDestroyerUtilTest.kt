package com.levibostian.tellerexample.util

import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.Teller
import com.levibostian.teller.cachestate.LocalCacheState
import com.levibostian.teller.cachestate.OnlineCacheState
import com.levibostian.teller.repository.LocalRepository
import com.levibostian.teller.testing.extensions.cache
import com.levibostian.tellerexample.model.RepoModel
import com.levibostian.tellerexample.model.db.AppDatabase
import com.levibostian.tellerexample.repository.GitHubUsernameRepository
import com.levibostian.tellerexample.repository.ReposRepository
import com.levibostian.tellerexample.service.provider.SchedulersProvider
import com.nhaarman.mockito_kotlin.*
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.util.*

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