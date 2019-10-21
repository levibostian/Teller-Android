package com.levibostian.tellerexample.repository

import androidx.paging.PagedList
import androidx.paging.toObservable
import com.levibostian.teller.error.ServerNotAvailableException
import com.levibostian.teller.error.UnknownHttpResponseError
import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.teller.type.Age
import com.levibostian.tellerexample.model.IssueCommentModel
import com.levibostian.tellerexample.model.db.AppDatabase
import com.levibostian.tellerexample.service.GitHubService
import com.levibostian.tellerexample.service.provider.SchedulersProvider
import com.levibostian.tellerexample.type.IsLastPagedListItem
import io.reactivex.Observable
import io.reactivex.Single

class IssueCommentsRepository(private val service: GitHubService,
                              private val db: AppDatabase,
                              private val schedulersProvider: SchedulersProvider): OnlineRepository<PagedList<IssueCommentModel>, IssueCommentsRepository.Requirements, List<IssueCommentModel>>() {

    companion object {
        private const val PAGE_SIZE = 30
    }

    /**
     * Tells Teller how old cache can be before it's determined "too old" and new cache is fetched automatically by calling `fetchFreshCache()`.
     */
    override var maxAgeOfCache: Age = Age(1, Age.Unit.DAYS)
    override val pagingRepository: Boolean = true

    private var morePagesDataToLoad: Boolean = true

    /**
     * Fetch fresh cache via a network call.
     * Note: This function is guaranteed to be called on a background thread.
     *
     * You are in charge of performing any error handling and parsing of the network response body.
     *
     * After the network call, you tell Teller if the fetch was successful or a failure. Do this by returning a `FetchResponse` object.
     *
     * If the network call was successful, Teller will cache the response and deliver it to the listeners. If it failed, Teller will deliver your error to the listeners so you can notify your users of errors if you wish.
     */
    override fun fetchFreshCache(requirements: Requirements): Single<FetchResponse<List<IssueCommentModel>>> {
        return service.listIssueComments(requirements.githubUsername, requirements.repoName, requirements.issueNumber, requirements.pageNumber)
                .map { response ->
                    val fetchResponse: FetchResponse<List<IssueCommentModel>> = if (!response.isSuccessful) {
                        when (response.code()) {
                            in 500..600 -> {
                                FetchResponse.fail(ServerNotAvailableException("The GitHub API is down. Please, try again later."))
                            }
                            else -> {
                                // I do not like when apps say, "Unknown error. Please try again". It's terrible to do. But if it ever happens, that means you need to handle more HTTP status codes. Above are the only ones that I know GitHub will return. They don't document the rest of them, I don't think?
                                FetchResponse.fail(UnknownHttpResponseError("Unknown error. Please, try again."))
                            }
                        }
                    } else {
                        val successfulBody = FetchResponse.success(response.body()!!.map { IssueCommentModel.from(it, requirements.githubUsername, requirements.repoName, requirements.issueNumber) })

                        morePagesDataToLoad = response.headers().get("Link")!!.contains("rel=\"next\"")

                        successfulBody
                    }

                    fetchResponse
                }
    }

    /**
     * Save cache to the local Android device.
     * Note: This function is guaranteed to be called on a background thread.
     *
     * Teller is not opinionated about how you save your new cache. Use SQLite/Room/Realm, SharedPreferences, Files, Images, whatever!
     */
    public override fun saveCache(cache: List<IssueCommentModel>, requirements: Requirements) {
        db.reposDao().insertIssueComments(cache)
    }

    /**
     * Return a RxJava2 `Observable` that continuously queries the local cache and notifies any observers of changes.
     * Note: This function is guaranteed to be called on the main UI thread.
     *
     * Do *not* send `null` in the `Observable` returned from this function. This is a rule set by RxJava and it will throw an exception.
     * If your cache response is in an "empty" state, either (1) return an empty value (Example: an empty String, "") or (2) create a POJO with an optional inside of it.
     */
    public override fun observeCache(requirements: Requirements): Observable<PagedList<IssueCommentModel>> {
        return db.reposDao().observeIssueCommentsForRepo(requirements.githubUsername, requirements.repoName, requirements.issueNumber)
                .toObservable(
                        PAGE_SIZE,
                        boundaryCallback = PagedListBoundaryCallback())
                .subscribeOn(schedulersProvider.io())
    }

    /**
     * Help Teller determine is your cached response is "empty" or not.
     * Note: This function is guaranteed to be called on the same thread as `observeCache()`.
     *
     * In our example, an empty List, is what we qualify as empty. If you have a POJO, String, or any other response type, you return back if the `cache` parameter is empty or not.
     */
    public override fun isCacheEmpty(cache: PagedList<IssueCommentModel>, requirements: Requirements): Boolean = cache.isEmpty()

    private fun goToNextPage() {
        val currentPage = requirements!!.pageNumber
        requirements = requirements!!.copy(pageNumber = currentPage + 1)
    }

    data class Requirements(val githubUsername: String, val repoName: String, val issueNumber: Int, val pageNumber: Int = 1): GetCacheRequirements {
        override var tag = "Issue comments for repo $githubUsername/$repoName, number $issueNumber, page $pageNumber"
    }

    inner class PagedListBoundaryCallback: PagedList.BoundaryCallback<IssueCommentModel>() {

        override fun onItemAtEndLoaded(itemAtEnd: IssueCommentModel) {
            if (morePagesDataToLoad) this@IssueCommentsRepository.goToNextPage()
        }

    }

}