package com.levibostian.teller.datastate

import com.levibostian.teller.datastate.listener.OnlineDataStateCacheListener
import com.levibostian.teller.datastate.listener.OnlineDataStateListener
import com.levibostian.teller.datastate.listener.OnlineDataStateFetchingListener
import com.levibostian.teller.datastate.listener.OnlineDataStateNoCacheStateListener
import com.levibostian.teller.datastate.online.statemachine.OnlineDataStateStateMachine
import com.levibostian.teller.repository.OnlineRepository
import java.util.*

/**
Holds the current state of data that is obtained via a network call. This data structure is meant to be passed out of Teller and to the application using Teller so it can parse it and display the data representation in the app.
The online data state is *not* manipulated here. It is only stored.
Data in apps are in 1 of 3 different types of state:
1. Cache data does not exist. It has never been attempted to be fetched or it has been attempted but failed and needs to be attempted again.
2. Data has been cached in the app and is either empty or not.
3. A cache exists, and we are fetching fresh data to update the cache.
 */
data class OnlineDataState<CACHE: Any> internal constructor(val noCacheExists: Boolean,
                                                           val fetchingForFirstTime: Boolean,
                                                           val cacheData: CACHE?,
                                                           val lastTimeFetched: Date?,
                                                           val isFetchingFreshData: Boolean,
                                                           val requirements: OnlineRepository.GetDataRequirements?,
                                                           internal val stateMachine: OnlineDataStateStateMachine<CACHE>?,
        // To prevent the end user getting spammed like crazy with UI messages of the same error or same status of data, the following properties should be set once in the constructor and then for future state calls, negate them.
                                                           val errorDuringFirstFetch: Throwable?,
                                                           val justCompletedSuccessfulFirstFetch: Boolean,
                                                           val errorDuringFetch: Throwable?,
                                                           val justCompletedSuccessfullyFetchingFreshData: Boolean) {

    internal companion object {
        /**
         * This constructor is meant to be more of a placeholder. It's having "no state".
         */
        fun <CACHE: Any> none(): OnlineDataState<CACHE> {
            return OnlineDataState(
                    noCacheExists = false,
                    fetchingForFirstTime = false,
                    cacheData = null,
                    lastTimeFetched = null,
                    isFetchingFreshData = false,
                    requirements = null,
                    stateMachine = null,
                    errorDuringFirstFetch = null,
                    justCompletedSuccessfulFirstFetch = false,
                    errorDuringFetch = null,
                    justCompletedSuccessfullyFetchingFreshData = false)
        }
    }

    /**
     * Used to change the state of data.
     */
    internal fun change(): OnlineDataStateStateMachine<CACHE> {
        return stateMachine!!
    }

    /**
     * Receive the full status of the data.
     *
     * @see OnlineDataStateNoCacheStateListener
     * @see OnlineDataStateCacheListener
     * @see OnlineDataStateFetchingListener
     */
    fun deliverAllStates(listener: OnlineDataStateListener<CACHE>) {
        deliverCacheState(listener)
        deliverFetchingFreshCacheState(listener)
        deliverNoCacheState(listener)
    }

    /**
     * This is usually used in the UI of an app to display the status of loading the data type for the first time to a user.
     */
    fun deliverFetchingFreshCacheState(listener: OnlineDataStateFetchingListener) {
        if (isFetchingFreshData) listener.fetching()
        errorDuringFetch?.let { listener.finishedFetching(it) }
        if (justCompletedSuccessfullyFetchingFreshData) listener.finishedFetching(errorDuringFetch)
    }

    /**
     * This is usually used in the UI of an app to display the cached data to a user.
     *
     * Using this function, you can get the state of the cached data as well as handle errors that may have happened during fetching the cached data.
     */
    fun deliverCacheState(listener: OnlineDataStateCacheListener<CACHE>) {
        if (!noCacheExists) {
            val data = cacheData
            if (data != null) {
                listener.cacheData(data, lastTimeFetched!!)
            } else {
                listener.cacheEmpty(lastTimeFetched!!)
            }
        }
    }

    /**
     * This is usually used in the UI of an app to display that the cached data on the device is empty to a user.
     */
    fun deliverNoCacheState(listener: OnlineDataStateNoCacheStateListener) {
        if (justCompletedSuccessfulFirstFetch) listener.finishedFirstFetch(null)
        if (fetchingForFirstTime) listener.firstFetch()
        errorDuringFirstFetch?.let { listener.finishedFirstFetch(it) }
        if (noCacheExists) listener.noCache()
    }

    override fun hashCode(): Int {
        var result = noCacheExists.hashCode()
        result = 31 * result + fetchingForFirstTime.hashCode()
        result = 31 * result + (cacheData?.hashCode() ?: 0)
        result = 31 * result + (lastTimeFetched?.hashCode() ?: 0)
        result = 31 * result + isFetchingFreshData.hashCode()
        result = 31 * result + (requirements?.hashCode() ?: 0)
        result = 31 * result + (errorDuringFirstFetch?.hashCode() ?: 0)
        result = 31 * result + justCompletedSuccessfulFirstFetch.hashCode()
        result = 31 * result + (errorDuringFetch?.hashCode() ?: 0)
        result = 31 * result + justCompletedSuccessfullyFetchingFreshData.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is OnlineDataState<*>) return false

        // Everything but state machine. That doesn't matter. The data here does.
        return this.noCacheExists == other.noCacheExists &&
                this.fetchingForFirstTime == other.fetchingForFirstTime &&
                this.cacheData == other.cacheData &&
                this.isFetchingFreshData == other.isFetchingFreshData &&
                this.lastTimeFetched == other.lastTimeFetched &&

                this.requirements?.tag == other.requirements?.tag &&

                this.errorDuringFirstFetch == other.errorDuringFirstFetch &&
                this.justCompletedSuccessfulFirstFetch == other.justCompletedSuccessfulFirstFetch &&
                this.justCompletedSuccessfullyFetchingFreshData == other.justCompletedSuccessfullyFetchingFreshData &&
                this.errorDuringFetch == other.errorDuringFetch
    }

}
