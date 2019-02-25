package com.levibostian.teller.cachestate

import com.levibostian.teller.cachestate.listener.LocalCacheStateListener
import com.levibostian.teller.repository.LocalRepository
import com.levibostian.teller.subject.LocalCacheStateCompoundBehaviorSubject

/**
 * Local response (response obtained from the user or device, no network fetch call) in apps are in 1 of 2 different types of state:
 *
 * 1. It is empty.
 * 2. It is not empty.
 *
 * This class takes in a type of [cacheData] to keep state on via generic [CACHE] and it maintains the state of that [cacheData]. It gives you a snapshot of the state of your local response at any given time.
 *
 * This class is used in companion with [LocalRepository] and [LocalCacheStateCompoundBehaviorSubject] to maintain the state of [cacheData] to deliver to someone observing.
 *
 */
data class LocalCacheState<CACHE: Any> internal constructor(val isEmpty: Boolean,
                                                            val cacheData: CACHE?) {

    internal companion object {
        fun <CACHE: Any> none(): LocalCacheState<CACHE> = LocalCacheState(false, null)

        fun <CACHE: Any> isEmpty(): LocalCacheState<CACHE> = LocalCacheState(true, null)

        fun <CACHE: Any> data(data: CACHE): LocalCacheState<CACHE> = LocalCacheState(false, data)
    }

    /**
     * This is usually used in the UI of an app to display cache to a user.
     *
     * Using this function, you can get the state of the cache locally on your device.
     */
    fun deliverState(listener: LocalCacheStateListener<CACHE>) {
        if (isEmpty) listener.isEmpty()
        cacheData?.let { listener.data(it) }
    }

}
