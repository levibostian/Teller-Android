package com.levibostian.tellerexample.job

import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator

class JobCreator: JobCreator {

    override fun create(tag: String): Job? {
        return when (tag) {
            RepositorySyncJob.TAG -> { RepositorySyncJob() }
            else -> null
        }
    }

}