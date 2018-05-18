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
 */
class LocalDataState<DATA> private constructor(val isEmpty: Boolean = false,
                                               val data: DATA? = null) {

    companion object {
        // Use these constructors to construct the initial state of this immutable object. Use the functions
        fun <T> isEmpty(): LocalDataState<T> {
            return LocalDataState(isEmpty = true)
        }

        fun <T> data(data: T): LocalDataState<T> {
            return LocalDataState(data = data)
        }
    }

    /**
     * This is usually used in the UI of an app to display cacheData to a user.
     *
     * Using this function, you can get the state of the cacheData as well as handle errors that may have happened with cacheData (during fetching fresh cacheData or reading the cacheData off the device) or get the status of fetching fresh new cacheData.
     */
    fun deliver(listener: LocalDataStateListener<DATA>) {
        if (isEmpty) listener.isEmpty()
        data?.let { listener.data(it) }
    }

}
