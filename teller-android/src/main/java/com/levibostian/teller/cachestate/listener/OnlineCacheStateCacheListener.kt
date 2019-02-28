package com.levibostian.teller.cachestate.listener

import java.util.*

interface OnlineCacheStateCacheListener<in CACHE> {
    fun cacheEmpty(fetched: Date)
    fun cache(cache: CACHE, fetched: Date)
}