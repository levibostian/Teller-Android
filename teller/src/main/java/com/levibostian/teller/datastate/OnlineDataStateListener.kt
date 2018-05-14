package com.levibostian.teller.datastate

interface OnlineDataStateListener<in DATA> {
    fun firstFetchOfData()
    fun isEmpty()
    fun data(data: DATA)
    fun error(error: Throwable)
    fun fetchingFreshData()
    fun finishedFetchingFreshData(errorDuringFetch: Throwable?)
}