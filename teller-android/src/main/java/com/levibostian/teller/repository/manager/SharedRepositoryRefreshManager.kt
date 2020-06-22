package com.levibostian.teller.repository.manager

import com.levibostian.teller.repository.GetCacheRequirementsTag
import com.levibostian.teller.repository.TellerRepository
import com.levibostian.teller.repository.DataSourceFetchResponse
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.ReplaySubject
import java.lang.ref.WeakReference

/**
 * Thread safe, singleton, implementation of [RepositoryRefreshManager].
 *
 * The reason for using a singleton manager is so that [TellerRepository.fetchFreshCache] calls are shared amongst *all* instances of [TellerRepository]. This improves the performance of Teller by guaranteeing that only 1 network call is ever being performed for each unique [GetCacheRequirementsTag].
 */
internal object SharedRepositoryRefreshManager: RepositoryRefreshManager {

    private var refreshItems: MutableMap<GetCacheRequirementsTag, RefreshTaskItem> = mutableMapOf()

    /**
     * Get a [Single] you can observe to get notified on the result of a fetch network call.
     *
     * [task] will begin if another [Single] does not already exist for [tag]. If a [Single] already exists, [task] will be ignored as it's assumed to be a duplicate of the returned [Single] since the [tag] is identical.
     *
     * Note: No matter what you do with the returned [Single] (set the thread to subscribe on, dispose of Single, etc), the [task] will not be impacted. Because [task] may be shared between 1+ instances of [TellerRepository], we do not want to cancel [task]. We also want to guarantee that [TellerRepository.fetchFreshCache] is called on a background thread so this manager is in charge of beginning the fetch request.
     *
     * Note: If this function is called multiple times by the same [repository] with same [tag] while a [task] is still not complete, you will receive the same return value.
     *
     * @param task The actual fetch task from [TellerRepository.fetchFreshCache].
     * @param tag Used to label the [task]. [tag]s can be used multiple times by different [repository] instances.
     * @param repository Stored as a [WeakReference] to reference later to send updates to and to cancel task if desired.
     *
     * @return Observable to get notified with the refresh task result (unless you cancel before then).
     */
    override fun <RefreshResponseType: DataSourceFetchResponse> refresh(task: Single<TellerRepository.FetchResponse<RefreshResponseType>>, tag: GetCacheRequirementsTag, repository: RepositoryRefreshManager.Listener): Single<TellerRepository.RefreshResult> {
        return synchronized(refreshItems) {
            var refreshItem = refreshItems[tag]

            if (refreshItem == null) {
                /**
                 * It's ok that we are starting the refresh task here without adding a repository until later because we have a lock to [refreshItems] so even if the refresh task ends instantly, it cannot do anything until the lock is unlocked.
                 */
                refreshItem = RefreshTaskItem(startRefreshTask(task, tag), arrayListOf())
                refreshItems[tag] = refreshItem
            }

            var repositoryItems = refreshItem.repositories.find { it.listener.get() == repository }
            if (repositoryItems == null) {
                repositoryItems = Repository(ReplaySubject.create<TellerRepository.RefreshResult>(), WeakReference(repository))

                repository.refreshBegin(tag) // since refresh has already begun, send listener update now.

                refreshItem.repositories.add(repositoryItems)
            }

            repositoryItems.refreshTaskStatus.singleOrError()
        }
    }

    /**
     * Modify the given fetch task with some observer listeners and start running it.
     */
    private fun <RefreshResponseType: DataSourceFetchResponse> startRefreshTask(task: Single<TellerRepository.FetchResponse<RefreshResponseType>>, tag: GetCacheRequirementsTag): Disposable {
        fun doneRefresh(result: TellerRepository.RefreshResult?, failure: Throwable?) {
            val repositories = refreshItems[tag]?.repositories

            failure?.let { failure ->
                repositories?.forEach {
                    it.refreshTaskStatus.onError(failure)
                }
            } ?: kotlin.run {
                repositories?.forEach {
                    it.refreshTaskStatus.onNext(result!!)
                }
            }
            repositories?.forEach {
                it.refreshTaskStatus.onComplete()
            }

            refreshItems.remove(tag)
        }

        return task.doOnSuccess { response ->
            synchronized(refreshItems) {
                updateRepositoryListeners(tag) { listener ->
                    listener.refreshComplete(tag, response)
                }

                val responseFailure = response.failure
                if (responseFailure != null) {
                    doneRefresh(TellerRepository.RefreshResult.failure(responseFailure), null)
                } else {
                    doneRefresh(TellerRepository.RefreshResult.success(), null)
                }
            }
        }.doOnError { error ->
            synchronized(refreshItems) {
                doneRefresh(null, error)
            }
        }.subscribeOn(Schedulers.io()).subscribe()
    }

    private fun updateRepositoryListeners(tag: GetCacheRequirementsTag, update: (listener: RepositoryRefreshManager.Listener) -> Unit) {
        refreshItems[tag]?.repositories?.forEach { repository ->
            repository.listener.get()?.let { update.invoke(it) }
        }
    }

    /**
     * Useful for when [TellerRepository.requirements] is changed and the previous [TellerRepository.fetchFreshCache] call (if there was one) should be cancelled as a new [TellerRepository.fetchFreshCache] could be triggered.
     *
     * If the [repository] is not found in the list of added owners for [tag], the request will be ignored.
     *
     * If there are 0 [RepositoryRefreshManager.Listener]s after removing the [repository] for the given [tag], the actual fetch call will be cancelled for performance gain. If there is 1+ other [RepositoryRefreshManager.Listener]s that are still using the fetch request with the given [tag], the actual [TellerRepository.fetchFreshCache] will continue but the given [repository] will not get a notification on updates.
     *
     * The [Single] returned from [refresh] will receive a [TellerRepository.RefreshResult.SkippedReason.CANCELLED] event on cancel.
     */
    override fun cancelTasksForRepository(tag: GetCacheRequirementsTag, repository: RepositoryRefreshManager.Listener) {
        synchronized(refreshItems) {
            refreshItems[tag]?.let { refreshItem ->
                // Must use iterator to avoid ConcurrentModificationException
                val refreshRepositoriesIterator = refreshItem.repositories.iterator()
                while (refreshRepositoriesIterator.hasNext()) {
                    val existingOwner = refreshRepositoriesIterator.next()
                    val ownerRef = existingOwner.listener.get()
                    if (ownerRef == null || ownerRef == repository) {
                        existingOwner.refreshTaskStatus.apply {
                            onNext(TellerRepository.RefreshResult.skipped(TellerRepository.RefreshResult.SkippedReason.CANCELLED))
                            onComplete()
                        }
                        refreshRepositoriesIterator.remove() // Remove existingOwner.
                    }
                }

                if (refreshItem.repositories.isEmpty()) {
                    refreshItem.taskDisposable.apply {
                        if (!isDisposed) dispose()
                    }

                    refreshItems.remove(tag)
                }
            }
        }
    }

    /**
     * Represents a refresh network call task.
     *
     * Because 1+ [TellerRepository]s can use a refresh call, this object maps each 1 refresh call to the list of [TellerRepository]s.
     */
    private data class RefreshTaskItem(val taskDisposable: Disposable,
                                       val repositories: ArrayList<Repository>)

    /**
     * Represents an [TellerRepository] and the Observable used to get notified on the status of the completion of a refresh call.
     *
     * Each [TellerRepository] has it's own [refreshTaskStatus] instance because each [TellerRepository] can call [cancelTasksForRepository]. [SharedRepositoryRefreshManager]'s implementation of [cancelTasksForRepository] will send a [TellerRepository.RefreshResult.SkippedReason.CANCELLED] event to the [refreshTaskStatus] but keep all other [TellerRepository]s using the same refresh task undisturbed.
     *
     * Why use a Subject? In this manager, we are using `Single`s for observing the `refresh()` result. We are using a subject to update the `refresh()` status because we can call `onNext()` directly on it in the manager instance.
    How does the Subject need to work? In order to use a Subject as a Single, you need to call `onNext()` followed by `onCompleted()` on the subject in order for the Single.success to be called and delivered to the observers. If you only call complete(), you will receive an RxError "Event error(Sequence doesn't contain any elements.)". If you only call onNext(), nothing will happen.
    Why a ReplaySubject? Well, when you call `refresh()` and receive an instance of this `subject.asSingle()`, that Single instance may not be subscribed to right away. Especially in a multi threaded environment, there may be a delay for the subscribe to complete. Therefore, it is possible for the observer of `subject.asSingle()` to subscribe between (or after) the `subject.onNext()` call and the `subject.complete` call. As stated already, if you only receive the `complete` call, you will receive an RxError for sequence not containing any elements.
    To prevent this scenario, we use a ReplaySubject. That way when an observer subscribes to the Single, they will be guaranteed to not receive the RxError as it will or will not receive a `Single.success` call.
     */
    private data class Repository(val refreshTaskStatus: ReplaySubject<TellerRepository.RefreshResult>,
                                  val listener: WeakReference<RepositoryRefreshManager.Listener>)

}