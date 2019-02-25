package com.levibostian.teller.provider

import android.os.Looper
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import io.reactivex.android.schedulers.AndroidSchedulers

internal class TellerSchedulersProvider: SchedulersProvider {

    override fun io(): Scheduler = Schedulers.io()

    override fun main(): Scheduler = AndroidSchedulers.mainThread()

}