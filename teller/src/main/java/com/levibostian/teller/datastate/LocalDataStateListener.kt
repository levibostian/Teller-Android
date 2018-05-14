package com.levibostian.teller.datastate

import java.util.*

interface LocalDataStateListener<in DATA> {
    fun isEmpty()
    fun data(data: DATA, fetched: Date)
    fun error(error: Throwable)
}