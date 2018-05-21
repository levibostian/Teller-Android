package com.levibostian.tellerexample

import android.app.Application
import android.content.Context
import com.evernote.android.job.JobManager
import com.levibostian.teller.Teller
import com.levibostian.tellerexample.job.JobCreator
import com.levibostian.tellerexample.job.RepositorySyncJob

class MainApplication: Application() {

    companion object {
        @JvmStatic lateinit var appContext: Context
    }

    override fun onCreate() {
        super.onCreate()

        appContext = this

        Teller.init(this)

        JobManager.create(this).addJobCreator(JobCreator())
        RepositorySyncJob.scheduleJob()
    }

}