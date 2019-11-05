# Paging 

When working with a network API, you may encounter the situation of paging (also known as pagination). Paging is required when you are dealing with a large amount of data. Imagine if you asked the Twitter API to give you every tweet in your timeline. Twitter could respond with thousands, hundreds of thousands, or even millions of tweets! That many tweets could result is your app slowing down from having to download and then show that many tweets at a time. To combat this potential problem, pagination exists to help you retrieve chunks (or pages) of data from an API to make your app speedy. 

Need an example? [Open this GitHub webpage](https://github.com/search?q=android) and scroll to the bottom. You will see something that looks like this:

![buttons allowing you to browse to other pages of results from GitHub search](img/paging_page_indicator.png)

GitHub's search returns pages of results where you can browse to other pages of results if you wish. Imagine if Google returned all of your search results to you without paging...

Teller has built-in support for paging so you can easily add caching abilities to your paging API endpoint!

!> Paging is a common scenario to encounter. In fact, [Android Jetpack](https://developer.android.com/jetpack) offers a library [specifically meant for paging](https://developer.android.com/topic/libraries/architecture/paging/) that you can use in your Android app. This library is optional and Teller will work if you decide to use it or not. You can decide on that later. Let's move onto learning about how to implement paging in our app. 

# How to implement paging in an app

For anyone needing to add paging support to their app, you need to follow these steps:

1. Fetch the first page of data from a network API. Display this first page of fetched data to your user. Like if you went to the first page of [GitHub's search results](https://github.com/search?q=android). 
2. In the UI of your app, know when to browse to the next page of data. This can be done through a scroll listener on a RecyclerView that determines when the user has scrolled to the end of the list or this can be done by a button instructing you to go to the next page like the bottom of [GitHub's search results](https://github.com/search?q=android). 
3. Repeat. This process of paging in an app is a simple loop of (1) fetching and displaying pages of data and (2) determining in your UI when to go to the next page. 

## How to implement paging *with a cache* in an app

You're using Teller in your app which means that you want to cache the result of network API calls. For anyone needing to add paging support to their app while also caching the paged data, the process is similar to the steps above with a couple of additions:

1. When the app starts up, check if an existing cache exists. 
  * If a cache exists, show the cache to the user within the app. 
  * If a cache does not exist, fetch the first page of data from a network API. 
2. In the UI of your app, know when to browse to the next page of data. This can be done through a scroll listener on a RecyclerView that determines when the user has scrolled to the end of the list or this can be done by a button instructing you to go to the next page like the bottom of [GitHub's search results](https://github.com/search?q=android). 
3. When you determine it's time to navigate to the next page, fetch the next page of data, append it to the existing cache, and then show the cache to the user. 
4. Repeat. Keep going this process until it's time to navigate away from your app. The user might close the app, might navigate away to another screen, or something else. Either way, you need to do some cleanup. At this point, delete your cache beyond the first page. That way when the user opens up your app again, we are ready to go back to step 1 above and display the first page of cache only. We want to delete this old cache because (1) it's a convenient way to make sure we don't store an infinite amount of old, cached data in our app which may continue to take up storage on our user's device and (2) bugs can easily occur. To prevent bugs and keep our app's speedy, it's best to delete irrelevant cache and start over. 

# Add paging with a cache to your app easily with Teller 

Teller was built with paging in mind. It's easy to get going if you're used to the way Teller works already. To add paging, all it takes are these simple steps:

1. Determine how paging works with the network API you are working with. Every API works differently with how pages of data are requested. Here are some examples.

[GitHub's API](https://developer.github.com/v3/guides/traversing-with-pagination/) uses a page number where you request what page of data you need in the HTTP request: `https://api.github.com/search/code?q=addClass+user:mozilla&page=14` (see the `page=14` query parameter at the end?). So, as long as you have the page number of the last successful HTTP request, that's all you need to make the next request. 

[SoundCloud has a few endpoints that use pagination](https://developers.soundcloud.com/docs/api/guide#pagination). SoundCloud will return a `next_href` property in the JSON response body if there is more cache to fetch. This `next_href` is a URL that you call to get the next page. So, as long as you have the `next_href` URL found in the last successful HTTP request, that's all you need to make the next request. 

[Twitter's API uses 2 properties for pagination](https://developer.twitter.com/en/docs/basics/cursoring.html) that they like to call "cursoring". So, you will need to remember the `previousCursor` and `nextCursor` values from the last successful HTTP request to make the next request. 

?> These 3 examples above are popular techniques amongst APIs. If you are working with an API that follows a technique that is different from the 3 above, [share it with us so we can add examples to these docs](https://github.com/levibostian/teller-android/issues/new). 

Remember your decision. You will need to know this information for implementing the `OnlinePagingRepository.PagingRequirements` subclass. 

2. Decide if you would like to use the [Android Jetpack paging library](https://developer.android.com/topic/libraries/architecture/paging/) or not. If you are using Room and LiveData, this library adds paging support to your RecyclerView with little to no effort. If you're not using Room and LiveData, read the paging library overview to see if it's worth using for your situation.

There are many ways to determine if your user has scrolled to the end of a RecyclerView when not using the Jetpack paging library. Because of that, this document does not cover how to do this. It's up to you to look up a solution that works best for you. 

Remember your decision. You will need to know this for implementing how you query the cache and navigate to new pages of your cache. 

3. Time to bring everything together. Make a new class extending `OnlinePagingRepository`. `OnlinePagingRepository` is almost identical to the `OnlineRepository` [subclass you are familiar with](create_onlinerepository) except it adds a couple small changes specifically to make paging work with no extra work on your part. 

Here is the example paging repository from the Teller example app with comments to help explain it all to you. 

*Note: The below example code uses the Android Jetpack paging library. However, the logic behind the use of the abstract `OnlinePagingRepository` is the same no matter if you use this library or not. Follow the tips in the comments below if you're not using the paging library.*

```kotlin
/**
 * Note the use of [PagedList] as the Cache type. This is because this example is using the Android Jetpack paging library. If you're not using the Jetpack paging library, you can use [List] or something equivalent. Whatever datatype your cache is in on the device. 
 */
class IssueCommentsRepository(private val service: GitHubService,
                              private val db: AppDatabase,
                              private val schedulersProvider: SchedulersProvider) : OnlinePagingRepository<PagedList<IssueCommentModel>, IssueCommentsRepository.PagingRequirements, IssueCommentsRepository.Requirements, List<IssueCommentModel>>(PagingRequirements()) {

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

    data class Requirements(val githubUsername: String, val repoName: String, val issueNumber: Int) : OnlineRepository.GetCacheRequirements {
        override var tag = "Issue comments for repo $githubUsername/$repoName, number $issueNumber"
    }

    /**
     * Because we are using the GitHub API, we use a page number to request pages of data from the GitHub API. Depending on the API you are working with, you may use a different set of requirements.
     *
     * @param pageNumber Defaulted to 1 as that's the value for the first page of data.
     */
    data class PagingRequirements(val pageNumber: Int = 1) : OnlinePagingRepository.PagingRequirements

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
```

4. Now that your repository is made, you can use it like a regular Teller `OnlineRepository`. The only difference is that when your user has scrolled through your RecyclerView and it's time to go to the next page of data, you need to call `goToNextPage()`. 

Oh, and quick tip. If you want to implement swipe-to-refresh into your app to refresh your cache, Teller has made this super easy for you. All you need to do is call the `.refresh(force = true)` on your instance of `OnlinePagingRepository` that you made. This is the same behavior that the `OnlineRepository` subclass offers to perform a pull-to-refresh. Teller takes care of deleting your old cache and resetting your paging number requirements for you. 

#### Jetpack paging library 

*Optional Jetpack paging library steps below. If not using this library, skip this section*

If you are using the Jetpack paging library in your app, you can go ahead and read the official Jetpack paging library documentation to learn how to use the library in your app. To help you get up and running faster with Teller paging, here is some help. 

* For your RecyclerView, extend `PagedListAdapter`. 

```kotlin 
class IssueCommentsRecyclerViewAdapter: PagedListAdapter<IssueCommentModel, IssueCommentsRecyclerViewAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<IssueCommentModel>() {
            override fun areItemsTheSame(old: IssueCommentModel, new: IssueCommentModel): Boolean = old.id == new.id

            override fun areContentsTheSame(old: IssueCommentModel, new: IssueCommentModel): Boolean = old == new
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.repo_name_textview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_repo_recyclerview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = getItem(position)

        holder.nameTextView.text = "${comment?.body ?: "Loading comment..."}"
    }

}
```

* If you're using Room change your DAO to return `DataSource.Factory` instead of what you were doing before for observing your cache. 

```kotlin
@Dao
interface ReposDao {
    @Query("SELECT * FROM issue_comment WHERE github_username = :username AND repo = :repoName AND issue_number = :issueNumber")
    fun observeIssueCommentsForRepo(username: String, repoName: String, issueNumber: Int): DataSource.Factory<Int, IssueCommentModel>
}
```

* Add this to whatever you have already seen from the example `OnlinePagingRepository` subclass above and you're set!
