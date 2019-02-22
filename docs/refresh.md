# Keep your cache always up-to-date

You want to make sure that the cache of your app is always up-to-date. When your users open your app, it's nice that they can jump right into some new content and not need to wait for a fetch to complete. Teller provides a simple method to refresh your `OnlineRepository`s in the background when your app is not running.

```kotlin
val refreshTasks: ArrayList<Single<OnlineRepository.RefreshResult>> = arrayListOf()

val reposRepositoryRefreshTask = ReposRepository().apply {
    requirements = ReposRepository.GetReposRequirements(usernameOfGitHubUserToLoadReposFor)
}.refresh(force = false)

refreshTasks.add(reposRepository)

val otherOnlineRepository = OtherOnlineRepository().apply {
    requirements = OtherOnlineRepository.GetReposRequirements()
}.refresh(force = false)

refreshTasks.add(otherOnlineRepository)

Single.concat(repos)
    .subscribeOn(Schedulers.io())
    .subscribe()
```

Teller `OnlineRepository.refresh()` will check if any of the following scenarios are true and perform a network fetch call and cache save:

1. `force` parameter is true
2. Cache for the `OnlineRepository` instance is too old
3. The `OnlineRepository` has never fetched cache successfully before

The code above uses `Single.concat()` from RxJava to take a list of `Single` observables and runs each one 1 by 1. 

The code snippet above is handy to run in the `onCreate()` function of the `Activity` that launches when your app opens. It is also recommended to use an Android background task runner to run the refresh even when your app is not running. 

Teller recommends using the [android-job](https://github.com/evernote/android-job) library to run background tasks periodically. Google is working hard on [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager), but until it is out of Beta, you may encounter issues while using it. 

Here is some example [android-job](https://github.com/evernote/android-job) code to get you up and running!

* Be sure to [download and install](https://github.com/evernote/android-job#download) the `android-job` library in your Android project. 

* Create a `Job` Object used to run the Teller refresh tasks: 

```kotlin
import com.evernote.android.job.Job
import com.levibostian.tellerexample.repository.ReposRepository
import com.evernote.android.job.JobRequest
import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.tellerexample.MainApplication
import com.levibostian.tellerexample.repository.GitHubUsernameRepository
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
        val refreshTasks: ArrayList<Single<OnlineRepository.RefreshResult>> = arrayListOf()

        val reposRepositoryRefreshTask = ReposRepository().apply {
            requirements = ReposRepository.GetReposRequirements(usernameOfGitHubUserToLoadReposFor)
        }.refresh(force = false)

        refreshTasks.add(reposRepository)

        val otherOnlineRepository = OtherOnlineRepository().apply {
            requirements = OtherOnlineRepository.GetReposRequirements()
        }.refresh(force = false)

        refreshTasks.add(otherOnlineRepository)

        Single.concat(repos)
            .subscribeOn(Schedulers.io())
            .subscribe()

        return Result.SUCCESS
    }

}
```

Be sure to check out the code in the `scheduleJob()` function. It is the code that tells `android-job` when to run this background job. The code in the example above states that (1) the device must be connected to the Internet (2) run periodically every 15 minutes (remember, Teller does not refresh your `OnlineRepository` every 15 minutes, it will only refresh when it needs to). Because you can run your background job every 15 minutes, it's very important to make sure that your `OnlineRepository.maxAge` value is not too low such as every 30 minutes. That may drain the battery of your user's device.

* Create a `JobCreator` Object as required by `android-job`:

```kotlin
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
```

* Lastly, initialize `android-job` and schedule your refresh job to run. Do this in your `Application`:

```kotlin   
import android.app.Application
import android.content.Context
import com.evernote.android.job.JobManager
import com.levibostian.teller.Teller

class MainApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        // Make sure to initialize Teller first so that the Repositories can initialize to refresh!
        Teller.init(this)

        JobManager.create(this).addJobCreator(JobCreator())
        RepositorySyncJob.scheduleJob()
    }

}
```