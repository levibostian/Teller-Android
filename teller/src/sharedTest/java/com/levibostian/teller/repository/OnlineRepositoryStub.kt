package com.levibostian.teller.repository

import com.levibostian.teller.provider.SchedulersProvider
import com.levibostian.teller.repository.manager.OnlineRepositoryRefreshManager
import com.levibostian.teller.repository.manager.OnlineRepositorySyncStateManager
import com.levibostian.teller.type.AgeOfData
import com.levibostian.teller.util.TaskExecutor
import io.reactivex.Observable
import io.reactivex.Single

internal class OnlineRepositoryStub(syncStateManager: OnlineRepositorySyncStateManager,
                                    refreshManager: OnlineRepositoryRefreshManager,
                                    schedulersProvider: SchedulersProvider,
                                    taskExecutor: TaskExecutor): OnlineRepository<String, OnlineRepositoryStub.GetRequirements, String>(syncStateManager, refreshManager, schedulersProvider, taskExecutor) {

    override var maxAgeOfData: AgeOfData = AgeOfData(1, AgeOfData.Unit.HOURS)

    var fetchFreshData_return = Single.never<FetchResponse<String>>()
    var fetchFreshData_invoke: ((GetRequirements) -> Unit)? = null
    override fun fetchFreshData(requirements: GetRequirements): Single<FetchResponse<String>> {
        fetchFreshData_invoke?.invoke(requirements)
        return fetchFreshData_return
    }

    var saveData_results = arrayListOf<String>()
    var saveData_invoke: ((String) -> Unit)? = null
    var saveData_count = 0
        get() = saveData_results.size
        private set
    override fun saveData(data: String, requirements: GetRequirements) {
        saveData_invoke?.invoke(data)
        saveData_results.add(data)
    }

    var observeCachedData_results = arrayListOf<GetRequirements>()
    var observeCachedData_return = Observable.never<String>()
    var observeCachedData_invoke: ((GetRequirements) -> Unit)? = null
    var observeCachedData_count = 0
        get() = observeCachedData_results.size
        private set
    override fun observeCachedData(requirements: GetRequirements): Observable<String> {
        observeCachedData_results.add(requirements)
        observeCachedData_invoke?.invoke(requirements)
        return observeCachedData_return
    }

    var isDataEmpty_return = false
    override fun isDataEmpty(cache: String, requirements: GetRequirements): Boolean = isDataEmpty_return

    data class GetRequirements(val param: String = ""): OnlineRepository.GetDataRequirements {
        override var tag: GetDataRequirementsTag = "Stub param:$param"
    }

}