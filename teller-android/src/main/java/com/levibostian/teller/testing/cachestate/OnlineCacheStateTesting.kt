package com.levibostian.teller.testing.cachestate

import com.levibostian.teller.cachestate.OnlineCacheState
import com.levibostian.teller.cachestate.online.statemachine.OnlineCacheStateStateMachine
import com.levibostian.teller.repository.OnlineRepository
import java.util.*

/**
 * Convenient utility to generate instances of [OnlineCacheState] used for testing purposes.
 *
 * Intentions of [OnlineCacheStateTesting]:
 * 1. Be able to initialize an instance of [OnlineCacheState] with 1 line of code.
 * 2. Immutable. Represent a snapshot of [OnlineCacheState] without the ability to edit it.
 */
class OnlineCacheStateTesting {

    companion object {
        fun <CACHE: Any> none(): OnlineCacheState<CACHE> {
            return OnlineCacheState.none()
        }

        fun <CACHE: Any> noCache(requirements: OnlineRepository.GetCacheRequirements,
                                 more: (NoCacheExists.() -> Unit)? = null): OnlineCacheState<CACHE> {
            val noCacheExists = NoCacheExists()
            more?.let { noCacheExists.it() }

            /**
             * We are using the [OnlineCacheStateStateMachine] here to (1) prevent duplicate constructor code that is a pain to maintain and (2)
             */
            var stateMachine = OnlineCacheStateStateMachine.noCacheExists<CACHE>(requirements)

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

        fun <CACHE: Any> cache(requirements: OnlineRepository.GetCacheRequirements,
                               lastTimeFetched: Date,
                               more: (CacheExists<CACHE>.() -> Unit)? = null): OnlineCacheState<CACHE> {
            val cacheExists = CacheExists<CACHE>(lastTimeFetched)
            more?.let { cacheExists.it() }

            var stateMachine = OnlineCacheStateStateMachine.cacheExists<CACHE>(requirements, lastTimeFetched)

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

    @OnlineCacheStateEntryPoint
    class NoCacheExists {
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

    @OnlineCacheStateEntryPoint
    class CacheExists<CACHE: Any>(lastFetched: Date) {
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

        data class Props<CACHE: Any>(val cache: CACHE? = null,
                                     val fetching: Boolean = false,
                                     val errorDuringFetch: Throwable? = null,
                                     val successfulFetch: Boolean = false,
                                     val timeFetched: Date? = null)
    }

}