package com.levibostian.teller.util

internal interface TaskExecutor {
    fun postUI(block: () -> Unit)
}