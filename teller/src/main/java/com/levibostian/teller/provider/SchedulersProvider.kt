package com.levibostian.teller.provider

import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers

internal interface SchedulersProvider {

    fun io(): Scheduler
    fun main(): Scheduler

}