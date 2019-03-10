package com.levibostian.tellerexample.service.provider

import io.reactivex.Scheduler

interface SchedulersProvider {
    fun io(): Scheduler
    fun mainThread(): Scheduler
}