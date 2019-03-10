package com.levibostian.tellerexample.repository

import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.levibostian.tellerexample.rule.TellerUnitTestRule
import com.levibostian.tellerexample.service.provider.SchedulersProvider
import com.levibostian.tellerexample.wrapper.RxSharedPrefsWrapper
import com.nhaarman.mockito_kotlin.*
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class GitHubUsernameRepositoryTest {

    @Mock private lateinit var rxSharedPrefsWrapper: RxSharedPrefsWrapper
    @Mock private lateinit var sharedPreferences: SharedPreferences
    @Mock private lateinit var sharedPrefsEditor: SharedPreferences.Editor
    @Mock private lateinit var schedulersProvider: SchedulersProvider

    @get:Rule val rule = InstantTaskExecutorRule()
    @get:Rule val tellerRule = TellerUnitTestRule()

    private val defaultRequirements = GitHubUsernameRepository.GitHubUsernameGetCacheRequirements()

    private lateinit var repository: GitHubUsernameRepository

    @Before
    fun setUp() {
        whenever(sharedPreferences.edit()).thenReturn(sharedPrefsEditor)
        whenever(sharedPrefsEditor.putString(eq(GitHubUsernameRepository.GITHUB_USERNAME_SHARED_PREFS_KEY), anyOrNull())).thenReturn(sharedPrefsEditor)
        whenever(schedulersProvider.io()).thenReturn(Schedulers.trampoline())

        repository = GitHubUsernameRepository(rxSharedPrefsWrapper, sharedPreferences, schedulersProvider)
    }

    @Test
    fun currentUsernameSaved_expectGetFromSharedPrefs() {
        val currentUsername = "currentUsername"

        whenever(sharedPreferences.getString(GitHubUsernameRepository.GITHUB_USERNAME_SHARED_PREFS_KEY, null)).thenReturn(currentUsername)

        assertThat(repository.currentUsernameSaved).isEqualTo(currentUsername)
    }

    @Test
    fun saveCache_expectSave() {
        val newCache = "newCache"

        repository.saveCache(newCache, defaultRequirements)

        verify(sharedPrefsEditor).putString(GitHubUsernameRepository.GITHUB_USERNAME_SHARED_PREFS_KEY, newCache)
    }

    @Test
    fun observeCache_expectQueryOnBackgroundThread() {
        whenever(rxSharedPrefsWrapper.observeString(GitHubUsernameRepository.GITHUB_USERNAME_SHARED_PREFS_KEY, GitHubUsernameRepository.GITHUB_USERNAME_SHARED_PREFS_DEFAULT)).thenReturn(Observable.just(""))

        repository.observeCache(defaultRequirements)
                .test()
                .dispose()

        verify(schedulersProvider).io()
        verify(schedulersProvider, never()).mainThread()
    }

    @Test
    fun isCacheEmpty_expectEmpty() {
        assertThat(repository.isCacheEmpty(GitHubUsernameRepository.GITHUB_USERNAME_SHARED_PREFS_DEFAULT, defaultRequirements)).isTrue()
    }

    @Test
    fun isCacheEmpty_expectNotEmpty() {
        assertThat(repository.isCacheEmpty("a", defaultRequirements)).isFalse()
    }

}