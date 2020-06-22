package com.levibostian.tellerexample.job

import android.preference.PreferenceManager
import com.evernote.android.job.Job
import com.levibostian.tellerexample.repository.ReposRepository
import com.evernote.android.job.JobRequest
import com.levibostian.teller.repository.TellerRepository
import com.levibostian.tellerexample.MainApplication
import com.levibostian.tellerexample.service.provider.AppSchedulersProvider
import com.levibostian.tellerexample.util.DependencyUtil
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
        val repos: ArrayList<Single<TellerRepository.RefreshResult>> = arrayListOf()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val latestUsername = sharedPreferences.getString("githubUsernameKey", null)
        if (latestUsername != null) repos.add(
                ReposRepository(DependencyUtil.serviceInstance(), DependencyUtil.dbInstance(MainApplication.appContext), AppSchedulersProvider()).apply {
                    requirements = ReposRepository.GetReposRequirements(latestUsername)
                }.refresh(false)
        )

        Single.concat(repos)
                .subscribeOn(Schedulers.io())
                .subscribe()

        return Result.SUCCESS
    }

}
