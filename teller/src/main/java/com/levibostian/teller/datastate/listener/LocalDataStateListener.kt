package com.levibostian.teller.datastate.listener

interface LocalDataStateListener<in DATA> {
    fun isEmpty()
    fun data(data: DATA)
}