package com.levibostian.teller.datastate.listener

interface OnlineDataStateFirstFetchListener {
    fun firstFetchOfData()
    /**
     * @param errorDuringFetch Error that occurred during the fetch of getting first set of cacheData. It is up to you to capture this error and determine how to show it to the user. It's best practice that when there is an error here, you will dismiss a loading UI if you are showing one since [firstFetchOfData] was called before.
     */
    fun finishedFirstFetchOfData(errorDuringFetch: Throwable?)
}