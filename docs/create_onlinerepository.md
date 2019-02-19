# Create OnlineRepository

* The first step to creating a repository is to decide what type of data will be saved and queried in our `OnlineRepository`? Perhaps a SQLite/Room/Realm Model? A `List` of Models? A `File`? `Image`? 

After you decide, create a subclass of `Teller`'s `OnlineRepository` with the data type you choose as the generic parameter. *Do not worry about compile errors yet. Continue reading this page to get everything working*. 

In the example below, we will create a `OnlineRepository` to store a `List` of GitHub repositories for a given GitHub user. When the user of our app closes our app and comes back into it later, we can display to them a cached list of GitHub repositories instantly instead of having to perform a network fetch to the GitHub API!

```kotlin
class ReposRepository(): OnlineRepository<List<RepoModel>>() {
}
```

* One reason Teller is so helpful is that it works hard to keep your cached data always up-to-date. When the cached data is a certain length of time that you specify as "too old", Teller will automatically attempt to fetch fresh data from the network to get updated date to replace the device's cache. 

It's up to you to decide how old is "too old" for your data type's cached data. 

In our example of `ReposRepository`, I would say that 1 week is too old as people do not create new repositories all too often. 

In your `OnlineRepository` subclass, override the `maxAgeOfData` property: 

```kotlin
class ReposRepository(): OnlineRepository<List<RepoModel>>() {
    override var maxAgeOfData: AgeOfData = AgeOfData(7, AgeOfData.Unit.DAYS)
}
```

* An important piece of Teller is the `GetDataRequirements` object. With our `ReposRepository` example, we store all of the GitHub repositories for a given GitHub user. When it comes time to load a list of repositories in the app, how is Teller supposed to know who's GitHub repositories to load? That is where `GetDataRequirements` comes in. *You* use a `GetDataRequirements` subclass to determine what cache data to query and display. 

Let's say that you *identify* a List of GitHub repositories by the GitHub username of the person. An example of a `GetDataRequirements` would be: 

```kotlin
class GetReposRequirements(val githubUsername: String): GetDataRequirements
```

Now, in the `OnlineRepository` abstract functions such as `saveData()` and `observeCachedData()`, you can use the passed in `GetReposRequirements` parameter to save the data or observe the cached data depending on the `githubUsername` passed in! *Keep reading, we will get to implementing these functions*

Your turn. Inside of your `OnlineRepository` subclass, (1) create a `GetRequirements` subclass (2) add that subclass class as the 2nd generic parameter of `OnlineRepository`:

```kotlin
class ReposRepository(): OnlineRepository<List<RepoModel>, ReposRepository.GetReposRequirements>() {

    class GetReposRequirements(val githubUsername: String): GetDataRequirements

}
```

...We are not quite done with `GetRequirements` yet. Teller does not only use your `GetRequirements` subclass to identify what data to query, it also uses the same Object to identify what data to fetch from the network. 

In our case of `ReposRepository`, the GitHub API [endpoint to get repositories for a user](https://developer.github.com/v3/repos/#list-user-repositories) only requires the username of the GitHub user in order to fetch data. The means that our `GetReposRequirements(val githubUsername: String)` will work just the way it is. If the API endpoint required more data such a user ID, paging number, or other fields, add those properties to your `GetDataRequirements` subclass. ([docs on pagination](paging)).

Now that we have added all of the properties to our `GetDataRequirements` subclass that we need to *save, query, and fetch* we need to create a tag. In order for Teller to keep your cached data always up-to-date, it needs to be able to *identify* the cached data. This is done via the `GetDataRequirements.tag`. 

The tag is simple. It's just a tag that **identifies the fetch request** for the data type. 

Here are some examples of `GetDataRequirements` subclasses with their appropriate tag. 

```kotlin
// The fetch request gets all of the repositories for a given user. 
class GetReposRequirements(val githubUsername: String): GetDataRequirements {
    override var tag = "Repos for user:$githubUsername"
}

// The fetch request gets a chunk of Tweets for a given user's Twitter profile and page number
// Because the tag identifies the **fetch request**, it is important to specify the page number for pagination in the tag!
class GetTweetsForUserProfile(val username: String, val pagingNumber: Int): GetDataRequirements {
    override var tag = "Tweets for user profile:$username, page number:$pagingNumber"
}

// The fetch request gets the currently logged in user profile. 
class GetMyProfile(): GetDataRequirements {
    override var tag = "My profile" 
}
```

Go ahead, create your `GetDataRequirements.tag`

* Speaking of fetching data from a network, what is the data type returned from your network call? Perhaps a database Model? A List of database Models? A POJO deserialized from moshi?

Whatever it is, specify that as the 3rd and final generic parameter of your `OnlineRepository` subclass: 

```kotlin
class ReposRepository(): OnlineRepository<List<RepoModel>, ReposRepository.GetReposRequirements, List<RepoModel>>() {
}
```

In our example `ReposRepository`, the GitHub API returns back a list of repositories. So, the data type returned from the network call would be `List<RepoModel>` which happens to be the same thing as what is queried. 

* Lastly, let's implement the abstract methods of `OnlineRepository`. They are each explained below: 

```kotlin
class ReposRepository(): OnlineRepository<List<RepoModel>, ReposRepository.GetReposRequirements, List<RepoModel>>() {

    /**
     * Fetch fresh data via a network call. 
     * Note: This function is guaranteed to be called on a background thread. 
     * 
     * You are in charge of performing any error handling and parsing of the network response body.
     *
     * After the network call, you tell Teller if the fetch was successful or a failure. Do this by returning a `FetchResponse` object.  
     * 
     * If the network call was successful, Teller will cache the data and deliver it to the listeners. If it failed, Teller will deliver your error to the listeners so you can notify your users of errors if you wish. 
     */
    override fun fetchFreshData(requirements: GetReposRequirements): Single<FetchResponse<List<RepoModel>>> {        
    }

    /**
     * Save cache data to the local Android device.
     * Note: This function is guaranteed to be called on a background thread.
     *
     * Teller is not opinionated about how you save your new cache data. Use SQLite/Room/Realm, SharedPreferences, Files, Images, whatever!
     */
    override fun saveData(data: List<RepoModel>, requirements: GetReposRequirements) {
    }

    /**
     * Return a RxJava2 `Observable` that continuously queries the locally cached data and notifies any observers of changes.
     * Note: This function is guaranteed to be called on the main UI thread.
     *
     * Do *not* send `null` in the `Observable` returned from this function. This is a rule set by RxJava and it will throw an exception.
     * If your cache data is in an "empty" state, either (1) return an empty value (Example: an empty String, "") or (2) create a POJO with an optional inside of it.
     */
    override fun observeCachedData(requirements: GetReposRequirements): Observable<List<RepoModel>> {
    }

    /**
     * Help Teller determine is your cached data is "empty" or not.
     * Note: This function is guaranteed to be called on the same thread as `observeCachedData()`.
     *
     * In our example, an empty List, is what we qualify as empty. If you have a POJO, String, or any other data type, you return back if the `cache` parameter is empty or not.
     */
    override fun isDataEmpty(cache: List<RepoModel>, requirements: GetReposRequirements): Boolean = cache.isEmpty()

}
```

Now that you have an idea of what each function is responsible for doing, here is a full example of an `OnlineRepository` that fetches a list of GitHub repositories from the GitHub API, parses the response, saves and queries data from a database:

```kotlin   
class ReposRepository(private val service: GitHubService,
                      private val db: AppDatabase): OnlineRepository<List<RepoModel>, ReposRepository.GetReposRequirements, List<RepoModel>>() {

    /**
     * Tells Teller how old cached data can be before it's determined "too old" and new data is fetched automatically by calling `fetchFreshData()`.
     */
    override var maxAgeOfData: AgeOfData = AgeOfData(7, AgeOfData.Unit.DAYS)

    /**
     * Fetch fresh data via a network call.
     * Note: This function is guaranteed to be called on a background thread.
     *
     * You are in charge of performing any error handling and parsing of the network response body.
     *
     * After the network call, you tell Teller if the fetch was successful or a failure. Do this by returning a `FetchResponse` object.
     *
     * If the network call was successful, Teller will cache the data and deliver it to the listeners. If it failed, Teller will deliver your error to the listeners so you can notify your users of errors if you wish.
     */
    override fun fetchFreshData(requirements: GetReposRequirements): Single<FetchResponse<List<RepoModel>>> {
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
     * Save cache data to the local Android device.
     * Note: This function is guaranteed to be called on a background thread.
     *
     * Teller is not opinionated about how you save your new cache data. Use SQLite/Room/Realm, SharedPreferences, Files, Images, whatever!
     */
    override fun saveData(data: List<RepoModel>, requirements: GetReposRequirements) {
        db.reposDao().insertRepos(data)
    }

    /**
     * Return a RxJava2 `Observable` that continuously queries the locally cached data and notifies any observers of changes.
     * Note: This function is guaranteed to be called on the main UI thread.
     *
     * Do *not* send `null` in the `Observable` returned from this function. This is a rule set by RxJava and it will throw an exception.
     * If your cache data is in an "empty" state, either (1) return an empty value (Example: an empty String, "") or (2) create a POJO with an optional inside of it.
     */
    override fun observeCachedData(requirements: GetReposRequirements): Observable<List<RepoModel>> {
        return db.reposDao().observeReposForUser(requirements.githubUsername).toObservable()
    }

    /**
     * Help Teller determine is your cached data is "empty" or not.
     * Note: This function is guaranteed to be called on the same thread as `observeCachedData()`.
     *
     * In our example, an empty List, is what we qualify as empty. If you have a POJO, String, or any other data type, you return back if the `cache` parameter is empty or not.
     */
    override fun isDataEmpty(cache: List<RepoModel>, requirements: GetReposRequirements): Boolean = cache.isEmpty()

    class GetReposRequirements(val githubUsername: String): GetDataRequirements {
        override var tag = "Repos for user:$githubUsername"
    }

}
```

### What's next? 

All you need to do now is display the cached data in the UI of your app. Teller takes care of everything else for you! 

Let's learn how to [observe your newly created `OnlineRepository` in the UI](observe_onlinerepository) of your app!
