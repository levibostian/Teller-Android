package com.levibostian.teller.datastate

import java.util.*

/**
 * Data in apps are in 1 of 3 different types of state:
 *
 * 1. Data does not exist. It has never been obtained before.
 * 2. It is empty. Data has been obtained before, but there is none.
 * 3. Data exists.
 *
 * This class takes in a type of cacheData to keep state on via generic [DATA] and it maintains the state of that cacheData.
 *
 * Along with the 3 different states cacheData could be in, there are temporary states that cacheData could also be in.
 *
 * * An error occurred with that cacheData.
 * * Fresh cacheData is being fetched for this cacheData. It may be updated soon.
 *
 * The 3 states listed above empty, cacheData, loading are all permanent. Data is 1 of those 3 at all times. Data has this error or fetching status temporarily until someone calls [deliver] one time and then those temporary states are deleted.
 *
 * This class is used in companion with [Repository] and [OnlineStateDataCompoundBehaviorSubject] to maintain the state of cacheData to deliver to someone observing.
 *
 * @property firstFetchOfData When cacheData has never been fetched before for a cacheData type, this is where it all begins. After this, cacheData will be empty or cacheData state.
 * @property errorDuringFetch Says that the [latestError] was caused during the fetching phase.
 */
class OnlineDataState<DATA> private constructor(val firstFetchOfData: Boolean = false,
                                                val doneFirstFetchOfData: Boolean = false,
                                                val isEmpty: Boolean = false,
                                                val data: DATA? = null,
                                                val dataFetched: Date? = null,
                                                val errorDuringFirstFetch: Throwable? = null,
                                                val isFetchingFreshData: Boolean = false,
                                                val doneFetchingFreshData: Boolean = false,
                                                val errorDuringFetch: Throwable? = null) {

    companion object {
        // Use these constructors to construct the initial state of this immutable object. Use the functions
        fun <T> firstFetchOfData(): OnlineDataState<T> {
            return OnlineDataState(firstFetchOfData = true)
        }

        fun <T> isEmpty(): OnlineDataState<T> {
            return OnlineDataState(isEmpty = true)
        }

        fun <T> data(data: T, dataFetched: Date): OnlineDataState<T> {
            return OnlineDataState(data = data, dataFetched = dataFetched)
        }
    }

    /**
     * Tag on an error to this cacheData. Errors could be an error fetching fresh cacheData or reading cacheData off the device. The errors should have to deal with this cacheData, not some generic error encountered in the app.
     *
     * @return New immutable instance of [OnlineDataState]
     */
    fun doneFirstFetch(error: Throwable?): OnlineDataState<DATA> {
        return duplicate(
                firstFetchOfData = false,
                doneFirstFetchOfData = true,
                errorDuringFirstFetch = error
        )
    }

    /**
     * Set the status of this cacheData as fetching fresh cacheData.
     *
     * @return New immutable instance of [OnlineDataState]
     */
    fun fetchingFreshData(): OnlineDataState<DATA> {
        if (firstFetchOfData) throw RuntimeException("The state of cacheData is saying you are already fetching for the first time. You cannot fetch for first time and fetch after cache.")

        return duplicate(
                isFetchingFreshData = true
        )
    }

    /**
     * Set the status of this cacheData as done fetching fresh cacheData.
     *
     * @return New immutable instance of [OnlineDataState]
     */
    fun doneFetchingFreshData(errorDuringFetch: Throwable?): OnlineDataState<DATA> {
        if (firstFetchOfData) throw RuntimeException("Call doneFirstFetch() instead.")

        return duplicate(
                isFetchingFreshData = false,
                doneFetchingFreshData = true,
                errorDuringFetch = errorDuringFetch
        )
    }

    // This exists because I have tried to make OnlineDataSource immutable. With that, there are fields that need to be copied over from the previous state of the previous object. So, I am using this where you can override fields one at a time that you need.
    private fun duplicate(firstFetchOfData: Boolean = this.firstFetchOfData,
                          doneFirstFetchOfData: Boolean = this.doneFirstFetchOfData,
                          isEmpty: Boolean = this.isEmpty,
                          data: DATA? = this.data,
                          dataFetched: Date? = this.dataFetched,
                          errorDuringFirstFetch: Throwable? = null, // Set null to avoid calling the listener error() multiple times.
                          isFetchingFreshData: Boolean = this.isFetchingFreshData,
                          doneFetchingFreshData: Boolean = this.doneFetchingFreshData,
                          errorDuringFetch: Throwable? = null // Set null to avoid calling the listener error() multiple times.
    ): OnlineDataState<DATA> {
        return OnlineDataState(firstFetchOfData = firstFetchOfData,
                doneFirstFetchOfData = doneFirstFetchOfData,
                isEmpty = isEmpty,
                data = data,
                dataFetched = dataFetched,
                errorDuringFirstFetch = errorDuringFirstFetch,
                isFetchingFreshData = isFetchingFreshData,
                doneFetchingFreshData = doneFetchingFreshData,
                errorDuringFetch = errorDuringFetch)
    }

    /**
     * This is usually used in the UI of an app to display cacheData to a user.
     *
     * Using this function, you can get the state of the cacheData as well as handle errors that may have happened with cacheData (during fetching fresh cacheData or reading the cacheData off the device) or get the status of fetching fresh new cacheData.
     */
    fun deliver(listener: OnlineDataStateListener<DATA>) {
        if (firstFetchOfData) listener.firstFetchOfData()
        if (isEmpty) listener.cacheEmpty()
        if (isFetchingFreshData) listener.fetchingFreshData()
        if (doneFetchingFreshData) listener.finishedFetchingFreshData(errorDuringFetch)
        data?.let { listener.cacheData(it, dataFetched!!) }
        if (doneFirstFetchOfData) listener.finishedFirstFetchOfData(errorDuringFirstFetch)
    }

}
