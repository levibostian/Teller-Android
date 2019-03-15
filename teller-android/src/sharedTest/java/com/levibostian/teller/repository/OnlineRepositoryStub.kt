package com.levibostian.teller.repository

import android.content.SharedPreferences
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.levibostian.teller.Teller
import com.levibostian.teller.provider.SchedulersProvider
import com.levibostian.teller.repository.manager.OnlineRepositoryRefreshManager
import com.levibostian.teller.repository.manager.OnlineRepositoryCacheAgeManager
import com.levibostian.teller.type.Age
import com.levibostian.teller.util.TaskExecutor
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Stub of [OnlineRepository] used for integration testing purposes.
 */
internal class OnlineRepositoryStub(private val sharedPreferences: SharedPreferences,
                                    cacheAgeManager: OnlineRepositoryCacheAgeManager,
                                    refreshManager: OnlineRepositoryRefreshManager,
                                    schedulersProvider: SchedulersProvider,
                                    taskExecutor: TaskExecutor,
                                    refreshManagerListener: OnlineRepositoryRefreshManager.Listener,
                                    teller: Teller): OnlineRepository<String, OnlineRepositoryStub.GetRequirements, String>(schedulersProvider, cacheAgeManager, refreshManager, taskExecutor, refreshManagerListener, teller) {

    private val rxSharedPreferences = RxSharedPreferences.create(sharedPreferences)

    var currentObserveCache_observable: Observable<String>? = null

    init {
        sharedPreferences.edit().putString(CACHE_KEY, null).commit()
    }

    companion object {
        private const val CACHE_KEY = "CACHE_KEY.OnlineRepositoryStub"
    }

    override var maxAgeOfCache: Age = Age(1, Age.Unit.HOURS)

    var fetchFreshCache_return = Single.never<FetchResponse<String>>()
    var fetchFreshCache_invoke: ((GetRequirements) -> Unit)? = null
    override fun fetchFreshCache(requirements: GetRequirements): Single<FetchResponse<String>> {
        fetchFreshCache_invoke?.invoke(requirements)
        return fetchFreshCache_return
    }

    var saveCache_results = arrayListOf<String>()
    var saveCache_invoke: ((String) -> Unit)? = null
    override fun saveCache(cache: String, requirements: GetRequirements) {
        sharedPreferences.edit().putString(CACHE_KEY, cache).commit()

        saveCache_results.add(cache)
        saveCache_invoke?.invoke(cache)
    }

    var observeCache_results = arrayListOf<GetRequirements>()
    var observeCache_invoke: ((GetRequirements) -> Unit)? = null
    override fun observeCache(requirements: GetRequirements): Observable<String> {
        currentObserveCache_observable = rxSharedPreferences.getString(CACHE_KEY, "").asObservable()

        observeCache_results.add(requirements)
        observeCache_invoke?.invoke(requirements)

        return currentObserveCache_observable!!
    }

    override fun isCacheEmpty(cache: String, requirements: GetRequirements): Boolean = cache.isEmpty()

    data class GetRequirements(val param: String = ""): OnlineRepository.GetCacheRequirements {
        override var tag: GetCacheRequirementsTag = "Stub param:$param"
    }

}