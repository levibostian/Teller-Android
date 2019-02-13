package com.levibostian.teller.repository

import com.levibostian.teller.provider.SchedulersProvider
import com.levibostian.teller.repository.manager.OnlineRepositoryRefreshManager
import com.levibostian.teller.repository.manager.OnlineRepositorySyncStateManager
import com.levibostian.teller.type.AgeOfData
import io.reactivex.Observable
import io.reactivex.Single

internal class OnlineRepositoryStub(syncStateManager: OnlineRepositorySyncStateManager,
                                    refreshManager: OnlineRepositoryRefreshManager,
                                    schedulersProvider: SchedulersProvider): OnlineRepository<String, OnlineRepositoryStub.GetRequirements, String>(syncStateManager, refreshManager, schedulersProvider) {

    override var maxAgeOfData: AgeOfData = AgeOfData(1, AgeOfData.Unit.HOURS)

    var fetchFreshData_return = Single.never<FetchResponse<String>>()
    override fun fetchFreshData(requirements: GetRequirements): Single<FetchResponse<String>> = fetchFreshData_return

    var saveData_results = arrayListOf<String>()
    var saveData_invoke: ((String) -> Unit)? = null
    override fun saveData(data: String, requirements: GetRequirements) {
        saveData_invoke?.invoke(data)
        saveData_results.add(data)
    }

    var observeCachedData_return = Observable.never<String>()
    override fun observeCachedData(requirements: GetRequirements): Observable<String> = observeCachedData_return

    var isDataEmpty_return = false
    override fun isDataEmpty(cache: String, requirements: GetRequirements): Boolean = isDataEmpty_return

    data class GetRequirements(val param: String = ""): OnlineRepository.GetDataRequirements {
        override var tag: GetDataRequirementsTag = "Stub param:$param"
    }

}