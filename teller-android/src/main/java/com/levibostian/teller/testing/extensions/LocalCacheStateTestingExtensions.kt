package com.levibostian.teller.testing.extensions

import com.levibostian.teller.cachestate.LocalCacheState
import com.levibostian.teller.cachestate.OnlineCacheState
import com.levibostian.teller.repository.LocalRepository
import com.levibostian.teller.repository.LocalRepositoryCache
import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.teller.testing.cachestate.LocalCacheStateTesting
import com.levibostian.teller.testing.cachestate.OnlineCacheStateTesting
import java.util.*

fun <CACHE: LocalRepositoryCache> LocalCacheState.Testing.none(): LocalCacheState<CACHE> = LocalCacheStateTesting.none()

fun <CACHE: LocalRepositoryCache> LocalCacheState.Testing.isEmpty(requirements: LocalRepository.GetCacheRequirements): LocalCacheState<CACHE> = LocalCacheStateTesting.isEmpty(requirements)

fun <CACHE: LocalRepositoryCache> LocalCacheState.Testing.cache(requirements: LocalRepository.GetCacheRequirements,
                                               cache: CACHE): LocalCacheState<CACHE> = LocalCacheStateTesting.cache(requirements, cache)