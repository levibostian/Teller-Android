package com.levibostian.teller.testing.repository

import android.os.AsyncTask
import com.levibostian.teller.repository.GetCacheRequirementsTag
import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.teller.repository.OnlineRepositoryCache
import com.levibostian.teller.repository.OnlineRepositoryFetchResponse
import com.levibostian.teller.repository.manager.OnlineRepositoryCacheAgeManager
import com.levibostian.teller.repository.manager.OnlineRepositoryRefreshManager
import com.levibostian.teller.repository.manager.OnlineRepositoryRefreshManagerWrapper
import com.levibostian.teller.repository.manager.TellerOnlineRepositoryCacheAgeManager
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.max

/**
 * Used for testing purposes to initialize the state of a [OnlineRepository] subclass instance.
 *
 * You can use this class directly, or, use the recommended extension functions in the [OnlineRepository.Testing] object.
 *
 * Intentions of [OnlineRepositoryTesting]:
 * 1. Be able to initialize the state of an [OnlineRepository] state with 1 line of code.
 * 2. Be utility for integration testing purposes when an [OnlineRepository] is interacting with non-mocked instances of objects.
 * 3. Populate a new value for cache and last fetched time if a cache has been fetched before. However, it is also assumed that in your test setup you have cleared Teller data and all of your cache data so the test is running on a clean slate. This class does *not* delete data for you, it only provides new values.
 */
class OnlineRepositoryTesting {

    private constructor() {
        this.cacheAgeManager = TellerOnlineRepositoryCacheAgeManager()
    }

    private val cacheAgeManager: OnlineRepositoryCacheAgeManager

    internal constructor(cacheAgeManager: OnlineRepositoryCacheAgeManager) {
        this.cacheAgeManager = cacheAgeManager
    }

    companion object {
        fun <CACHE: OnlineRepositoryCache, REQ: OnlineRepository.GetCacheRequirements, FETCH_RESPONSE: OnlineRepositoryFetchResponse> initState(
                repository: OnlineRepository<CACHE, REQ, FETCH_RESPONSE>,
                requirements: REQ,
                more: (StateOfOnlineRepositoryDsl<FETCH_RESPONSE>.() -> Unit)? = null): SetValues {
            return OnlineRepositoryTesting().initState(repository,  requirements, more)
        }

        fun <CACHE: OnlineRepositoryCache, REQ: OnlineRepository.GetCacheRequirements, FETCH_RESPONSE: OnlineRepositoryFetchResponse> initStateAsync(
                repository: OnlineRepository<CACHE, REQ, FETCH_RESPONSE>,
                requirements: REQ,
                more: (StateOfOnlineRepositoryDsl<FETCH_RESPONSE>.() -> Unit)? = null,
                complete: (SetValues) -> Unit) {
            OnlineRepositoryTesting().initStateAsync(repository,  requirements, more, complete)
        }
    }

    fun <CACHE: OnlineRepositoryCache, REQ: OnlineRepository.GetCacheRequirements, FETCH_RESPONSE: OnlineRepositoryFetchResponse> initStateAsync(
            repository: OnlineRepository<CACHE, REQ, FETCH_RESPONSE>,
            requirements: REQ,
            more: (StateOfOnlineRepositoryDsl<FETCH_RESPONSE>.() -> Unit)? = null,
            complete: (SetValues) -> Unit) {
        SaveDataAsyncTask({
            initState(repository, requirements, more)
        }, { error, setValues ->
            error?.let { throw it }
            complete(setValues!!)
        }).execute()
    }

    fun <CACHE: OnlineRepositoryCache, REQ: OnlineRepository.GetCacheRequirements, FETCH_RESPONSE: OnlineRepositoryFetchResponse> initState(
            repository: OnlineRepository<CACHE, REQ, FETCH_RESPONSE>,
            requirements: REQ,
            more: (StateOfOnlineRepositoryDsl<FETCH_RESPONSE>.() -> Unit)? = null): SetValues {
        val cacheExists = StateOfOnlineRepositoryDsl<FETCH_RESPONSE>()
        more?.let { cacheExists.it() }

        var setValues = SetValues(null)

        cacheExists.props.let { proposedState ->
            if (proposedState.cacheExists) {
                proposedState.cache?.let { testingCache ->
                    repository.saveCacheSyncCurrentThread(testingCache, requirements)
                }

                var lastFetched = Date()
                cacheExists.cacheExistsDsl?.props?.let { proposedCacheState ->
                    if (proposedCacheState.cacheTooOld) {
                        val maxAgeOfDataMinusHour = Calendar.getInstance().apply {
                            time = repository.maxAgeOfCache.toDate()
                            add(Calendar.HOUR, -1)
                        }.time

                        lastFetched = maxAgeOfDataMinusHour
                    }
                }

                setValues = SetValues(lastFetched)
                cacheAgeManager.updateLastSuccessfulFetch(requirements.tag, lastFetched)
            } else {
                // No cache exists. Ignore request, assume that dev cleared.
            }
        }

        return setValues
    }

    @OnlineRepositoryTestingDsl
    class StateOfOnlineRepositoryDsl<CACHE: OnlineRepositoryCache> {
        var props = Props<CACHE>()
        var cacheExistsDsl: CacheExistsDsl? = null

        fun noCache() {
            props = Props(
                    cacheExists = false,
                    cache = null
            )
        }

        fun cacheEmpty(more: (CacheExistsDsl.() -> Unit)? = null) {
            props = Props(
                    cacheExists = true,
                    cache = null
            )

            more?.let {
                cacheExistsDsl = CacheExistsDsl().apply(it)
            } ?: kotlin.run {
                cacheExistsDsl = null
            }
        }

        fun cache(cache: CACHE, more: (CacheExistsDsl.() -> Unit)? = null) {
            props = Props(
                    cacheExists = true,
                    cache = cache
            )
            more?.let {
                cacheExistsDsl = CacheExistsDsl().apply(it)
            } ?: kotlin.run {
                cacheExistsDsl = null
            }
        }

        data class Props<CACHE: OnlineRepositoryCache>(val cacheExists: Boolean = false,
                                                       val cache: CACHE? = null)
    }

    @OnlineRepositoryTestingDsl
    class CacheExistsDsl {
        var props = Props()

        /**
         * Disabled until [Kotlin bug](https://youtrack.jetbrains.com/issue/KT-30257#focus=streamItem-27-3277487-0-0) is resolved.
         */
//        fun lastFetched(lastFetched: Date) {
//            props = Props(
//                    lastFetched = lastFetched,
//                    cacheTooOld = false,
//                    cacheNotTooOld = false
//            )
//        }

        fun cacheTooOld() {
            props = Props(
                    lastFetched = null,
                    cacheTooOld = true,
                    cacheNotTooOld = false
            )
        }

        fun cacheNotTooOld() {
            props = Props()
        }

        /**
         * Make default where cache not too old (make cache last fetched now) to give [OnlineRepository] the minimum amount of interactions. There must be *some* last fetched time because cache does exist!
         */
        data class Props(val lastFetched: Date? = null,
                         val cacheTooOld: Boolean = false,
                         val cacheNotTooOld: Boolean = true)
    }

    private class SaveDataAsyncTask(private val saveData: () -> SetValues,
                                    private val complete: (error: Throwable?, setValues: SetValues?) -> Unit?): AsyncTask<Unit?, Unit?, Unit?>() {

        private var setValues: SetValues? = null
        private var doInBackgroundError: Throwable? = null

        override fun doInBackground(vararg p: Unit?): Unit? {
            try {
                setValues = saveData()
            } catch (e: Throwable) {
                doInBackgroundError = e
            }

            return null
        }

        override fun onPostExecute(result: Unit?) {
            super.onPostExecute(result)

            complete(doInBackgroundError, setValues)
        }

    }

    /**
     * The values set in one of the [OnlineRepositoryTesting] init functions.
     */
    data class SetValues(val lastFetched: Date?)

}