package com.levibostian.teller.datastate

import java.util.*

interface OnlineDataStateListener<in DATA> {
    fun firstFetchOfData()
    fun finishedFirstFetchOfData(errorDuringFetch: Throwable?)
    fun isEmpty()
    fun data(data: DATA, fetched: Date)
    fun fetchingFreshData()
    fun finishedFetchingFreshData(errorDuringFetch: Throwable?)
}