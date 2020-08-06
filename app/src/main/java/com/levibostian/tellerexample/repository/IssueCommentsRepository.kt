package com.levibostian.tellerexample.repository

import androidx.paging.PagedList
import androidx.paging.toObservable
import com.levibostian.teller.error.ServerNotAvailableException
import com.levibostian.teller.error.UnknownHttpResponseError
import com.levibostian.teller.repository.TellerPagingRepository
import com.levibostian.teller.repository.TellerRepository
import com.levibostian.teller.type.Age
import com.levibostian.tellerexample.extensions.transformMapSuccess
import com.levibostian.tellerexample.model.IssueCommentModel
import com.levibostian.tellerexample.model.db.AppDatabase
import com.levibostian.tellerexample.service.GitHubService
import com.levibostian.tellerexample.service.provider.SchedulersProvider
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Note the use of [PagedList] as the Cache type. This is because this example is using the Android Jetpack paging library. If you're not using the Jetpack paging library, you can use [List] or something equivalent. Whatever datatype your cache is in on the device.
 */
class IssueCommentsRepository(private val service: GitHubService,
                              private val db: AppDatabase,
                              private val schedulersProvider: SchedulersProvider) : TellerPagingRepository<PagedList<IssueCommentModel>, IssueCommentsRepository.PagingRequirements, IssueCommentsRepository.Requirements, List<IssueCommentModel>>(PagingRequirements()) {

    companion object {
        /**
         * The default page size returned from the GitHub API is 30.
         */
        private const val PAGE_SIZE: Int = 30
    }

    /**
     * Tells Teller how old cache can be before it's determined "too old" and new cache is fetched automatically by calling `fetchFreshCache()`.
     */
    override var maxAgeOfCache: Age = Age(1, Age.Unit.DAYS)

    /**
     * Convenient property that is populated after fetching a page of data from the network API. This property tells us if there are more pages of data to retrieve from the API or not.
     */
    private var morePagesDataToLoad: Boolean = true

    override fun fetchFreshCache(requirements: Requirements, pagingRequirements: PagingRequirements): Single<FetchResponse<List<IssueCommentModel>>> {
        return service.listIssueComments(requirements.githubUsername, requirements.repoName, requirements.issueNumber, pagingRequirements.pageNumber)
                .transformMapSuccess { httpResult ->
                    when (httpResult.statusCode) {
                        in 500..600 -> FetchResponse.fail(ServerNotAvailableException("The GitHub API is down. Please, try again later."))
                        in 200..300 -> {
                            val successfulBody = FetchResponse.success(httpResult.responseBody!!.map { IssueCommentModel.from(it, requirements.githubUsername, requirements.repoName, requirements.issueNumber) })

                            /**
                             * According to GitHub's API documents, https://developer.github.com/v3/guides/traversing-with-pagination/, the `Link` in the response header tells us if there is another page of data to retrieve or not.
                             */
                            morePagesDataToLoad = httpResult.responseHeaders.get("Link")!!.contains("rel=\"next\"")

                            successfulBody
                        }
                        else -> {
                            // I do not like when apps say, "Unknown error. Please try again". It's terrible to do. But if it ever happens, that means you need to handle more HTTP status codes. Above are the only ones that I know GitHub will return. They don't document the rest of them, I don't think?
                            FetchResponse.fail(UnknownHttpResponseError("Unknown error. Please, try again."))
                        }
                    }
                }
    }

    /**
     * Nothing special here. We simply save the cache somehow. If a cache exists already, this will be appended to it.
     */
    override fun saveCache(cache: List<IssueCommentModel>, requirements: Requirements, pagingRequirements: PagingRequirements) {
        db.reposDao().insertIssueComments(cache)
    }

    /**
     * Where you observe the cache. Nothing special here. Because Teller helps you with deleting old cache, you can feel free to observe your full cache.
     */
    override fun observeCache(requirements: Requirements, pagingRequirements: PagingRequirements): Observable<PagedList<IssueCommentModel>> {
        return db.reposDao().observeIssueCommentsForRepo(requirements.githubUsername, requirements.repoName, requirements.issueNumber)
                .toObservable(
                        PAGE_SIZE,
                        boundaryCallback = PagedListBoundaryCallback())
                .subscribeOn(schedulersProvider.io())
    }

    override fun isCacheEmpty(cache: PagedList<IssueCommentModel>, requirements: Requirements, pagingRequirements: PagingRequirements): Boolean = cache.isEmpty()

    /**
     * When using paging and a cache together, there are scenarios when you should delete old pages of cache. Teller takes care of determining when to delete old pages of data, but it's up to you to perform the actual deletion.
     *
     * @param persistFirstPage When true, delete the old cache that is beyond the first page cache. When false, go ahead and delete the full cache, including the first page.
     */
    override fun deleteOldCache(requirements: Requirements, persistFirstPage: Boolean): Completable {
        return Completable.fromCallable {
            db.reposDao().getIssueCommentsForRepo(requirements.githubUsername, requirements.repoName, requirements.issueNumber).let { allComments ->
                val commentsToDelete = if (persistFirstPage) allComments.subList(PAGE_SIZE, allComments.size) else allComments

                db.reposDao().deleteIssueComments(commentsToDelete)
            }
        }.subscribeOn(schedulersProvider.io())
    }

    /**
     * Convenient function to call when the user has scrolled to the end of the RecyclerView list. Here, we are setting a new value for the [pagingRequirements]. When this new property is set, Teller will automatically attempt to fetch the next page of cache.
     */
    private fun goToNextPage() {
        if (morePagesDataToLoad) {
            val currentPage = pagingRequirements.pageNumber
            pagingRequirements = PagingRequirements(currentPage + 1)
        }
    }

    data class Requirements(val githubUsername: String, val repoName: String, val issueNumber: Int) : TellerRepository.GetCacheRequirements {
        override var tag = "Issue comments for repo $githubUsername/$repoName, number $issueNumber"
    }

    /**
     * Because we are using the GitHub API, we use a page number to request pages of data from the GitHub API. Depending on the API you are working with, you may use a different set of requirements.
     *
     * @param pageNumber Defaulted to 1 as that's the value for the first page of data.
     */
    data class PagingRequirements(val pageNumber: Int = 1) : TellerPagingRepository.PagingRequirements

    /**
     * Android Jetpack's BoundaryCallback that is called when the user has scrolled to the end of the RecyclerView and we can navigate to the next page of data.
     *
     * *Note: If you're not using the Jetpack paging library, you need to detect on your own when the RecyclerView has been scrolled to the end. When it has, call [goToNextPage] on your repository.*
     */
    inner class PagedListBoundaryCallback : PagedList.BoundaryCallback<IssueCommentModel>() {

        override fun onItemAtEndLoaded(itemAtEnd: IssueCommentModel) {
            this@IssueCommentsRepository.goToNextPage()
        }

    }

}