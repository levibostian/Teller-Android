package com.levibostian.teller.cachestate

import com.levibostian.teller.cachestate.statemachine.CacheStateStateMachine
import com.levibostian.teller.repository.TellerRepository
import com.levibostian.teller.repository.RepositoryCache
import com.levibostian.teller.testing.cachestate.OnlineCacheStateTesting
import java.util.*

typealias WhenNoCacheCallback = (isFetching: Boolean, errorDuringFetch: Throwable?) -> Unit
typealias WhenCacheCallback<CACHE> = (cache: CACHE?, lastSuccessfulFetch: Date, isFetching: Boolean, justSuccessfullyFetched: Boolean, errorDuringFetch: Throwable?) -> Unit

/**
Holds the current state of response that is obtained via a network call. This response structure is meant to be passed out of Teller and to the application using Teller so it can parse it and display the response representation in the app.
The online response state is *not* manipulated here. It is only stored.
Data in apps are in 1 of 3 different types of state:
1. Cache response does not exist. It has never been attempted to be fetched or it has been attempted but failed and needs to be attempted again.
2. Data has been cached in the app and is either empty or not.
3. A cache exists, and we are fetching fresh response to update the cache.
 */
open class CacheState<CACHE: RepositoryCache> internal constructor(
        /**
         * Cache has never successfully been fetched before.
         */
        val noCacheExists: Boolean,
        /**
         * Is this cache currently being fetched for the first time?
         */
        val isFetchingFirstCache: Boolean,
        /**
         * The cache data, if it exists and is not empty.
         */
        val cache: CACHE?,
        /**
         * The time when the last successful cache fetch took finished.
         */
        val lastSuccessfulFetch: Date?,
        /**
         * Is this cache currently being updated?
         */
        val isFetchingToUpdateCache: Boolean,
        /**
         * The [TellerRepository.GetCacheRequirements] requirements that loaded this cache state.
         */
        val requirements: TellerRepository.GetCacheRequirements?,
        internal val stateMachine: CacheStateStateMachine<CACHE>?,
        // To prevent the end user getting spammed like crazy with UI messages of the same error or same status of response, the following properties should be set once in the constructor and then for future state calls, negate them.
        /**
         * Error thrown during the first fetch for cache.
         */
        val fetchFirstCacheError: Throwable?,
        /**
         * The first fetch of cache just successfully completed.
         */
        val justSuccessfullyFetchedFirstCache: Boolean,
        /**
         * Error thrown during the fetch to update the cache.
         */
        val fetchToUpdateCacheError: Throwable?,
        /**
         * The fetch to update cache just successfully completed.
         */
        val justSuccessfullyFetchedToUpdateCache: Boolean) {

    /**
     * Has a cache ever been successfully fetched before?
     */
    val cacheExists: Boolean
        get() = !noCacheExists

    /**
     * Cache has successfully been fetched before, and it's empty.
     */
    val cacheExistsAndEmpty: Boolean
        get() = cacheExists && cache == null

    /**
     * Is this cache currently being fetched for the first time or updated?
     */
    val isFetching: Boolean
        get() = isFetchingFirstCache || isFetchingToUpdateCache

    /**
     * If a recent fetch happened and there was an error.
     */
    val fetchError: Throwable?
        get() = fetchFirstCacheError ?: fetchToUpdateCacheError

    /**
     * Did a fetch just happen and was successful?
     */
    val justSuccessfullyFetchedCache: Boolean
        get() = justSuccessfullyFetchedFirstCache || justSuccessfullyFetchedToUpdateCache

    internal companion object {
        /**
         * This constructor is meant to be more of a placeholder. It's having "no state".
         */
        fun <CACHE: RepositoryCache> none(): CacheState<CACHE> {
            return CacheState(
                    noCacheExists = false,
                    isFetchingFirstCache = false,
                    cache = null,
                    lastSuccessfulFetch = null,
                    isFetchingToUpdateCache = false,
                    requirements = null,
                    stateMachine = null,
                    fetchFirstCacheError = null,
                    justSuccessfullyFetchedFirstCache = false,
                    fetchToUpdateCacheError = null,
                    justSuccessfullyFetchedToUpdateCache = false)
        }
    }

    /**
     * Used for testing purposes to create instances of [CacheState].
     *
     * @see OnlineCacheStateTesting
     */
    object Testing

    /**
     * Used to change the state of response.
     *
     * @throws RuntimeException If state machine is null. Which means the state of [CacheState] is none.
     */
    internal fun change(): CacheStateStateMachine<CACHE> {
        return stateMachine ?: throw RuntimeException("State machine is null. Cannot change state when there is no state machine!")
    }

    fun <NewDataType: Any> convert(converter: (CACHE?) -> NewDataType?): CacheState<NewDataType> {
        val newCache = converter(cache)

        // Notice that because this function is not used internally in the library, we provide `null` as the state machine because the state machine does not matter once it gets to the user.
        return CacheState(
                noCacheExists = noCacheExists,
                isFetchingFirstCache = isFetchingFirstCache,
                cache = newCache,
                lastSuccessfulFetch = lastSuccessfulFetch,
                isFetchingToUpdateCache = isFetchingToUpdateCache,
                requirements = requirements,
                stateMachine = null,
                fetchFirstCacheError = fetchFirstCacheError,
                justSuccessfullyFetchedFirstCache = justSuccessfullyFetchedFirstCache,
                fetchToUpdateCacheError = fetchToUpdateCacheError,
                justSuccessfullyFetchedToUpdateCache = justSuccessfullyFetchedToUpdateCache)
    }

    /**
     * Status of when a cache has never been successfully fetched before.
     *
     * *Note: After the first successful fetch for cache happens, this callback will not be notified. This callback will only be called when a cache has not successfully been fetched. See [justSuccessfullyFetchedFirstCache] when the first fetch finishes successfully.*
     */
    fun whenNoCache(call: WhenNoCacheCallback) {
        if (cacheExists) return

        call.invoke(isFetchingFirstCache, fetchFirstCacheError)
    }

    /**
     * Status of when a cache has been successfully fetched.
     */
    fun whenCache(call: WhenCacheCallback<CACHE>) {
        if (noCacheExists) return

        // state of OnlineCacheState could be none() which triggers this code. Therefore, make sure that last time fetched is not null before calling listener.
        lastSuccessfulFetch?.let { lastSuccessfulFetch ->
            call.invoke(cache, lastSuccessfulFetch, isFetchingToUpdateCache, justSuccessfullyFetchedCache, fetchToUpdateCacheError)
        }
    }

    override fun hashCode(): Int {
        var result = noCacheExists.hashCode()
        result = 31 * result + isFetchingFirstCache.hashCode()
        result = 31 * result + (cache?.hashCode() ?: 0)
        result = 31 * result + (lastSuccessfulFetch?.hashCode() ?: 0)
        result = 31 * result + isFetchingToUpdateCache.hashCode()
        result = 31 * result + (requirements?.hashCode() ?: 0)
        result = 31 * result + (fetchFirstCacheError?.hashCode() ?: 0)
        result = 31 * result + justSuccessfullyFetchedFirstCache.hashCode()
        result = 31 * result + (fetchToUpdateCacheError?.hashCode() ?: 0)
        result = 31 * result + justSuccessfullyFetchedToUpdateCache.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is CacheState<*>) return false

        // Everything but state machine. That doesn't matter. The response here does.
        return this.noCacheExists == other.noCacheExists &&
                this.isFetchingFirstCache == other.isFetchingFirstCache &&
                this.cache == other.cache &&
                this.isFetchingToUpdateCache == other.isFetchingToUpdateCache &&
                this.lastSuccessfulFetch == other.lastSuccessfulFetch &&

                this.requirements?.tag == other.requirements?.tag &&

                this.fetchFirstCacheError == other.fetchFirstCacheError &&
                this.justSuccessfullyFetchedFirstCache == other.justSuccessfullyFetchedFirstCache &&
                this.justSuccessfullyFetchedToUpdateCache == other.justSuccessfullyFetchedToUpdateCache &&
                this.fetchToUpdateCacheError == other.fetchToUpdateCacheError
    }

    override fun toString(): String {
        return stateMachine?.toString() ?: "State: none"
    }

}
