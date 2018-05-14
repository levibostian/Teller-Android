package com.levibostian.teller.datastate

interface LocalDataStateListener<in DATA> {
    fun isEmpty()
    fun data(data: DATA)
    fun error(error: Throwable)
}