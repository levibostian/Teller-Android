package com.levibostian.teller.datastate.listener

interface OnlineDataStateNoCacheStateListener {
    fun noCache()
    fun firstFetch()
    /**
     * @param errorDuringFetch Error that occurred during the fetch of getting first set of cacheData. It is up to you to capture this error and determine how to show it to the user. It's best practice that when there is an error here, you will dismiss a loading UI if you are showing one since [finishedFirstFetch] was called before.
     */
    fun finishedFirstFetch(errorDuringFetch: Throwable?)
}