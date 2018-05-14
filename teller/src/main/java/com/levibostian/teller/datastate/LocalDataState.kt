package com.levibostian.teller.datastate

import java.util.*

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
 */
class LocalDataState<DATA> private constructor(val isEmpty: Boolean = false,
                                               val data: DATA? = null,
                                               val dataFetched: Date? = null,
                                               val latestError: Throwable? = null) {

    companion object {
        // Use these constructors to construct the initial state of this immutable object. Use the functions
        fun <T> isEmpty(): LocalDataState<T> {
            return LocalDataState(isEmpty = true)
        }

        fun <T> data(data: T, dataFetched: Date): LocalDataState<T> {
            return LocalDataState(data = data, dataFetched = dataFetched)
        }
    }

    /**
     * Tag on an error to this data. Errors could be an error fetching fresh data or reading data off the device. The errors should have to deal with this data, not some generic error encountered in the app.
     *
     * @return New immutable instance of [LocalDataState]
     */
    fun errorOccurred(error: Throwable): LocalDataState<DATA> {
        return LocalDataState(isEmpty = isEmpty,
                data = data,
                latestError = error)
    }

    /**
     * This is usually used in the UI of an app to display data to a user.
     *
     * Using this function, you can get the state of the data as well as handle errors that may have happened with data (during fetching fresh data or reading the data off the device) or get the status of fetching fresh new data.
     */
    fun deliver(listener: LocalDataStateListener<DATA>) {
        if (isEmpty) listener.isEmpty()
        data?.let { listener.data(it, dataFetched!!) }
        latestError?.let { listener.error(it) }
    }

}
