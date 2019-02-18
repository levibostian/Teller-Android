package com.levibostian.tellerexample.job

import android.app.Application
import com.evernote.android.job.Job
import com.levibostian.teller.Teller
import com.levibostian.tellerexample.repository.ReposRepository
import android.content.ContentValues.TAG
import android.os.UserManager
import com.evernote.android.job.JobRequest
import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.tellerexample.MainApplication
import com.levibostian.tellerexample.repository.GitHubUsernameRepository
import com.levibostian.tellerexample.util.DependencyUtil
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit


class RepositorySyncJob: Job() {

    companion object {
        const val TAG = "RepositorySyncJob_job_tag"

        fun scheduleJob() {
            JobRequest.Builder(RepositorySyncJob.TAG)
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .setRequirementsEnforced(true)
                    .setUpdateCurrent(true)
                    .setPeriodic(TimeUnit.MINUTES.toMillis(15), TimeUnit.MINUTES.toMillis(5))
                    .build()
                    .schedule()
        }
    }

    override fun onRunJob(params: Params): Result {
        val repos: ArrayList<Single<OnlineRepository.RefreshResult>> = arrayListOf()

        val latestUsername = GitHubUsernameRepository(MainApplication.appContext).currentUsernameSaved
        if (latestUsername != null) repos.add(
                ReposRepository(DependencyUtil.serviceInstance(), DependencyUtil.dbInstance(MainApplication.appContext)).apply {
                    requirements = ReposRepository.GetRequirements(latestUsername)
                }.refresh(false)
        )

        Single.concat(repos)
                .subscribeOn(Schedulers.io())
                .subscribe()

        return Result.SUCCESS
    }

}
