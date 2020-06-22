package com.levibostian.teller.cachestate.statemachine

import com.levibostian.teller.cachestate.CacheState
import com.levibostian.teller.repository.TellerRepository
import com.levibostian.teller.repository.RepositoryCache
import java.util.*

/**
 * Finite state machine for [CacheState].
 *
 * This object is used internally to keep track and enforce the changing of states that an online cache can go through.
 *
 * The object is designed to begin with an empty state at the constructor. Then via a list of functions, change the state of response. If a function throws an error, it is an illegal traversal through the state machine. If you get an [CacheState] instance back from a function, you have successfully changed the state of response.
 *
 * Requirements of [CacheStateStateMachine]:
 * 1. Should be anal about traveling from node to node. Example: This should be invalid code: `StateMachine.cacheExists().successfulFetch()`. You need to prove that you refreshed first, then successfully finished fetching: `StateMachine.cacheExists().fetching().successfulFetch()`.
 * 2. Immutable. Each instance represents [CacheState] (which is also an immutable object).
 * 3. Allows you to travel from it's state (node of state machine) to another state the cache has moved to (another node of the state machine).
 */
internal class CacheStateStateMachine<CACHE: RepositoryCache> private constructor(private val requirements: TellerRepository.GetCacheRequirements,
                                                                                  private val noCacheStateMachine: NoCacheStateMachine?,
                                                                                  private val cacheExistsStateMachine: CacheStateMachine<CACHE>?,
                                                                                  private val fetchingFreshCacheStateMachine: FetchingFreshCacheStateMachine?) {

    companion object {
        fun <CACHE: RepositoryCache> noCacheExists(requirements: TellerRepository.GetCacheRequirements): CacheState<CACHE> {
            val onlineDataStateMachine = CacheStateStateMachine<CACHE>(requirements, NoCacheStateMachine.noCacheExists(), null, null)

            return CacheState(
                    noCacheExists = true,
                    isFetchingFirstCache = false,
                    cache = null,
                    lastSuccessfulFetch = null,
                    isFetchingToUpdateCache = false,
                    requirements = requirements,
                    stateMachine = onlineDataStateMachine,
                    fetchFirstCacheError = null,
                    justSuccessfullyFetchedFirstCache = false,
                    fetchToUpdateCacheError = null,
                    justSuccessfullyFetchedToUpdateCache = false)
        }

        fun <CACHE: RepositoryCache> cacheExists(requirements: TellerRepository.GetCacheRequirements, lastTimeFetched: Date): CacheState<CACHE> {
            val cacheExistsStateMachine = CacheStateMachine.cacheEmpty<CACHE>() // Empty is a placeholder for now but it indicates that a cache does exist for future calls to the state machine.
            val fetchingFreshCacheStateMachine = FetchingFreshCacheStateMachine.notFetching(lastTimeFetched)
            val onlineDataStateMachine = CacheStateStateMachine(requirements, null, cacheExistsStateMachine, fetchingFreshCacheStateMachine)

            return CacheState(
                    noCacheExists = false,
                    isFetchingFirstCache = false,
                    cache = null,
                    lastSuccessfulFetch = lastTimeFetched,
                    isFetchingToUpdateCache = false,
                    requirements = requirements,
                    stateMachine = onlineDataStateMachine,
                    fetchFirstCacheError = null,
                    justSuccessfullyFetchedFirstCache = false,
                    fetchToUpdateCacheError = null,
                    justSuccessfullyFetchedToUpdateCache = false)
        }
    }

    @Throws(NodeNotPossibleError::class)
    fun firstFetch(): CacheState<CACHE> {
        val noCacheStateMachine = this.noCacheStateMachine ?: throw NodeNotPossibleError(this.toString())

        val onlineDataStateMachine = CacheStateStateMachine<CACHE>(requirements, noCacheStateMachine.fetching(), null, null)

        return CacheState(
                noCacheExists = true,
                isFetchingFirstCache = true,
                cache = null,
                lastSuccessfulFetch = null,
                isFetchingToUpdateCache = false,
                requirements = requirements,
                stateMachine = onlineDataStateMachine,
                fetchFirstCacheError = null,
                justSuccessfullyFetchedFirstCache = false,
                fetchToUpdateCacheError = null,
                justSuccessfullyFetchedToUpdateCache = false)
    }

    @Throws(NodeNotPossibleError::class)
    fun failFirstFetch(error: Throwable): CacheState<CACHE> {
        val noCacheStateMachine = this.noCacheStateMachine 
        
        if (noCacheStateMachine == null || !noCacheStateMachine.isFetching) {
            throw NodeNotPossibleError(this.toString())
        }

        val onlineDataStateMachine = CacheStateStateMachine<CACHE>(requirements, noCacheStateMachine.failedFetching(error), null, null)
        
        return CacheState(
                noCacheExists = true,
                isFetchingFirstCache = false,
                cache = null,
                lastSuccessfulFetch = null,
                isFetchingToUpdateCache = false,
                requirements = requirements,
                stateMachine = onlineDataStateMachine,
                fetchFirstCacheError = error,
                justSuccessfullyFetchedFirstCache = false,
                fetchToUpdateCacheError = null,
                justSuccessfullyFetchedToUpdateCache = false)
    }

    @Throws(NodeNotPossibleError::class)
    fun successfulFirstFetch(timeFetched: Date): CacheState<CACHE> {
        val noCacheStateMachine = this.noCacheStateMachine

        if (noCacheStateMachine == null || !noCacheStateMachine.isFetching) {
            throw NodeNotPossibleError(this.toString())
        }

        val onlineDataStateMachine = CacheStateStateMachine<CACHE>(requirements,
                null, // Cache now exists, no remove this state machine. We will no longer be able to go back to these nodes. 
                CacheStateMachine.cacheEmpty(), // empty is like a placeholder.
                FetchingFreshCacheStateMachine.notFetching(timeFetched))

        return CacheState(
                noCacheExists = false,
                isFetchingFirstCache = false,
                cache = null,
                lastSuccessfulFetch = timeFetched,
                isFetchingToUpdateCache = false,
                requirements = requirements,
                stateMachine = onlineDataStateMachine,
                fetchFirstCacheError = null,
                justSuccessfullyFetchedFirstCache = true,
                fetchToUpdateCacheError = null,
                justSuccessfullyFetchedToUpdateCache = false)
    }

    @Throws(NodeNotPossibleError::class)
    fun cacheEmpty(): CacheState<CACHE> {
        val cacheExistsStateMachine = cacheExistsStateMachine
        val fetchingFreshCacheStateMachine = fetchingFreshCacheStateMachine
        
        if (cacheExistsStateMachine == null || fetchingFreshCacheStateMachine == null) {
            throw NodeNotPossibleError(this.toString())
        }

        val onlineDataStateMachine = CacheStateStateMachine(requirements,
                null,
                CacheStateMachine.cacheEmpty<CACHE>(),
                fetchingFreshCacheStateMachine)

        return CacheState(
                noCacheExists = false,
                isFetchingFirstCache = false,
                cache = null,
                lastSuccessfulFetch = fetchingFreshCacheStateMachine.lastTimeFetched,
                isFetchingToUpdateCache = fetchingFreshCacheStateMachine.isFetching,
                requirements = requirements,
                stateMachine = onlineDataStateMachine,
                fetchFirstCacheError = null,
                justSuccessfullyFetchedFirstCache = false,
                fetchToUpdateCacheError = null,
                justSuccessfullyFetchedToUpdateCache = false)
    }

    @Throws(NodeNotPossibleError::class)
    fun cache(cache: CACHE): CacheState<CACHE> {
        val cacheExistsStateMachine = cacheExistsStateMachine
        val fetchingFreshCacheStateMachine = fetchingFreshCacheStateMachine

        if (cacheExistsStateMachine == null || fetchingFreshCacheStateMachine == null) {
            throw NodeNotPossibleError(this.toString())
        }

        val onlineDataStateMachine = CacheStateStateMachine(requirements,
                null,
                CacheStateMachine.cacheExists(cache),
                fetchingFreshCacheStateMachine)

        return CacheState(
                noCacheExists = false,
                isFetchingFirstCache = false,
                cache = cache,
                lastSuccessfulFetch = fetchingFreshCacheStateMachine.lastTimeFetched,
                isFetchingToUpdateCache = fetchingFreshCacheStateMachine.isFetching,
                requirements = requirements,
                stateMachine = onlineDataStateMachine,
                fetchFirstCacheError = null,
                justSuccessfullyFetchedFirstCache = false,
                fetchToUpdateCacheError = null,
                justSuccessfullyFetchedToUpdateCache = false)
    }

    @Throws(NodeNotPossibleError::class)
    fun fetchingFreshCache(): CacheState<CACHE> {
        val cacheExistsStateMachine = cacheExistsStateMachine
        val fetchingFreshCacheStateMachine = fetchingFreshCacheStateMachine

        if (cacheExistsStateMachine == null || fetchingFreshCacheStateMachine == null) {
            throw NodeNotPossibleError(this.toString())
        }

        val onlineDataStateMachine = CacheStateStateMachine(requirements,
        null,
                cacheExistsStateMachine,
                fetchingFreshCacheStateMachine.fetching())

        return CacheState(
                noCacheExists = false,
                isFetchingFirstCache = false,
                cache = cacheExistsStateMachine.cache,
                lastSuccessfulFetch = fetchingFreshCacheStateMachine.lastTimeFetched,
                isFetchingToUpdateCache = true,
                requirements = requirements,
                stateMachine = onlineDataStateMachine,
                fetchFirstCacheError = null,
                justSuccessfullyFetchedFirstCache = false,
                fetchToUpdateCacheError = null,
                justSuccessfullyFetchedToUpdateCache = false)
    }

    @Throws(NodeNotPossibleError::class)
    fun failRefreshCache(error: Throwable): CacheState<CACHE> {
        val cacheExistsStateMachine = cacheExistsStateMachine
        val fetchingFreshCacheStateMachine = fetchingFreshCacheStateMachine

        if (cacheExistsStateMachine == null || fetchingFreshCacheStateMachine == null || !fetchingFreshCacheStateMachine.isFetching) {
            throw NodeNotPossibleError(this.toString())
        }

        val onlineDataStateMachine = CacheStateStateMachine(requirements,
                null,
                cacheExistsStateMachine,
                fetchingFreshCacheStateMachine.failedFetching(error))

        return CacheState(
                noCacheExists = false,
                isFetchingFirstCache = false,
                cache = cacheExistsStateMachine.cache,
                lastSuccessfulFetch = fetchingFreshCacheStateMachine.lastTimeFetched,
                isFetchingToUpdateCache = false,
                requirements = requirements,
                stateMachine = onlineDataStateMachine,
                fetchFirstCacheError = null,
                justSuccessfullyFetchedFirstCache = false,
                fetchToUpdateCacheError = error,
                justSuccessfullyFetchedToUpdateCache = false)
    }

    @Throws(NodeNotPossibleError::class)
    fun successfulRefreshCache(timeFetched: Date): CacheState<CACHE> {
        val cacheExistsStateMachine = cacheExistsStateMachine
        val fetchingFreshCacheStateMachine = fetchingFreshCacheStateMachine

        if (cacheExistsStateMachine == null || fetchingFreshCacheStateMachine == null || !fetchingFreshCacheStateMachine.isFetching) {
            throw NodeNotPossibleError(this.toString())
        }

        val onlineDataStateMachine = CacheStateStateMachine(requirements,
                null,
                cacheExistsStateMachine,
                fetchingFreshCacheStateMachine.successfulFetch(timeFetched))

        return CacheState(
                noCacheExists = false,
                isFetchingFirstCache = false,
                cache = cacheExistsStateMachine.cache,
                lastSuccessfulFetch = timeFetched,
                isFetchingToUpdateCache = false,
                requirements = requirements,
                stateMachine = onlineDataStateMachine,
                fetchFirstCacheError = null,
                justSuccessfullyFetchedFirstCache = false,
                fetchToUpdateCacheError = null,
                justSuccessfullyFetchedToUpdateCache = true)
    }

    override fun toString(): String {
        return "State: ${noCacheStateMachine?.toString() ?: ""} ${cacheExistsStateMachine?.toString() ?: ""} ${fetchingFreshCacheStateMachine?.toString() ?: ""}"
    }
    
    internal class NodeNotPossibleError(stateOfMachine: String): Throwable("Node not possible path in state machine with current state of machine: $stateOfMachine")

}