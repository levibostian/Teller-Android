package com.levibostian.teller.testing.extensions

import com.levibostian.teller.cachestate.OnlineCacheState
import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.teller.repository.OnlineRepositoryCache
import com.levibostian.teller.repository.OnlineRepositoryFetchResponse
import com.levibostian.teller.testing.cachestate.OnlineCacheStateTesting
import com.levibostian.teller.testing.repository.OnlineRepositoryTesting
import java.util.*

fun <CACHE: OnlineRepositoryCache, REQ: OnlineRepository.GetCacheRequirements, FETCH_RESPONSE: OnlineRepositoryFetchResponse> OnlineRepository.Testing.initState(
        repository: OnlineRepository<CACHE, REQ, FETCH_RESPONSE>,
        requirements: REQ,
        more: (OnlineRepositoryTesting.StateOfOnlineRepositoryDsl<FETCH_RESPONSE>.() -> Unit)? = null): OnlineRepositoryTesting.SetValues {
    return OnlineRepositoryTesting.initState(repository, requirements, more)
}

fun <FETCH_RESPONSE: OnlineRepositoryFetchResponse, REPO: OnlineRepository<*, OnlineRepository.GetCacheRequirements, FETCH_RESPONSE>> OnlineRepository.Testing.initStateAsync(
        repository: REPO,
        requirements: OnlineRepository.GetCacheRequirements,
        more: (OnlineRepositoryTesting.StateOfOnlineRepositoryDsl<FETCH_RESPONSE>.() -> Unit)? = null,
        complete: (OnlineRepositoryTesting.SetValues) -> Unit) {
    OnlineRepositoryTesting.initStateAsync(repository, requirements, more, complete)
}