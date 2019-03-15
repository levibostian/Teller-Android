# Observe an OnlineRepository

In your `Fragment`, `Activity`, `View`, `ViewModel` subclasses that you plan to display the cache, follow the below quick steps:

* Create an instance of your Teller Repository you created:

```kotlin
val reposRepository = ReposRepository(service, database)
```

* Observe the current state of the Repository in the UI of your app. This is the typical way that you would observe an RxJava `Observable` by subscribing to it: 

```kotlin
reposRepository.observe()
     .subscribe { reposState ->
        
    }
```

* Whenever you are ready, specify what cache Teller should read by setting the `requirements` on your `OnlineRepository` subclass:

```kotlin
val usernameOfGitHubUserToLoadReposFor = "levibostian"
reposRepository.requirements = ReposRepository.GetReposRequirements(usernameOfGitHubUserToLoadReposFor)
```

!> Even if your `OnlineRepository.GetCacheRequirements` subclass does not have any parameters, you still need to set the `requirements` object in your `OnlineRepository` instance to instruct Teller to begin.

* Lastly, let's have Teller help us to parse the `reposState` object and help us understand the current state of our cache. 

```kotlin
reposRepository.observe()
    .subscribe { reposState ->
        reposState.apply {
            // You can evaluate the properties of `OnlineCacheState` to perform actions when they are set. Here, we handle *all* fetch errors.
            fetchError?.let { fetchError ->
                // If fetch error occurred during an update cache fetch or fetch for the first cache. 
            }

            // Or, use one of the handy functions below when a cache exists and when it does not. 
            // This function tells us that a cache has never successfully been fetched before. 
            whenNoCache { isFetching, errorDuringFetch ->
                // GitHub repos have never been fetched successfully for this GitHub user before.    
                // This is a great place to show an "empty view" to your users. Perhaps a button for them to try a network fetch again if `errorDuringFetch` is not null? 
                // Also, make sure to check `isFetching` to determine if there is currently a network fetch happening to fetch the first cache successfully. 
            }        
            // This function tells us that a cache has successfully been fetched before. 
            whenCache { cache, lastSuccessfulFetch, isFetching, isFetching, justSuccessfullyFetched, errorDuringFetch ->
                if (cache == null) {
                    // Cache has been fetched successfully before, but it is empty. There are not GitHub repositories for this user.
                } else {
                    // Cache has been fetched successfully before, and it's available as the `cache` parameter! Those are the GitHub repositories for the GitHub user you asked for!
                }

                // `lastSuccessfulFetch` tells you how old the empty cache is. It's recommended to show this in the UI of your app so the user knows how old the cache they are looking at is. 
                // `isFetching` indicates cache exists, but is too old *or* you manually triggered a call to `ReposRepository.refresh()`.
            }
        }
    }
```

How easy is that? Teller tells you if the cache has been fetched before, if it's currently fetching, how old the cache is, and more! All you need to take care of is populating the UI of your app now depending on the state of the cache!

`OnlineCacheState` has *a lot* of properties, and a couple callback functions available you can use. Check out [the Javadoc](/javadoc/teller-android/com.levibostian.teller.cachestate/-online-cache-state/index.html) to learn about all of the options available to you for displaying the state of the cache in the UI.

?> It is important to inform your users how old their cache is. Please, **use the `fetched` property to your advantage**! Tell the user in the UI that their cache is "Last updated 25 hours ago", "Last updated 2 minutes ago". Android provides some handy utility functions to print a human readable date to your users. Check out [DateUtils](https://developer.android.com/reference/android/text/format/DateUtils) where you will find functions you can use such as: `DateUtils.getRelativeTimeSpanString(fetched.time, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS))`

?> As noted above, the `errorDuringFetch` Object is the `Throwable` returned from your `OnlineRepository.fetchFreshCache()` call. This means that you have full control over what the `errorDuringFetch` value is. Please, **use this to your advantage**! Example: If there was no Internet connection or a socket timeout in your fetch call, return a custom `BadInternetConnection` exception from `OnlineRepository.fetchFreshCache()`. If the user entered a bad value in an `EditText`, return a custom `UserEnteredBadValue` exception from `OnlineRepository.fetchFreshCache()`. You can then parse `errorDuringFetch` to determine what error it was for and show an error message to your user that **is actually helpful** rather then a generic, "Error found. Try again".

* Lastly, as you are used to with using RxJava, make sure to call `dispose()` on your `LocalRepository` instance when you are done with it. 

```kotlin
override fun onDestroy() {
    super.onDestroy()

    reposRepository.dispose()
}
```

## What if I want to manually refresh the cache?

Teller automatically takes care of fetching fresh cache for your `OnlineRepository`s when you set the `requirements` property on them. But what if you want to update the cache manually? What if your user opens the app and you want to show them fresh cache? What if the user pushes the refresh button in your app or swipes down on a RecyclerView expecting to see their freshest cache?

Easy. 

All you need to do is call `refresh(force = true)` on your `OnlineRepository`. 

```kotlin
reposRepository.refresh(force = true)
    .observeOn(AndroidSchedulers.main())
    .subscribe { refreshResult ->
        // The refresh has been completed. Parse the `refreshResult` to see if it was successful or not. 
    }
```

When calling `refresh()`, Teller will fetch fresh cache from the network and save the newly fetched cache if successful. The same process that it does automatically but this time you are ignoring the value set for `maxAge`.

### What's next? 

Congrats, you are using Teller! 

The next step is to, well, keep building your app. Go write some code! 

....but if you decide you would rather do more reading, I have a couple recommendations. 

Learn about another awesome library, [Wendy](https://github.com/levibostian/Wendy-Android), to help you build fast, offline-first mobile apps. 

Learn about some of the [advanced features of Teller](beyond_the_basics). Specifically [how to clear Teller data](clear) when your user logs out of the app, [how to keep your OnlineRepository cache up-to-date in the background](refresh) of your app.

If you find Teller helpful and want to be a part of it's positive community, consider [contributing your skills](contribute) to the project! 
