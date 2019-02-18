package com.levibostian.teller.provider

import android.os.Looper
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers

internal interface SchedulersProvider {

    fun io(): Scheduler
    fun main(): Scheduler

}