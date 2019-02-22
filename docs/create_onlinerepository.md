# Create OnlineRepository

* The first step to creating a repository is to decide what type of data will be saved and queried in our `OnlineRepository`? Perhaps a SQLite/Room/Realm Model? A `List` of Models? A `File`? `Image`? 

After you decide, create a subclass of `Teller`'s `OnlineRepository` with the data type you choose as the generic parameter. *Do not worry about compile errors yet. Continue reading this page to get everything working*.

In the example below, we will create a `OnlineRepository` to store a `List` of GitHub repositories for a given GitHub user. When the user of our app closes our app and comes back into it later, we can display to them a cached list of GitHub repositories instantly instead of having to perform a network fetch to the GitHub API!

```kotlin
class ReposRepository(): OnlineRepository<List<RepoModel>>() {
}
```

* One reason Teller is so helpful is that it works hard to keep your cache always up-to-date. When the cache is a certain length of time that you specify as "too old", Teller will automatically attempt to fetch fresh cache from the network to get updated date to replace the device's cache.

It's up to you to decide how old is "too old" for your cache.

In our example of `ReposRepository`, I would say that 1 week is too old as people do not create new repositories all too often. 

In your `OnlineRepository` subclass, override the `maxAge` property:

```kotlin
class ReposRepository(): OnlineRepository<List<RepoModel>>() {
    override var maxAgeOfCache: Age = AgeUnit(7, Age.Unit.DAYS)
}
```

* An important piece of Teller is the `GetCacheRequirements` object. With our `ReposRepository` example, we store all of the GitHub repositories for a given GitHub user. When it comes time to load a list of repositories in the app, how is Teller supposed to know who's GitHub repositories to load? That is where `GetCacheRequirements` comes in. *You* use a `GetCacheRequirements` subclass to determine what cache to query and display.

Let's say that you *identify* a List of GitHub repositories by the GitHub username of the person. An example of a `GetCacheRequirements` would be:

```kotlin
class GetReposRequirements(val githubUsername: String): GetCacheRequirements
```

Now, in the `OnlineRepository` abstract functions such as `saveCache()` and `observeCache()`, you can use the passed in `GetReposRequirements` parameter to save the cache or observe the cache depending on the `githubUsername` passed in! *Keep reading, we will get to implementing these functions*

Your turn. Inside of your `OnlineRepository` subclass, (1) create a `GetRequirements` subclass (2) add that subclass class as the 2nd generic parameter of `OnlineRepository`:

```kotlin
class ReposRepository(): OnlineRepository<List<RepoModel>, ReposRepository.GetReposRequirements>() {

    class GetReposRequirements(val githubUsername: String): GetCacheRequirements

}
```

...We are not quite done with `GetRequirements` yet. Teller does not only use your `GetRequirements` subclass to identify what cache to query, it also uses the same Object to identify what cache to fetch from the network.

In our case of `ReposRepository`, the GitHub API [endpoint to get repositories for a user](https://developer.github.com/v3/repos/#list-user-repositories) only requires the username of the GitHub user in order to fetch cache. The means that our `GetReposRequirements(val githubUsername: String)` will work just the way it is. If the API endpoint required more parameters such a user ID, paging number, or other fields, add those properties to your `GetCacheRequirements` subclass. ([docs on pagination](paging)).

Now that we have added all of the properties to our `GetCacheRequirements` subclass that we need to *save, query, and fetch* we need to create a tag. In order for Teller to keep your cache always up-to-date, it needs to be able to *identify* the cache. This is done via the `GetCacheRequirements.tag`.

The tag is simple. It's just a tag that **identifies the fetch request** for the cache.

Here are some examples of `GetCacheRequirements` subclasses with their appropriate tag.

```kotlin
// The fetch request gets all of the repositories for a given user. 
class GetReposRequirements(val githubUsername: String): GetCacheRequirements {
    override var tag = "Repos for user:$githubUsername"
}

// The fetch request gets a chunk of Tweets for a given user's Twitter profile and page number
// Because the tag identifies the **fetch request**, it is important to specify the page number for pagination in the tag!
class GetTweetsForUserProfile(val username: String, val pagingNumber: Int): GetCacheRequirements {
    override var tag = "Tweets for user profile:$username, page number:$pagingNumber"
}

// The fetch request gets the currently logged in user profile. 
class GetMyProfile(): GetCacheRequirements {
    override var tag = "My profile" 
}
```

Go ahead, create your `GetCacheRequirements.tag`

* Speaking of fetching cache from a network, what is the cache type returned from your network call? Perhaps a database Model? A List of database Models? A POJO deserialized from moshi?

Whatever it is, specify that as the 3rd and final generic parameter of your `OnlineRepository` subclass: 

```kotlin
class ReposRepository(): OnlineRepository<List<RepoModel>, ReposRepository.GetReposRequirements, List<RepoModel>>() {
}
```

In our example `ReposRepository`, the GitHub API returns back a list of repositories. So, the cache type returned from the network call would be `List<RepoModel>` which happens to be the same thing as what is queried.

* Lastly, let's implement the abstract methods of `OnlineRepository`. They are each explained below: 

```kotlin
class ReposRepository(): OnlineRepository<List<RepoModel>, ReposRepository.GetReposRequirements, List<RepoModel>>() {

    /**
     * Fetch fresh cache via a network call.
     * Note: This function is guaranteed to be called on a background thread. 
     * 
     * You are in charge of performing any error handling and parsing of the network response body.
     *
     * After the network call, you tell Teller if the fetch was successful or a failure. Do this by returning a `FetchResponse` object.  
     * 
     * If the network call was successful, Teller will save the new cache and deliver it to the listeners. If it failed, Teller will deliver your error to the listeners so you can notify your users of errors if you wish.
     */
    override fun fetchFreshCache(requirements: GetReposRequirements): Single<FetchResponse<List<RepoModel>>> {
    }

    /**
     * Save new cache to the local Android device.
     * Note: This function is guaranteed to be called on a background thread.
     *
     * Teller is not opinionated about how you save your new cache. Use SQLite/Room/Realm, SharedPreferences, Files, Images, whatever!
     */
    override fun saveCache(cache: List<RepoModel>, requirements: GetReposRequirements) {
    }

    /**
     * Return a RxJava2 `Observable` that continuously queries the locally cache and notifies any observers of changes.
     * Note: This function is guaranteed to be called on the main UI thread.
     *
     * Do *not* send `null` in the `Observable` returned from this function. This is a rule set by RxJava and it will throw an exception.
     * If your cache is in an "empty" state, either (1) return an empty value (Example: an empty String, "") or (2) create a POJO with an optional inside of it.
     */
    override fun observeCache(requirements: GetReposRequirements): Observable<List<RepoModel>> {
    }

    /**
     * Help Teller determine is your cache is "empty" or not.
     * Note: This function is guaranteed to be called on the same thread as `observeCache()`.
     *
     * In our example, an empty List, is what we qualify as empty. If you have a POJO, String, or any other type, you return back if the `cache` parameter is empty or not.
     */
    override fun isCacheEmpty(cache: List<RepoModel>, requirements: GetReposRequirements): Boolean = cache.isEmpty()

}
```

Now that you have an idea of what each function is responsible for doing, here is a full example of an `OnlineRepository` that fetches a list of GitHub repositories from the GitHub API, parses the response, saves and queries cache from a database:

```kotlin   
class ReposRepository(private val service: GitHubService,
                      private val db: AppDatabase): OnlineRepository<List<RepoModel>, ReposRepository.GetReposRequirements, List<RepoModel>>() {

    /**
     * Tells Teller how old cache can be before it's determined "too old" and new cache is fetched automatically by calling `fetchFreshCache()`.
     */
    override var maxAgeOfCache: Age = Age(7, Age.Unit.DAYS)

    /**
     * Fetch fresh cache via a network call.
     * Note: This function is guaranteed to be called on a background thread.
     *
     * You are in charge of performing any error handling and parsing of the network response body.
     *
     * After the network call, you tell Teller if the fetch was successful or a failure. Do this by returning a `FetchResponse` object.
     *
     * If the network call was successful, Teller will save the new cache and deliver it to the listeners. If it failed, Teller will deliver your error to the listeners so you can notify your users of errors if you wish.
     */
    override fun fetchFreshCache(requirements: GetReposRequirements): Single<FetchResponse<List<RepoModel>>> {
        return service.listRepos(requirements.githubUsername)
                .map { response ->
                    val fetchResponse: FetchResponse<List<RepoModel>>
                    if (!response.isSuccessful) {
                        fetchResponse = when (response.code()) {
                            in 500..600 -> {
                                FetchResponse.fail("The GitHub API is down. Please, try again later.")
                            }
                            404 -> {
                                FetchResponse.fail("The githubUsername ${requirements.githubUsername} does not exist. Try another one.")
                            }
                            else -> {
                                // I do not like when apps say, "Unknown error. Please try again". It's terrible to do. But if it ever happens, that means you need to handle more HTTP status codes. Above are the only ones that I know GitHub will return. They don't document the rest of them, I don't think?
                                FetchResponse.fail("Unknown error. Please, try again.")
                            }
                        }
                    } else {
                        fetchResponse = FetchResponse.success(response.body()!!)
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
    override fun saveCache(cache: List<RepoModel>, requirements: GetReposRequirements) {
        db.reposDao().insertRepos(cache)
    }

    /**
     * Return a RxJava2 `Observable` that continuously queries the local cache and notifies any observers of changes.
     * Note: This function is guaranteed to be called on the main UI thread.
     *
     * Do *not* send `null` in the `Observable` returned from this function. This is a rule set by RxJava and it will throw an exception.
     * If your cache is in an "empty" state, either (1) return an empty value (Example: an empty String, "") or (2) create a POJO with an optional inside of it.
     */
    override fun observeCache(requirements: GetReposRequirements): Observable<List<RepoModel>> {
        return db.reposDao().observeReposForUser(requirements.githubUsername).toObservable()
    }

    /**
     * Help Teller determine is your cache is "empty" or not.
     * Note: This function is guaranteed to be called on the same thread as `observeCache()`.
     *
     * In our example, an empty List, is what we qualify as empty. If you have a POJO, String, or any other type, you return back if the `cache` parameter is empty or not.
     */
    override fun isCacheEmpty(cache: List<RepoModel>, requirements: GetReposRequirements): Boolean = cache.isEmpty()

    class GetReposRequirements(val githubUsername: String): GetCacheRequirements {
        override var tag = "Repos for user:$githubUsername"
    }

}
```

### What's next? 

All you need to do now is display the cache in the UI of your app. Teller takes care of everything else for you!

Let's learn how to [observe your newly created `OnlineRepository` in the UI](observe_onlinerepository) of your app!
