package com.levibostian.teller.testing.cachestate

import com.levibostian.teller.cachestate.CacheState
import com.levibostian.teller.cachestate.statemachine.CacheStateStateMachine
import com.levibostian.teller.repository.TellerRepository
import com.levibostian.teller.repository.RepositoryCache
import java.util.*

/**
 * Convenient utility to generate instances of [CacheState] used for testing purposes.
 *
 * You can use this class directly, or, use the recommended extension functions in the [CacheState.Testing] object.
 *
 * Intentions of [OnlineCacheStateTesting]:
 * 1. Be able to initialize an instance of [CacheState] with 1 line of code.
 * 2. Immutable. Represent a snapshot of [CacheState] without the ability to edit it.
 */
class OnlineCacheStateTesting private constructor() {

    companion object {
        fun <CACHE: RepositoryCache> none(): CacheState<CACHE> {
            return CacheState.none()
        }

        fun <CACHE: RepositoryCache> noCache(requirements: TellerRepository.GetCacheRequirements,
                                             more: (NoCacheExistsDsl.() -> Unit)? = null): CacheState<CACHE> {
            val noCacheExists = NoCacheExistsDsl()
            more?.let { noCacheExists.it() }

            /**
             * We are using the [CacheStateStateMachine] here to (1) prevent duplicate constructor code that is a pain to maintain and (2) we are starting with the assumption that no cache exists and editing the state from there if the DSL asks for it.
             */
            var stateMachine = CacheStateStateMachine.noCacheExists<CACHE>(requirements)

            if (noCacheExists.props.fetchingFirstTime) {
                stateMachine = stateMachine.change().firstFetch()
            }

            noCacheExists.props.errorDuringFirstFetch?.let {
                stateMachine = stateMachine.change()
                        .firstFetch().change()
                        .failFirstFetch(it)
            }

            if (noCacheExists.props.successfulFirstFetch) {
                stateMachine = stateMachine.change()
                        .firstFetch().change()
                        .successfulFirstFetch(noCacheExists.props.timeFetched!!)
            }

            return stateMachine
        }

        fun <CACHE: RepositoryCache> cache(requirements: TellerRepository.GetCacheRequirements,
                                           lastTimeFetched: Date,
                                           more: (CacheExistsDsl<CACHE>.() -> Unit)? = null): CacheState<CACHE> {
            val cacheExists = CacheExistsDsl<CACHE>(lastTimeFetched)
            more?.let { cacheExists.it() }

            var stateMachine = CacheStateStateMachine.cacheExists<CACHE>(requirements, lastTimeFetched)

            cacheExists.props.cache?.let {
                stateMachine = stateMachine.change().cache(it)
            } ?: kotlin.run {
                stateMachine = stateMachine.change().cacheEmpty()
            }

            if (cacheExists.props.fetching) stateMachine = stateMachine.change().fetchingFreshCache()

            cacheExists.props.errorDuringFetch?.let {
                stateMachine = stateMachine.change().fetchingFreshCache().change().failRefreshCache(it)
            }

            if (cacheExists.props.successfulFetch) {
                stateMachine = stateMachine.change().fetchingFreshCache()
                        .change().successfulRefreshCache(cacheExists.props.timeFetched!!)
            }

            return stateMachine
        }
    }

    @OnlineCacheStateTestingDsl
    class NoCacheExistsDsl {
        var props = Props()

        fun fetchingFirstTime() {
            props = Props(
                    fetchingFirstTime = true
            )
        }

        fun failedFirstFetch(error: Throwable) {
            props = Props(
                    errorDuringFirstFetch = error
            )
        }

        fun successfulFirstFetch(timeFetched: Date) {
            props = Props(
                    successfulFirstFetch = true,
                    timeFetched = timeFetched)
        }

        data class Props(val fetchingFirstTime: Boolean = false,
                         val errorDuringFirstFetch: Throwable? = null,
                         val successfulFirstFetch: Boolean = false,
                         val timeFetched: Date? = null)
    }

    @OnlineCacheStateTestingDsl
    class CacheExistsDsl<CACHE: RepositoryCache>(lastFetched: Date) {
        var props = Props<CACHE>(
                timeFetched = lastFetched
        )

        fun cache(cache: CACHE) {
            props = Props(
                    cache = cache
            )
        }

        fun fetching() {
            props = Props(
                    cache = props.cache,
                    fetching = true
            )
        }

        fun failedFetch(error: Throwable) {
            props = Props(
                    cache = props.cache,
                    errorDuringFetch = error
            )
        }

        fun successfulFetch(timeFetched: Date) {
            props = Props(
                    cache = props.cache,
                    successfulFetch = true,
                    timeFetched = timeFetched
            )
        }

        data class Props<CACHE: RepositoryCache>(val cache: CACHE? = null,
                                                 val fetching: Boolean = false,
                                                 val errorDuringFetch: Throwable? = null,
                                                 val successfulFetch: Boolean = false,
                                                 val timeFetched: Date? = null)
    }

}