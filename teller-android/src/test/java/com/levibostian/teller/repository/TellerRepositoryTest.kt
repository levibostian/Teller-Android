package com.levibostian.teller.repository

import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.Teller
import com.levibostian.teller.error.TellerLimitedFunctionalityException
import com.levibostian.teller.extensions.awaitComplete
import com.levibostian.teller.extensions.plusAssign
import com.levibostian.teller.provider.SchedulersProvider
import com.levibostian.teller.repository.manager.RepositoryCacheAgeManager
import com.levibostian.teller.repository.manager.RepositoryRefreshManager
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
class TellerRepositoryTest {

    private lateinit var repository: TellerRepositoryStub
    private lateinit var requirements: TellerRepositoryStub.GetRequirements

    private lateinit var compositeDisposable: CompositeDisposable

    @Mock private lateinit var cacheAgeManager: RepositoryCacheAgeManager
    @Mock private lateinit var refreshManager: RepositoryRefreshManager
    @Mock private lateinit var schedulersProvider: SchedulersProvider
    @Mock private lateinit var taskExecutor: TaskExecutor
    @Mock private lateinit var refreshManagerListener: RepositoryRefreshManager.Listener
    @Mock private lateinit var sharedPreferences: SharedPreferences
    @Mock private lateinit var sharedPreferencesEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        whenever(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor)
        whenever(sharedPreferencesEditor.putString(any(), anyOrNull())).thenReturn(sharedPreferencesEditor)

        compositeDisposable = CompositeDisposable()
        schedulersProvider.apply {
            `when`(io()).thenReturn(Schedulers.trampoline())
            `when`(main()).thenReturn(Schedulers.trampoline())
        }
        `when`(taskExecutor.postUI(any())).thenAnswer {
            (it.getArgument(0) as () -> Unit).invoke()
        }
        requirements = TellerRepositoryStub.GetRequirements()
    }

    private fun initRepository(limitedFunctionality: Boolean = false) {
        val teller = if (limitedFunctionality) Teller.getUnitTestingInstance() else Teller.getTestingInstance(sharedPreferences)

        repository = TellerRepositoryStub(sharedPreferences, cacheAgeManager, refreshManager, schedulersProvider, taskExecutor, refreshManagerListener, teller)
    }

    @After
    fun teardown() {
        compositeDisposable.clear()
    }

    @Test
    fun refresh_requirementsNotSet_expectThrowException() {
        initRepository()

        assertFailsWith(IllegalStateException::class) {
            repository.refresh(false)
                    .subscribe()
        }
    }

    @Test
    fun refresh_hasNeverFetched_expectRefreshCall() {
        initRepository()

        val refreshResult = Single.just(TellerRepository.RefreshResult.success())
        `when`(cacheAgeManager.hasSuccessfullyFetchedCache(requirements.tag)).thenReturn(false)
        `when`(refreshManager.refresh(repository.fetchFreshCache_return, requirements.tag, refreshManagerListener)).thenReturn(refreshResult)

        repository.requirements = requirements

        compositeDisposable += repository.refresh(false)
                .test()
                .awaitComplete()
                .assertValue(check {
                    assertThat(it).isEqualTo(TellerRepository.RefreshResult.success())
                })
    }

    @Test
    fun refresh_fetchedCacheTooOld_expectRefreshCall() {
        initRepository()

        val refreshResult = Single.just(TellerRepository.RefreshResult.success())
        `when`(cacheAgeManager.hasSuccessfullyFetchedCache(requirements.tag)).thenReturn(true)
        `when`(cacheAgeManager.lastSuccessfulFetch(requirements.tag)).thenReturn(Date())
        `when`(cacheAgeManager.isCacheTooOld(requirements.tag, repository.maxAgeOfCache)).thenReturn(true)
        `when`(refreshManager.refresh(repository.fetchFreshCache_return, requirements.tag, refreshManagerListener)).thenReturn(refreshResult)

        repository.requirements = requirements

        compositeDisposable += repository.refresh(false)
                .test()
                .awaitComplete()
                .assertValue(check {
                    assertThat(it).isEqualTo(TellerRepository.RefreshResult.success())
                })
    }

    @Test
    fun refresh_fetchedCacheNotTooOldForceRefresh_expectRefreshCall() {
        initRepository()

        val refreshResult = Single.just(TellerRepository.RefreshResult.success())
        `when`(cacheAgeManager.hasSuccessfullyFetchedCache(requirements.tag)).thenReturn(true)
        `when`(cacheAgeManager.lastSuccessfulFetch(requirements.tag)).thenReturn(Date())
        // `when`(cacheAgeManager.isCacheTooOld(requirements.tag, repository.maxAgeOfCache)).thenReturn(false)
        `when`(refreshManager.refresh(repository.fetchFreshCache_return, requirements.tag, refreshManagerListener)).thenReturn(refreshResult)

        repository.requirements = requirements

        compositeDisposable += repository.refresh(true)
                .test()
                .awaitComplete()
                .assertValue(check {
                    assertThat(it).isEqualTo(TellerRepository.RefreshResult.success())
                })
    }

    @Test
    fun refresh_fetchedCacheNotTooOldNoForceRefresh_expectSkipRefreshCall() {
        initRepository()

        `when`(cacheAgeManager.hasSuccessfullyFetchedCache(requirements.tag)).thenReturn(true)
        `when`(cacheAgeManager.lastSuccessfulFetch(requirements.tag)).thenReturn(Date())
        `when`(cacheAgeManager.isCacheTooOld(requirements.tag, repository.maxAgeOfCache)).thenReturn(false)

        repository.requirements = requirements

        compositeDisposable += repository.refresh(false)
                .test()
                .awaitComplete()
                .assertValue(check {
                    assertThat(it.didSkip()).isTrue()
                    assertThat(it.skipped).isEqualTo(TellerRepository.RefreshResult.SkippedReason.CACHE_NOT_TOO_OLD)
                })
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
    fun refresh_throwExceptionWhenLimitedFunctionality() {
        initRepository(limitedFunctionality = true)

        assertFailsWith(TellerLimitedFunctionalityException::class) {
            repository.refresh(false)
        }
    }



}