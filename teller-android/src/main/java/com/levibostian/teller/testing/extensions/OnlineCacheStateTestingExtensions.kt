package com.levibostian.teller.testing.extensions

import com.levibostian.teller.cachestate.CacheState
import com.levibostian.teller.repository.TellerRepository
import com.levibostian.teller.repository.RepositoryCache
import com.levibostian.teller.testing.cachestate.OnlineCacheStateTesting
import java.util.*

fun <CACHE: RepositoryCache> CacheState.Testing.none(): CacheState<CACHE> = OnlineCacheStateTesting.none()

fun <CACHE: RepositoryCache> CacheState.Testing.noCache(requirements: TellerRepository.GetCacheRequirements,
                                                        more: (OnlineCacheStateTesting.NoCacheExistsDsl.() -> Unit)? = null): CacheState<CACHE> = OnlineCacheStateTesting.noCache(requirements, more)

fun <CACHE: RepositoryCache> CacheState.Testing.cache(requirements: TellerRepository.GetCacheRequirements,
                                                      lastTimeFetched: Date,
                                                      more: (OnlineCacheStateTesting.CacheExistsDsl<CACHE>.() -> Unit)? = null): CacheState<CACHE> = OnlineCacheStateTesting.cache(requirements, lastTimeFetched, more)