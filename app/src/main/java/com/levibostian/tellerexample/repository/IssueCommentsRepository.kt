package com.levibostian.tellerexample.repository

import androidx.paging.Config
import androidx.paging.PagedList
import androidx.paging.toObservable
import com.levibostian.teller.error.ServerNotAvailableException
import com.levibostian.teller.error.UnknownHttpResponseError
import com.levibostian.teller.repository.OnlinePagingRepository
import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.teller.type.Age
import com.levibostian.tellerexample.model.IssueCommentModel
import com.levibostian.tellerexample.model.db.AppDatabase
import com.levibostian.tellerexample.service.GitHubService
import com.levibostian.tellerexample.service.provider.SchedulersProvider
import com.levibostian.tellerexample.type.IsLastPagedListItem
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

class IssueCommentsRepository(private val service: GitHubService,
                              private val db: AppDatabase,
                              private val schedulersProvider: SchedulersProvider): OnlinePagingRepository<PagedList<IssueCommentModel>, IssueCommentsRepository.PagingRequirements, IssueCommentsRepository.Requirements, List<IssueCommentModel>>(PagingRequirements()) {

    companion object {
        private const val PAGE_SIZE: Int = 30
    }

    /**
     * Tells Teller how old cache can be before it's determined "too old" and new cache is fetched automatically by calling `fetchFreshCache()`.
     */
    override var maxAgeOfCache: Age = Age(1, Age.Unit.DAYS)

    private var morePagesDataToLoad: Boolean = true

    override fun fetchFreshCache(requirements: Requirements, pagingRequirements: PagingRequirements): Single<FetchResponse<List<IssueCommentModel>>> {
        return service.listIssueComments(requirements.githubUsername, requirements.repoName, requirements.issueNumber, pagingRequirements.pageNumber)
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

    override fun saveCache(cache: List<IssueCommentModel>, requirements: Requirements, pagingRequirements: PagingRequirements) {
        db.reposDao().insertIssueComments(cache)
    }

    override fun observeCache(requirements: Requirements, pagingRequirements: PagingRequirements): Observable<PagedList<IssueCommentModel>> {
        return db.reposDao().observeIssueCommentsForRepo(requirements.githubUsername, requirements.repoName, requirements.issueNumber)
                .toObservable(
                        PAGE_SIZE,
                        boundaryCallback = PagedListBoundaryCallback())
                .subscribeOn(schedulersProvider.io())
    }

    override fun isCacheEmpty(cache: PagedList<IssueCommentModel>, requirements: Requirements, pagingRequirements: PagingRequirements): Boolean = cache.isEmpty()

    override fun persistOnlyFirstPage(requirements: Requirements): Completable {
        return Completable.fromCallable {
            db.reposDao().getIssueCommentsForRepo(requirements.githubUsername, requirements.repoName, requirements.issueNumber).let { allComments ->
                db.reposDao().deleteIssueComments(allComments.subList(PAGE_SIZE, allComments.size))
            }
        }.subscribeOn(schedulersProvider.io())
    }

    private fun goToNextPage() {
        val currentPage = pagingRequirements.pageNumber
        pagingRequirements = PagingRequirements(currentPage + 1)
    }

    data class Requirements(val githubUsername: String, val repoName: String, val issueNumber: Int): OnlineRepository.GetCacheRequirements {
        override var tag = "Issue comments for repo $githubUsername/$repoName, number $issueNumber"
    }

    data class PagingRequirements(val pageNumber: Int = 1): OnlinePagingRepository.PagingRequirements

    inner class PagedListBoundaryCallback: PagedList.BoundaryCallback<IssueCommentModel>() {

        override fun onItemAtEndLoaded(itemAtEnd: IssueCommentModel) {
            if (morePagesDataToLoad) this@IssueCommentsRepository.goToNextPage()
        }

    }

}