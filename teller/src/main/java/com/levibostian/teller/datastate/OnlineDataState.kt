package com.levibostian.teller.datastate

/**
 * Data in apps are in 1 of 3 different types of state:
 *
 * 1. Data does not exist. It has never been obtained before.
 * 2. It is empty. Data has been obtained before, but there is none.
 * 3. Data exists.
 *
 * This class takes in a type of data to keep state on via generic [DATA] and it maintains the state of that data.
 *
 * Along with the 3 different states data could be in, there are temporary states that data could also be in.
 *
 * * An error occurred with that data.
 * * Fresh data is being fetched for this data. It may be updated soon.
 *
 * The 3 states listed above empty, data, loading are all permanent. Data is 1 of those 3 at all times. Data has this error or fetching status temporarily until someone calls [deliver] one time and then those temporary states are deleted.
 *
 * This class is used in companion with [Repository] and [OnlineStateDataCompoundBehaviorSubject] to maintain the state of data to deliver to someone observing.
 *
 * @property firstFetchOfData When data has never been fetched before for a data type, this is where it all begins. After this, data will be empty or data state.
 * @property errorDuringFetch Says that the [latestError] was caused during the fetching phase.
 */
class OnlineDataState<DATA> private constructor(val firstFetchOfData: Boolean = false,
                                                val isEmpty: Boolean = false,
                                                val data: DATA? = null,
                                                val latestError: Throwable? = null,
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

        fun <T> data(data: T): OnlineDataState<T> {
            return OnlineDataState(data = data)
        }
    }

    /**
     * Tag on an error to this data. Errors could be an error fetching fresh data or reading data off the device. The errors should have to deal with this data, not some generic error encountered in the app.
     *
     * @return New immutable instance of [OnlineDataState]
     */
    fun errorOccurred(error: Throwable): OnlineDataState<DATA> {
        return OnlineDataState(firstFetchOfData = firstFetchOfData,
                isEmpty = isEmpty,
                data = data,
                latestError = error,
                isFetchingFreshData = isFetchingFreshData,
                doneFetchingFreshData = doneFetchingFreshData,
                errorDuringFetch = null) // Set null to avoid calling the listener error() multiple times.
    }

    /**
     * Set the status of this data as fetching fresh data.
     *
     * @return New immutable instance of [OnlineDataState]
     */
    fun fetchingFreshData(): OnlineDataState<DATA> {
        return OnlineDataState(firstFetchOfData = firstFetchOfData,
                isEmpty = isEmpty,
                data = data,
                latestError = null, // Set null to avoid calling the listener error() multiple times.
                isFetchingFreshData = true,
                doneFetchingFreshData = false,
                errorDuringFetch = null) // Set null to avoid calling the listener error() multiple times.
    }

    /**
     * Set the status of this data as done fetching fresh data.
     *
     * @return New immutable instance of [OnlineDataState]
     */
    fun doneFetchingFreshData(errorDuringFetch: Throwable?): OnlineDataState<DATA> {
        return OnlineDataState(firstFetchOfData = firstFetchOfData,
                isEmpty = isEmpty,
                data = data,
                latestError = null, // Set null to avoid calling the listener error() multiple times.
                isFetchingFreshData = false,
                doneFetchingFreshData = true,
                errorDuringFetch = errorDuringFetch)
    }

    /**
     * This is usually used in the UI of an app to display data to a user.
     *
     * Using this function, you can get the state of the data as well as handle errors that may have happened with data (during fetching fresh data or reading the data off the device) or get the status of fetching fresh new data.
     */
    fun deliver(listener: OnlineDataStateListener<DATA>) {
        if (firstFetchOfData) listener.firstFetchOfData()
        if (isEmpty) listener.isEmpty()
        if (isFetchingFreshData) listener.fetchingFreshData()
        if (doneFetchingFreshData) listener.finishedFetchingFreshData(errorDuringFetch)
        data?.let { listener.data(it) }
        latestError?.let { listener.error(it) }
    }

}
