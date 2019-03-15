package com.levibostian.teller.cachestate

import com.levibostian.teller.repository.LocalRepository
import com.levibostian.teller.repository.LocalRepositoryCache
import com.levibostian.teller.subject.LocalCacheStateBehaviorSubject
import com.levibostian.teller.testing.cachestate.LocalCacheStateTesting

/**
 * Local response (response obtained from the user or device, no network fetch call) in apps are in 1 of 2 different types of state:
 *
 * 1. It is empty.
 * 2. It is not empty.
 *
 * This class takes in a type of [cache] to keep state on via generic [CACHE] and it maintains the state of that [cache]. It gives you a snapshot of the state of your local response at any given time.
 *
 * This class is used in companion with [LocalRepository] and [LocalCacheStateBehaviorSubject] to maintain the state of [cache] to deliver to someone observing.
 */
data class LocalCacheState<CACHE: LocalRepositoryCache> internal constructor(
        /**
         * Local cache, if it's not empty.
         */
        val cache: CACHE?,
        /**
         * The [LocalRepository.GetCacheRequirements] set to load this cache state.
         */
        val requirements: LocalRepository.GetCacheRequirements?) {

    /**
     * Used for testing purposes to create instances of [LocalCacheState].
     *
     * @see LocalCacheStateTesting
     */
    object Testing

    internal companion object {
        /**
         * This constructor is meant to be more of a placeholder. It's having "no state".
         *
         * Similar to [OnlineCacheState.none].
         *
         * @see OnlineCacheState.none
         */
        fun <CACHE: LocalRepositoryCache> none(): LocalCacheState<CACHE> {
            return LocalCacheState(
                    cache = null,
                    requirements = null)
        }

        /**
         * Unique from [OnlineCacheState] where a state machine replaces this. There is no reason to use a state machine at this time for [LocalCacheState] as there is never any traversal of nodes (cache data changing state). You either start here, or start in another initial state.
         */
        fun <CACHE: LocalRepositoryCache> isEmpty(requirements: LocalRepository.GetCacheRequirements): LocalCacheState<CACHE> {
            return LocalCacheState(
                    cache = null,
                    requirements = requirements)
        }

        /**
         * Unique from [OnlineCacheState] where a state machine replaces this. There is no reason to use a state machine at this time for [LocalCacheState] as there is never any traversal of nodes (cache data changing state). You either start here, or start in another initial state.
         */
        fun <CACHE: LocalRepositoryCache> cache(requirements: LocalRepository.GetCacheRequirements,
                                                cache: CACHE): LocalCacheState<CACHE> {
            return LocalCacheState(
                    cache = cache,
                    requirements = requirements)
        }

    }

}
