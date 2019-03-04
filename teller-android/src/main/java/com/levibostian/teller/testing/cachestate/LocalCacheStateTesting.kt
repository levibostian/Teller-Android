package com.levibostian.teller.testing.cachestate

import com.levibostian.teller.cachestate.LocalCacheState
import com.levibostian.teller.cachestate.OnlineCacheState
import com.levibostian.teller.cachestate.online.statemachine.OnlineCacheStateStateMachine
import com.levibostian.teller.repository.LocalRepository
import com.levibostian.teller.repository.OnlineRepository
import java.util.*

/**
 * Convenient utility to generate instances of [LocalCacheState] used for testing purposes.
 *
 * You can use this class directly, or, use the recommended extension functions in the [LocalCacheState.Testing] object.
 *
 * Intentions of [LocalCacheStateTesting]:
 * 1. Be able to initialize an instance of [LocalCacheState] with 1 line of code.
 * 2. Immutable. Represent a snapshot of [LocalCacheState] without the ability to edit it.
 */
class LocalCacheStateTesting private constructor() {

    companion object {
        fun <CACHE: Any> none(): LocalCacheState<CACHE> {
            return LocalCacheState.none()
        }

        fun <CACHE: Any> isEmpty(requirements: LocalRepository.GetCacheRequirements): LocalCacheState<CACHE> {
            return LocalCacheState.isEmpty(requirements)
        }

        fun <CACHE: Any> cache(requirements: LocalRepository.GetCacheRequirements,
                               cache: CACHE): LocalCacheState<CACHE> {
            return LocalCacheState.cache(requirements, cache)
        }
    }

}