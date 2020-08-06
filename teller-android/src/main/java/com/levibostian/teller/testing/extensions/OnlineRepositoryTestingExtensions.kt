package com.levibostian.teller.testing.extensions

import com.levibostian.teller.repository.TellerRepository
import com.levibostian.teller.repository.RepositoryCache
import com.levibostian.teller.repository.DataSourceFetchResponse
import com.levibostian.teller.testing.repository.OnlineRepositoryTesting

fun <CACHE: RepositoryCache, REQ: TellerRepository.GetCacheRequirements, FETCH_RESPONSE: DataSourceFetchResponse> TellerRepository.Testing.initState(
        repository: TellerRepository<CACHE, REQ, FETCH_RESPONSE>,
        requirements: REQ,
        more: (OnlineRepositoryTesting.StateOfOnlineRepositoryDsl<FETCH_RESPONSE>.() -> Unit)? = null): OnlineRepositoryTesting.SetValues {
    return OnlineRepositoryTesting.initState(repository, requirements, more)
}

fun <FETCH_RESPONSE: DataSourceFetchResponse, REPO: TellerRepository<*, TellerRepository.GetCacheRequirements, FETCH_RESPONSE>> TellerRepository.Testing.initStateAsync(
        repository: REPO,
        requirements: TellerRepository.GetCacheRequirements,
        more: (OnlineRepositoryTesting.StateOfOnlineRepositoryDsl<FETCH_RESPONSE>.() -> Unit)? = null,
        complete: (OnlineRepositoryTesting.SetValues) -> Unit) {
    OnlineRepositoryTesting.initStateAsync(repository, requirements, more, complete)
}