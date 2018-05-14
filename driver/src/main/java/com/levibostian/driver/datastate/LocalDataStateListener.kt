package com.levibostian.driver.datastate

interface LocalDataStateListener<in DATA> {
    fun isEmpty()
    fun data(data: DATA)
    fun error(error: Throwable)
}