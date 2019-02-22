package com.levibostian.teller.repository

import com.levibostian.teller.provider.SchedulersProvider
import com.levibostian.teller.repository.manager.OnlineRepositoryRefreshManager
import com.levibostian.teller.repository.manager.OnlineRepositoryCacheAgeManager
import com.levibostian.teller.type.Age
import com.levibostian.teller.util.TaskExecutor
import io.reactivex.Observable
import io.reactivex.Single

internal class OnlineRepositoryStub(cacheAgeManager: OnlineRepositoryCacheAgeManager,
                                    refreshManager: OnlineRepositoryRefreshManager,
                                    schedulersProvider: SchedulersProvider,
                                    taskExecutor: TaskExecutor,
                                    refreshManagerListener: OnlineRepositoryRefreshManager.Listener): OnlineRepository<String, OnlineRepositoryStub.GetRequirements, String>(cacheAgeManager, refreshManager, schedulersProvider, taskExecutor, refreshManagerListener) {

    override var maxAgeOfCache: Age = Age(1, Age.Unit.HOURS)

    var fetchFreshCache_return = Single.never<FetchResponse<String>>()
    var fetchFreshCache_invoke: ((GetRequirements) -> Unit)? = null
    override fun fetchFreshCache(requirements: GetRequirements): Single<FetchResponse<String>> {
        fetchFreshCache_invoke?.invoke(requirements)
        return fetchFreshCache_return
    }

    var saveCache_results = arrayListOf<String>()
    var saveCache_invoke: ((String) -> Unit)? = null
    var saveCache_count = 0
        get() = saveCache_results.size
        private set
    override fun saveCache(cache: String, requirements: GetRequirements) {
        saveCache_results.add(cache)
        saveCache_invoke?.invoke(cache)
    }

    var observeCache_results = arrayListOf<GetRequirements>()
    var observeCache_return = Observable.never<String>()
    var observeCache_invoke: ((GetRequirements) -> Unit)? = null
    var observeCache_count = 0
        get() = observeCache_results.size
        private set
    override fun observeCache(requirements: GetRequirements): Observable<String> {
        observeCache_results.add(requirements)
        observeCache_invoke?.invoke(requirements)
        return observeCache_return
    }

    var isCacheEmpty_return = false
    override fun isCacheEmpty(cache: String, requirements: GetRequirements): Boolean = isCacheEmpty_return

    data class GetRequirements(val param: String = ""): OnlineRepository.GetCacheRequirements {
        override var tag: GetCacheRequirementsTag = "Stub param:$param"
    }

}