package com.levibostian.teller.datastate.listener

import java.util.*

interface OnlineDataStateCacheListener<in CACHE> {
    fun cacheEmpty(fetched: Date)
    fun cacheData(data: CACHE, fetched: Date)
}