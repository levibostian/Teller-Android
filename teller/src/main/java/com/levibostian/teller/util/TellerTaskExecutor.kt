package com.levibostian.teller.util

import android.os.Handler
import android.os.Looper

internal class TellerTaskExecutor: TaskExecutor {

    override fun postUI(block: () -> Unit) {
        Handler(Looper.getMainLooper()).post(block)
    }

}