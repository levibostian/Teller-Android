package com.levibostian.teller.cachestate.listener

interface LocalCacheStateListener<in DATA> {
    fun isEmpty()
    fun cache(cache: DATA)
}