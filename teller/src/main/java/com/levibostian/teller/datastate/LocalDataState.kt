package com.levibostian.teller.datastate

import com.levibostian.teller.datastate.listener.LocalDataStateListener
import com.levibostian.teller.repository.LocalRepository
import com.levibostian.teller.subject.LocalDataStateCompoundBehaviorSubject

/**
 * Local data (data obtained from the user or device, no network fetch call) in apps are in 1 of 2 different types of state:
 *
 * 1. It is empty.
 * 2. It is not empty.
 *
 * This class takes in a type of [cacheData] to keep state on via generic [CACHE] and it maintains the state of that [cacheData]. It gives you a snapshot of the state of your local data at any given time.
 *
 * This class is used in companion with [LocalRepository] and [LocalDataStateCompoundBehaviorSubject] to maintain the state of [cacheData] to deliver to someone observing.
 *
 */
data class LocalDataState<CACHE: Any> internal constructor(val isEmpty: Boolean,
                                                           val cacheData: CACHE?) {

    internal companion object {
        fun <CACHE: Any> none(): LocalDataState<CACHE> = LocalDataState(false, null)

        fun <CACHE: Any> isEmpty(): LocalDataState<CACHE> = LocalDataState(true, null)

        fun <CACHE: Any> data(data: CACHE): LocalDataState<CACHE> = LocalDataState(false, data)
    }

    /**
     * This is usually used in the UI of an app to display cacheData to a user.
     *
     * Using this function, you can get the state of the cacheData locally on your device.
     */
    fun deliverState(listener: LocalDataStateListener<CACHE>) {
        if (isEmpty) listener.isEmpty()
        cacheData?.let { listener.data(it) }
    }

}
