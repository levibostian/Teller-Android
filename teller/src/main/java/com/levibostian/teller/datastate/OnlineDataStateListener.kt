package com.levibostian.teller.datastate

import java.util.*

interface OnlineDataStateListener<in DATA> {
    fun firstFetchOfData()
    fun isEmpty()
    fun data(data: DATA, fetched: Date)
    fun error(error: Throwable)
    fun fetchingFreshData()
    fun finishedFetchingFreshData(errorDuringFetch: Throwable?)
}