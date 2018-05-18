package com.levibostian.teller.datastate.listener

import java.util.*

interface OnlineDataStateCacheListener<in DATA> {
    fun cacheEmpty()
    fun cacheData(data: DATA, fetched: Date)
}