package com.levibostian.teller.repository

import com.google.common.truth.Truth.assertThat
import com.levibostian.teller.extensions.plusAssign
import com.levibostian.teller.provider.SchedulersProvider
import com.levibostian.teller.repository.manager.OnlineRepositoryRefreshManager
import com.levibostian.teller.repository.manager.OnlineRepositorySyncStateManager
import org.junit.Test
import com.levibostian.teller.util.AssertionUtil.Companion.check

import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import org.junit.Before
import org.mockito.Mockito.`when`
import io.reactivex.schedulers.Schedulers
import org.junit.After
import kotlin.test.assertFailsWith

@RunWith(MockitoJUnitRunner::class)
class LocalRepositoryTest {

    private lateinit var repository: LocalRepositoryStub
    private lateinit var requirements: LocalRepositoryStub.GetRequirements

    private lateinit var compositeDisposable: CompositeDisposable

    @Mock private lateinit var schedulersProvider: SchedulersProvider

    @Before
    fun setup() {
        compositeDisposable = CompositeDisposable()
        schedulersProvider.apply {
            `when`(io()).thenReturn(Schedulers.trampoline())
            `when`(main()).thenReturn(Schedulers.trampoline())
        }
        requirements = LocalRepositoryStub.GetRequirements()
        repository = LocalRepositoryStub(schedulersProvider)
    }

    @After
    fun teardown() {
        compositeDisposable.clear()
    }

}