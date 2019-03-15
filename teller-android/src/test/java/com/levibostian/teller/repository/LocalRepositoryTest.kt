package com.levibostian.teller.repository

import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.Teller
import com.levibostian.teller.error.TellerLimitedFunctionalityException
import com.levibostian.teller.extensions.plusAssign
import com.levibostian.teller.provider.SchedulersProvider
import com.levibostian.teller.repository.manager.OnlineRepositoryCacheAgeManager
import com.levibostian.teller.repository.manager.OnlineRepositoryRefreshManager
import com.levibostian.teller.util.AssertionUtil.Companion.check
import com.levibostian.teller.util.TaskExecutor
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.util.*
import kotlin.test.assertFailsWith

@RunWith(MockitoJUnitRunner::class)
class LocalRepositoryTest {

    private lateinit var repository: LocalRepositoryStub
    private lateinit var requirements: LocalRepositoryStub.GetRequirements

    @Mock private lateinit var schedulersProvider: SchedulersProvider
    @Mock private lateinit var taskExecutor: TaskExecutor
    @Mock private lateinit var sharedPreferences: SharedPreferences
    @Mock private lateinit var sharedPreferencesEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        whenever(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor)
        whenever(sharedPreferencesEditor.putString(any(), anyOrNull())).thenReturn(sharedPreferencesEditor)

        requirements = LocalRepositoryStub.GetRequirements()
    }

    private fun initRepository(limitedFunctionality: Boolean = false) {
        val teller = if (limitedFunctionality) Teller.getUnitTestingInstance() else Teller.getTestingInstance(sharedPreferences)

        repository = LocalRepositoryStub(sharedPreferences, schedulersProvider, taskExecutor, teller)
    }

    @Test
    fun setRequirements_throwExceptionWhenLimitedFunctionality() {
        initRepository(limitedFunctionality = true)

        assertFailsWith(TellerLimitedFunctionalityException::class) {
            repository.requirements = requirements
        }
    }

    @Test
    fun observe_throwExceptionWhenLimitedFunctionality() {
        initRepository(limitedFunctionality = true)

        assertFailsWith(TellerLimitedFunctionalityException::class) {
            repository.observe()
        }
    }

    @Test
    fun dispose_throwExceptionWhenLimitedFunctionality() {
        initRepository(limitedFunctionality = true)

        assertFailsWith(TellerLimitedFunctionalityException::class) {
            repository.dispose()
        }
    }

    @Test
    fun newCache_throwExceptionWhenLimitedFunctionality() {
        initRepository(limitedFunctionality = true)

        assertFailsWith(TellerLimitedFunctionalityException::class) {
            repository.newCache("", requirements)
        }
    }

}