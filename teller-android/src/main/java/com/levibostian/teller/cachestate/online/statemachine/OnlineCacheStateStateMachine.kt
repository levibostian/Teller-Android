package com.levibostian.teller.cachestate.online.statemachine

import com.levibostian.teller.cachestate.OnlineCacheState
import com.levibostian.teller.repository.OnlineRepository
import java.util.*


/**
Finite state machine for the state of response that is fetched from an online location.
This file is used internally to keep track and enforce the changing of states that online response can go through.
The file is designed to begin with an empty state at the constructor. Then via a list of functions, change the state of response. If a function throws an error, it is an illegal traversal through the state machine. If you get an OnlineCacheState instance back from a function, you have successfully changed the state of response.

OnlineDataStateMachine is meant to be immutable. It represents that state machine of an instance of OnlineCacheState (which is also immutable).
 */
class OnlineCacheStateStateMachine<CACHE: Any> private constructor(private val requirements: OnlineRepository.GetCacheRequirements,
                                                                            private val noCacheStateMachine: NoCacheStateMachine?,
                                                                            private val cacheExistsStateMachine: CacheStateMachine<CACHE>?,
                                                                            private val fetchingFreshCacheStateMachine: FetchingFreshCacheStateMachine?) {

    internal companion object {
        fun <CACHE: Any> noCacheExists(requirements: OnlineRepository.GetCacheRequirements): OnlineCacheState<CACHE> {
            val onlineDataStateMachine = OnlineCacheStateStateMachine<CACHE>(requirements, NoCacheStateMachine.noCacheExists(), null, null)

            return OnlineCacheState(
                    noCacheExists = true,
                    fetchingForFirstTime = false,
                    cacheData = null,
                    lastTimeFetched = null,
                    isFetchingFreshData = false,
                    requirements = requirements,
                    stateMachine = onlineDataStateMachine,
                    errorDuringFirstFetch = null,
                    justCompletedSuccessfulFirstFetch = false,
                    errorDuringFetch = null,
                    justCompletedSuccessfullyFetchingFreshData = false)
        }

        fun <CACHE: Any> cacheExists(requirements: OnlineRepository.GetCacheRequirements, lastTimeFetched: Date): OnlineCacheState<CACHE> {
            val cacheExistsStateMachine = CacheStateMachine.cacheEmpty<CACHE>() // Empty is a placeholder for now but it indicates that a cache does exist for future calls to the state machine.
            val fetchingFreshCacheStateMachine = FetchingFreshCacheStateMachine.notFetching(lastTimeFetched)
            val onlineDataStateMachine = OnlineCacheStateStateMachine(requirements, null, cacheExistsStateMachine, fetchingFreshCacheStateMachine)

            return OnlineCacheState(
                    noCacheExists = false,
                    fetchingForFirstTime = false,
                    cacheData = null,
                    lastTimeFetched = lastTimeFetched,
                    isFetchingFreshData = false,
                    requirements = requirements,
                    stateMachine = onlineDataStateMachine,
                    errorDuringFirstFetch = null,
                    justCompletedSuccessfulFirstFetch = false,
                    errorDuringFetch = null,
                    justCompletedSuccessfullyFetchingFreshData = false)
        }
    }

    @Throws(NodeNotPossibleError::class)
    fun firstFetch(): OnlineCacheState<CACHE> {
        val noCacheStateMachine = this.noCacheStateMachine ?: throw NodeNotPossibleError(this.toString())

        val onlineDataStateMachine = OnlineCacheStateStateMachine<CACHE>(requirements, noCacheStateMachine.fetching(), null, null)

        return OnlineCacheState(
                noCacheExists = true,
                fetchingForFirstTime = true,
                cacheData = null,
                lastTimeFetched = null,
                isFetchingFreshData = false,
                requirements = requirements,
                stateMachine = onlineDataStateMachine,
                errorDuringFirstFetch = null,
                justCompletedSuccessfulFirstFetch = false,
                errorDuringFetch = null,
                justCompletedSuccessfullyFetchingFreshData = false)
    }

    @Throws(NodeNotPossibleError::class)
    fun failFirstFetch(error: Throwable): OnlineCacheState<CACHE> {
        val noCacheStateMachine = this.noCacheStateMachine 
        
        if (noCacheStateMachine == null || !noCacheStateMachine.isFetching) {
            throw NodeNotPossibleError(this.toString())
        }

        val onlineDataStateMachine = OnlineCacheStateStateMachine<CACHE>(requirements, noCacheStateMachine.failedFetching(error), null, null)
        
        return OnlineCacheState(
                noCacheExists = true,
                fetchingForFirstTime = false,
                cacheData = null,
                lastTimeFetched = null,
                isFetchingFreshData = false,
                requirements = requirements,
                stateMachine = onlineDataStateMachine,
                errorDuringFirstFetch = error,
                justCompletedSuccessfulFirstFetch = false,
                errorDuringFetch = null,
                justCompletedSuccessfullyFetchingFreshData = false)
    }

    @Throws(NodeNotPossibleError::class)
    fun successfulFirstFetch(timeFetched: Date): OnlineCacheState<CACHE> {
        val noCacheStateMachine = this.noCacheStateMachine

        if (noCacheStateMachine == null || !noCacheStateMachine.isFetching) {
            throw NodeNotPossibleError(this.toString())
        }

        val onlineDataStateMachine = OnlineCacheStateStateMachine<CACHE>(requirements,
                null, // Cache now exists, no remove this state machine. We will no longer be able to go back to these nodes. 
                CacheStateMachine.cacheEmpty(), // empty is like a placeholder.
                FetchingFreshCacheStateMachine.notFetching(timeFetched))

        return OnlineCacheState(
                noCacheExists = false,
                fetchingForFirstTime = false,
                cacheData = null,
                lastTimeFetched = timeFetched,
                isFetchingFreshData = false,
                requirements = requirements,
                stateMachine = onlineDataStateMachine,
                errorDuringFirstFetch = null,
                justCompletedSuccessfulFirstFetch = true,
                errorDuringFetch = null,
                justCompletedSuccessfullyFetchingFreshData = false)
    }

    @Throws(NodeNotPossibleError::class)
    fun cacheEmpty(): OnlineCacheState<CACHE> {
        val cacheExistsStateMachine = cacheExistsStateMachine
        val fetchingFreshCacheStateMachine = fetchingFreshCacheStateMachine
        
        if (cacheExistsStateMachine == null || fetchingFreshCacheStateMachine == null) {
            throw NodeNotPossibleError(this.toString())
        }

        val onlineDataStateMachine = OnlineCacheStateStateMachine(requirements,
                null,
                CacheStateMachine.cacheEmpty<CACHE>(),
                fetchingFreshCacheStateMachine)

        return OnlineCacheState(
                noCacheExists = false,
                fetchingForFirstTime = false,
                cacheData = null,
                lastTimeFetched = fetchingFreshCacheStateMachine.lastTimeFetched,
                isFetchingFreshData = fetchingFreshCacheStateMachine.isFetching,
                requirements = requirements,
                stateMachine = onlineDataStateMachine,
                errorDuringFirstFetch = null,
                justCompletedSuccessfulFirstFetch = false,
                errorDuringFetch = null,
                justCompletedSuccessfullyFetchingFreshData = false)
    }

    @Throws(NodeNotPossibleError::class)
    fun cache(cache: CACHE): OnlineCacheState<CACHE> {
        val cacheExistsStateMachine = cacheExistsStateMachine
        val fetchingFreshCacheStateMachine = fetchingFreshCacheStateMachine

        if (cacheExistsStateMachine == null || fetchingFreshCacheStateMachine == null) {
            throw NodeNotPossibleError(this.toString())
        }

        val onlineDataStateMachine = OnlineCacheStateStateMachine(requirements,
                null,
                CacheStateMachine.cacheExists(cache),
                fetchingFreshCacheStateMachine)

        return OnlineCacheState(
                noCacheExists = false,
                fetchingForFirstTime = false,
                cacheData = cache,
                lastTimeFetched = fetchingFreshCacheStateMachine.lastTimeFetched,
                isFetchingFreshData = fetchingFreshCacheStateMachine.isFetching,
                requirements = requirements,
                stateMachine = onlineDataStateMachine,
                errorDuringFirstFetch = null,
                justCompletedSuccessfulFirstFetch = false,
                errorDuringFetch = null,
                justCompletedSuccessfullyFetchingFreshData = false)
    }

    @Throws(NodeNotPossibleError::class)
    fun fetchingFreshCache(): OnlineCacheState<CACHE> {
        val cacheExistsStateMachine = cacheExistsStateMachine
        val fetchingFreshCacheStateMachine = fetchingFreshCacheStateMachine

        if (cacheExistsStateMachine == null || fetchingFreshCacheStateMachine == null) {
            throw NodeNotPossibleError(this.toString())
        }

        val onlineDataStateMachine = OnlineCacheStateStateMachine(requirements,
        null,
                cacheExistsStateMachine,
                fetchingFreshCacheStateMachine.fetching())

        return OnlineCacheState(
                noCacheExists = false,
                fetchingForFirstTime = false,
                cacheData = cacheExistsStateMachine.cache,
                lastTimeFetched = fetchingFreshCacheStateMachine.lastTimeFetched,
                isFetchingFreshData = true,
                requirements = requirements,
                stateMachine = onlineDataStateMachine,
                errorDuringFirstFetch = null,
                justCompletedSuccessfulFirstFetch = false,
                errorDuringFetch = null,
                justCompletedSuccessfullyFetchingFreshData = false)
    }

    @Throws(NodeNotPossibleError::class)
    fun failRefreshCache(error: Throwable): OnlineCacheState<CACHE> {
        val cacheExistsStateMachine = cacheExistsStateMachine
        val fetchingFreshCacheStateMachine = fetchingFreshCacheStateMachine

        if (cacheExistsStateMachine == null || fetchingFreshCacheStateMachine == null || !fetchingFreshCacheStateMachine.isFetching) {
            throw NodeNotPossibleError(this.toString())
        }

        val onlineDataStateMachine = OnlineCacheStateStateMachine(requirements,
                null,
                cacheExistsStateMachine,
                fetchingFreshCacheStateMachine.failedFetching(error))

        return OnlineCacheState(
                noCacheExists = false,
                fetchingForFirstTime = false,
                cacheData = cacheExistsStateMachine.cache,
                lastTimeFetched = fetchingFreshCacheStateMachine.lastTimeFetched,
                isFetchingFreshData = false,
                requirements = requirements,
                stateMachine = onlineDataStateMachine,
                errorDuringFirstFetch = null,
                justCompletedSuccessfulFirstFetch = false,
                errorDuringFetch = error,
                justCompletedSuccessfullyFetchingFreshData = false)
    }

    @Throws(NodeNotPossibleError::class)
    fun successfulRefreshCache(timeFetched: Date): OnlineCacheState<CACHE> {
        val cacheExistsStateMachine = cacheExistsStateMachine
        val fetchingFreshCacheStateMachine = fetchingFreshCacheStateMachine

        if (cacheExistsStateMachine == null || fetchingFreshCacheStateMachine == null || !fetchingFreshCacheStateMachine.isFetching) {
            throw NodeNotPossibleError(this.toString())
        }

        val onlineDataStateMachine = OnlineCacheStateStateMachine(requirements,
                null,
                cacheExistsStateMachine,
                fetchingFreshCacheStateMachine.successfulFetch(timeFetched))

        return OnlineCacheState(
                noCacheExists = false,
                fetchingForFirstTime = false,
                cacheData = cacheExistsStateMachine.cache,
                lastTimeFetched = timeFetched,
                isFetchingFreshData = false,
                requirements = requirements,
                stateMachine = onlineDataStateMachine,
                errorDuringFirstFetch = null,
                justCompletedSuccessfulFirstFetch = false,
                errorDuringFetch = null,
                justCompletedSuccessfullyFetchingFreshData = true)
    }

    override fun toString(): String {
        return "State of machine: ${noCacheStateMachine?.toString() ?: ""} ${cacheExistsStateMachine?.toString() ?: ""} ${fetchingFreshCacheStateMachine?.toString() ?: ""}"
    }
    
    internal class NodeNotPossibleError(stateOfMachine: String): Throwable("Node not possible path in state machine with current state of machine: $stateOfMachine")

}