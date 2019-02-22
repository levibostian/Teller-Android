# Paging 

Teller works nicely with if your `OnlineRepository` is working with a network API that uses [paging/pagination](https://en.wikipedia.org/wiki/Pagination). Here are a couple of tips and some example code to build an `OnlineRepository` that will help do the job. 

* Create an `OnlineRepository` that is built to work with a paging network API. This `OnlineRepository` subclass looks like a regular `OnlineRepository` you [have created before](create_onlinerepository) with a couple small additions. 

Add a new parameter to your `OnlineRepository.GetCacheRequirements` instance to take in a page number.

```kotlin
class GetReposRequirements(val githubUsername: String, val pageNumber: Int = 0): GetCacheRequirements {
    override var tag = "Repos for user:$githubUsername, pageNumber:$pageNumber"
}
```

To determine what parameter to add to your `OnlineRepository.GetCacheRequirements` subclass is difficult to say. It all depends on how the network API is created. Here are some examples.

[GitHub's search code API endpoint](https://developer.github.com/v3/search/) uses a page number technique: 

```kotlin
class SearchCodeRequirements(val searchQuery: String, val pageNumber: Int = 0): GetCacheRequirements {
    override var tag = "Search code for repo. Query:$searchQuery, pageNumber:$pageNumber"
}
```

[SoundCloud has a few endpoints that use pagination](https://developers.soundcloud.com/docs/api/guide#pagination). SoundCloud will return a `next_href` property in the JSON response if there is more cache to fetch, or it will not exist if you have reached the end.

```kotlin
class GetTracksRequirements(val soundcloudUserId: Int, 
                            val linkedPartitioning: Int? = null, 
                            val nextHref: String? = null): GetCacheRequirements {
    // `linkedPartitioning` represents the page number and `soundcloudUserId` represents the user.
    override var tag = "Tracks for user. User ID:$soundcloudUserId, pageNumber:${linkedPartitioning ?: "0"}"
}
```

[Twitter's API uses 2 properties for pagination](https://developer.twitter.com/en/docs/basics/cursoring.html) that they like to call "cursoring". 

```kotlin
class GetTimelineTweetsRequirements(val previousCursor: Long? = null, 
                                    val nextCursor: Long? = null): GetCacheRequirements {
    // `previousCursor` and `nextCursor` represents the paged chunk of cache we fetched.
    override var tag = "My timeline of tweets. Next page:${nextCursor ?: "0"}, previous page:${previousCursor ?: "0"}"
}
```

Try to think to yourself, "What pieces of information identify this unique chunk of cache?". Whatever that may be is how you need to construct the `OnlineRepository.GetCacheRequirements.tag`.

* The next step is to incorporate paging into the rest of the `OnlineRepository` subclass. 

This can be pretty straightforward. 

```kotlin
class SearchCodeRepository(private val service: GitHubRetrofitService,
                           private val db: AppDatabase): OnlineRepository<SearchCodeFetchResponse, SearchCodeRepository.GetReposRequirements, SearchCodeFetchResponse>() {

    override var maxAgeOfCache: Age = Age(7, Age.Unit.DAYS)

    companion object {
        const val PAGE_SIZE = 50
    }

    override fun fetchFreshCache(requirements: SearchCodeRequirements): Single<FetchResponse<SearchCodeFetchResponse>> {
        return service.searchCode(requirements.searchQuery, pageNumber = requirements.pageNumber)
                .map { response ->
                    val fetchResponse: FetchResponse<SearchCodeFetchResponse>
                    if (!response.isSuccessful) {
                        fetchResponse = when (response.code()) {
                            in 500..600 -> {
                                FetchResponse.fail("The GitHub API is down. Please, try again later.")
                            }                        
                            else -> {
                                // I do not like when apps say, "Unknown error. Please try again". It's terrible to do. But if it ever happens, that means you need to handle more HTTP status codes. Above are the only ones that I know GitHub will return. They don't document the rest of them, I don't think?
                                FetchResponse.fail("Unknown error. Please, try again.")
                            }
                        }
                    } else {         
                        // Here, I am specifying if more pages are available or not.                
                        val areMorePagesAvailable = response.body()!!.incomplete_results
                        this.requirements.morePagesAvailable = areMorePagesAvailable                        
                        val items = response.body()!!.items
                        
                        fetchResponse = FetchResponse.success(SearchCodeFetchResponse(items, areMorePagesAvailable))                        
                    }

                    fetchResponse
                }
    }

    override fun saveCache(cache: SearchCodeFetchResponse, requirements: SearchCodeRequirements) {
        db.dao().insertSearchCodeResults(cache.searchCodeItems)
    }

    override fun observeCache(requirements: SearchCodeRequirements): Observable<SearchCodeFetchResponse> {
        val offset = requirements.pageNumber * PAGE_SIZE
        return db.dao().observeSearchedCodeResults(requirements.searchQuery, limit = PAGE_SIZE, offset = offset).toObservable()
            .map { codeResultsList ->
                val morePagesAreAvailable = codeResultsList.count() >= PAGE_SIZE || requirements.morePagesAvailable

                SearchCodeFetchResponse(morePagesAreAvailable, codeResultsList)
            }
    }

    override fun isCacheEmpty(cache: SearchCodeFetchResponse, requirements: SearchCodeRequirements): Boolean = cache.searchCodeItems.isEmpty()

    fun getToNextPage() {
        if (this.requirements == null) throw RuntimeException("Cannot go to next page if you have not even looked at the first one.")

        this.requirements = SearchCodeRequirements(requirements.searchQuery,
                                                   requirements.pageNumber += 1)
    }

    fun getToPreviousPage() {
        if (this.requirements == null) throw RuntimeException("Cannot go to next page if you have not even looked at the first one.")
        if (this.requirements.pageNumber <= 0) throw RuntimeException("Already on the first page. Cannot go to previous one.")

        this.requirements = SearchCodeRequirements(requirements.searchQuery,
                                                  requirements.pageNumber -= 1)
    }

    class SearchCodeRequirements(val searchQuery: String, 
                                 val pageNumber: Int = 0, 
                                 var morePagesAvailable: Boolean = true): GetCacheRequirements {
        override var tag = "Search code for repo. Query:$searchQuery, pageNumber:$pageNumber"
    }

    // We use a custom POJO to represent the search results because we want to show in our UI if more pages are available or not. 
    // When your user scrolls in a RecyclerView and they hit the bottom, you need to know if you should show a loading view as you load another page of cache or not. This property will indicate that.
    data class SearchCodeFetchResponse(val searchCodeItems: List<SearchResultModel>,
                                       val morePagesAvailable: Boolean)

}
```

The above code has a few interesting additions. 

1. The addition of a POJO, `SearchCodeFetchResponse` that is the response Object of the `fetchFreshCache()` function as well as the Object we return back from `observeCached()`. The reason a POJO is used here is because we need to send more information to the UI then just the list of code search results. We need to know if there is more cache available in paging or not!

View the documentation for the API that you are using to learn how to determine if more cache is available or not in paging. Each API is unique.

2. The `fetchFreshCache()` passes in the `SearchCodeRequirements.pageNumber` property into the API request to determine what page number to get.

3. `getToNextPage()` and `getToPreviousPage()` functions have been added as convenient ways for the UI to go to the next page. When the user scrolls to the bottom of the RecyclerView, for example, and the cache Object, `SearchCodeFetchResponse` says `morePagesAvailable`, then go ahead and attempt to load it!

4. `observeCached()` is querying the database for a limit of `PAGE_SIZE` and an offset. This is important to do as it is what allows Teller to keep the cache up-to-date.

Say you had an `OnlineRepository` that fetched 200 GitHub repositories. 4 separate fetch calls, 50 repositories in each page chunk. If the user of your app closes the app, reopens it, your `OnlineRepository` loads all 200 repositories in `observeCache()`, your user scrolls to the bottom of the list, Teller is instructed to load page number 2 of cache (which is chunks 51-100 repositories) and updates those in the local database. What about repositories 101-200? Your user will think that cache is fresh. Teller will never be instructed to ever fetch that cache.

By having a page size and limit implemented, it allows your UI to control the loading and fetching of Teller. As the user scrolls, you can detect when the user has reached the top of bottom of the RecyclerView list which then triggers the loading of a previous or next page of cache in Teller. This allows Teller to always keep cache up-to-date.

!> To go along with the explanation above, make sure to set the `PAGE_SIZE` value to the page size found in the API documentation or set it to a value you are certain is smaller then the page size found in the API documentation. As long as `PAGE_SIZE` <= actual page size returned from the API, then Teller will make sure that all of your cache is up-to-date.

* Lastly, you need to detect when the bottom of the RecyclerView is reached so that you know when to load the next chunk of cache.

This is out of the scope of this documentation as there are many ways to accomplish this. However, it is recommended to check out the [Android Jetpack Paging library](https://developer.android.com/topic/libraries/architecture/paging/). 

This documentation will not go over the Paging library, but here is some example code that you may find helpful while reading through the [Paging library documentation](https://developer.android.com/topic/libraries/architecture/paging/)]. 

Here is an example `OnlineRepository` using `PagedList`s and a `BoundaryCallback`:

```kotlin
import androidx.paging.PagedList
import androidx.paging.RxPagedListBuilder
import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.teller.type.Age
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.Single

class SearchCodeRepository(private val service: GitHubRetrofitService,
                           private val db: Database,
                           private val boundaryCallbackFactory: SearchCodeRepositoryPagedListBoundaryCallback): OnlineRepository<SearchCode, SearchCodeRepository.SearchCodeRequirements, SearchCodeFetchResponse>() {

    companion object {
        const val PAGE_SIZE = 50
    }

    override var maxAgeOfCache: Age = Age(5, Age.Unit.MINUTES)

    override fun fetchFreshCache(requirements: SearchCodeRequirements): Single<FetchResponse<SearchCodeFetchResponse>> {
        return service.searchCode(requirements.searchQuery, pageNumber = requirements.pageNumber)
                .map { response ->
                    val fetchResponse: FetchResponse<SearchCodeFetchResponse>
                    if (!response.isSuccessful) {
                        fetchResponse = when (response.code()) {
                            in 500..600 -> {
                                FetchResponse.fail("The GitHub API is down. Please, try again later.")
                            }
                            else -> {
                                // I do not like when apps say, "Unknown error. Please try again". It's terrible to do. But if it ever happens, that means you need to handle more HTTP status codes. Above are the only ones that I know GitHub will return. They don't document the rest of them, I don't think?
                                FetchResponse.fail("Unknown error. Please, try again.")
                            }
                        }
                    } else {
                        // Here, I am specifying if more pages are available or not.
                        val areMorePagesAvailable = response.body()!!.incomplete_results
                        this.requirements.morePagesAvailable = areMorePagesAvailable
                        val items = response.body()!!.items

                        fetchResponse = FetchResponse.success(SearchCodeFetchResponse(items, areMorePagesAvailable))
                    }

                    fetchResponse
                }
    }

    override fun isCacheEmpty(cache: SearchCode, requirements: SearchCodeRequirements): Boolean = cache.searchCodeItems.isEmpty()

    override fun observeCache(requirements: SearchCodeRequirements): Observable<SearchCode> {
        return RxPagedListBuilder<Int, StatWithGamePlayers>(db.dao().observeSearchedCodeResults(), PAGE_SIZE)
                .setBoundaryCallback(boundaryCallbackFactory.build(this))
                .buildFlowable(BackpressureStrategy.LATEST)
                .distinctUntilChanged()
                .toObservable()
                .map { codeResultsList ->
                    val morePagesAreAvailable = codeResultsList.count() >= PAGE_SIZE || requirements.morePagesAvailable

                    SearchCode(morePagesAreAvailable, codeResultsList)
                }
    }

    override fun saveCache(cache: SearchCodeFetchResponse, requirements: SearchCodeRequirements) {
        db.dao().insertSearchCodeResults(cache.searchCodeItems)
    }

    fun getToNextPage() {
        if (this.requirements == null) throw RuntimeException("Cannot go to next page if you have not even looked at the first one.")

        this.requirements = SearchCodeRequirements(requirements.searchQuery,
                                                   requirements.pageNumber += 1)
    }

    fun getToPreviousPage() {
        if (this.requirements == null) throw RuntimeException("Cannot go to next page if you have not even looked at the first one.")
        if (this.requirements.pageNumber <= 0) throw RuntimeException("Already on the first page. Cannot go to previous one.")

        this.requirements = SearchCodeRequirements(requirements.searchQuery,
                                                  requirements.pageNumber -= 1)
    }    

    class SearchCodeRequirements(val searchQuery: String,
                                 val pageNumber: Int = 0,
                                 var morePagesAvailable: Boolean = true): GetCacheRequirements {
        override var tag = "Search code for repo. Query:$searchQuery, pageNumber:$pageNumber"
    }
    
    // POJO returned from Observable for the UI to receive. Uses a `PagedList` instead of a `List`
    data class SearchCode(val searchCodeItems: PagedList<SearchResultModel>,
                          val morePagesAvailable: Boolean)
    
    // POJO returned from fetch response. Uses a `List` instead of a `PagedList`
    data class SearchCodeFetchResponse(val searchCodeItems: List<SearchResultModel>,
                                       val morePagesAvailable: Boolean)

}
```

Here is a `PagedList.BoundaryCallback` example for the `OnlineRepository` above. 

```kotlin
import androidx.paging.PagedList

class SearchCodeRepositoryPagedListBoundaryCallback(private val repository: SearchCodeRepository): PagedList.BoundaryCallback<SearchCodeRepository.SearchCode>() {

    override fun onItemAtEndLoaded(itemAtEnd: SearchCodeRepository.SearchCode) {
        if (itemAtEnd.morePagesAvailable) repository.getToNextPage()
    }

    class Factory() {
        fun build(repo: SearchCodeRepository): SearchCodeRepositoryPagedListBoundaryCallback = SearchCodeRepositoryPagedListBoundaryCallback(repo)
    }

}
```

By using the Paging library, it takes care of loading pages of cache from the database using Teller, displaying that information using a `PagedListAdapter`, then when the end of the list has been reached it will tell the Teller repository to go to the next page if there is another page available.

Awesome! That was easy. 
