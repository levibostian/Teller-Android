[![Release](https://jitpack.io/v/levibostian/Teller-Android.svg)](https://jitpack.io/#levibostian/Teller-Android)

# Teller

Android library that manages the state of your app's data. Teller facilitates loading cached data and fetching fresh data so your app's data is always up to date.

Teller works very well with MVVM and MVI design patterns (note the use of `Repository` subclasses in the library). However, you do not need to use these design patterns to use it.

![project logo](misc/logo.jpg)

[Read the official announcement of Teller](https://levibostian.com/blog/manage-cached-data-teller/) to learn more about what it does and why to use it.

iOS developer? Check out the [iOS version of Teller](https://github.com/levibostian/teller-ios).

## What is Teller?

The data used in your mobile app: user profiles, a collection of photos, list of friends, etc. *all have state*. Your data is in 1 of many different states:

* Being fetched for the first time (if it comes from an async network call)
* The data is empty
* Data exists in the device's storage (cached).
* During the empty and data states, your app could also be fetching fresh data to replace the cached data on the device that is out of date.

Determining what state your data is in and managing it can be a big pain. That is where Teller comes in. All you need to do is tell Teller how to save your data, query your data, and how to fetch fresh data (probably with a network API call) and Teller facilities everything else for you. Teller will query your cached data, parse it to determine the state of it, fetch fresh data if the cached data is too old, and deliver the state of the data to listeners so you can update the UI to your users.

## Why use Teller?

When creating mobile apps that cache data (such as offline-first mobile apps), it is important to show in your app's UI the state of your cached data to the app user. Telling your app user how old data is, if your app is performing a network call, if there were any errors during network calls, if the data set is empty or not. These are all states that data can be in and notifying your user of these states helps your user trust your app and feel they are in control.

Well, keeping track of the state of your data can be complex. Querying a database is easy. Performing an network API call is easy. Updating the UI of your app is easy. But tying all of that together can be difficult. That is why Teller was created. You take care of querying data, saving data, fetching fresh data via a network call and let Teller take care of everything else.

For example: If you are building a Twitter client app that is offline-first, when the user opens your app you should be showing them a list of cached tweets so that the user has something to interact with and not a loading screen saying "Loading tweets, please wait...". When you show this list of cached tweets, you may also be performing an API network call in the background to fetch the newest tweets for your user. In the UI of your app, you should be notifying your user that your app is fetching fresh tweets or else your user may think your app is broken. Keeping your user always informed about exactly what your app is doing is a good idea to follow. Teller helps you keep track of the state of your data and facilitates keeping it up to date.

Here are the added benefits of Teller:

* Small. The only dependency at this time is RxJava2 ([follow this issue as I work to remove this 1 dependency and make it optional](https://github.com/levibostian/Teller-Android/issues/1))
* Built for Kotlin, by Kotlin. Teller is written in Kotlin which means you can expect a nice to use API.
* Not opinionated. Teller does not care where your data is stored or how it is queried. You simply tell Teller when you're done fetching, saving, and querying and Teller takes care of delivering it to the listeners.
* Teller works very well with MVVM and MVI design patterns (note the use of `Repository` subclasses in the library). However, you do not need to use these design patterns to use it.

# Install

Add this to your root build.gradle at the end of repositories:

```
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

Then, install the Teller module:

```
compile 'com.github.levibostian:teller-android:version-goes-here'
```

The latest release version at this time is: [![Release](https://jitpack.io/v/levibostian/Teller-Android.svg)](https://jitpack.io/#levibostian/Teller-Android)

**Note: Teller is early on in development.** Even though it is used in production in my own apps, it is still early on in development as I use the library more and more, it will mature.

I plan to release the library in an alpha, beta, then stable release phase.

#### Stages:

Pre-alpha (where the library is at currently):

- [X] Build example app on how to use it.
- [X] Documentation for README created.

Alpha:

- [ ] Make non-RxJava version of the library to make it even smaller and more portable.
- [ ] Documentation in form of JavaDoc created.
- [ ] Documentation on how to use in MVVM, MVI setup and other setups as well.
- [ ] Fixup the API for the library if needed.

Beta:

- [ ] Library API has been used enough that the API does not have any huge changes planned for it.
- [ ] Tests written (and passing 😉) for the library.

Stable:

- [ ] Library has been running in many production apps, developers have tried it and given feedback on it.

# Getting started

The steps to get Teller up and running is pretty simple: (1) Initialize Teller. (2) Create a `Repository` subclass for your data set. (3) Add a listener to your `Repository` subclass.

* Initialize Teller in your app's `Application`:

```Kotlin
Teller.init(this)
```

* The next step is where you tell Teller how to query cached data, save data to the cache, and how to fetch fresh data. You do this by creating a subclass of `LocalRepository` or `OnlineRepository`.

What type of `Repository` should you use you ask? Here is a description of each:

...TL;DR...if you need to perform a network call to obtain data, use `OnlineRepository`. Else, `LocalRepository`.

`LocalRepository` is a very simple abstract class that does not require network calls to fetch fresh data. Data is simply saved to a cache and queried. If you need to store data in `SharedPreferences`, for example, `LocalRepository` is the perfect way to do that.

`OnlineRepository` is an abstract class that saves data to a cache, queries data from the cache, and performs network calls to fetch fresh data when data expires. If you have a data set that is obtained from calling your network API, use `OnlineRepository`.

Here is an example of each:

`LocalRepository`

```Kotlin
class GitHubUsernameRepository(private val context: Context): LocalRepository<String>() {

    private val githubUsernameSharedPrefsKey = "${this::class.java.simpleName}_githubUsername_key"
    private val rxSharedPreferences: RxSharedPreferences = RxSharedPreferences.create(PreferenceManager.getDefaultSharedPreferences(context))

    // Save data to a cache. In this case, we are using SharedPreferences to save our data.
    override fun saveData(data: String) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(githubUsernameSharedPrefsKey, data).apply()
    }

    // Using RxJava2 Observables, you query your cached data.
    override fun observeData(): Observable<String> {
        return rxSharedPreferences.getString(githubUsernameSharedPrefsKey, "")
                .asObservable()
                .filter { it.isNotBlank() }
                .subscribeOn(Schedulers.io())
    }

    // Help Teller to determine if data is empty or not. Teller uses this when parsing the cache to determine if a data set is empty or not.
    override fun isDataEmpty(data: String): Boolean = data.isBlank()

}
```

This is a `LocalRepository` that is meant to store a `String` representing a GitHub username. As you can see, this `LocalRepository` uses `SharedPreferences` to store data. You may use whatever type of data storage that you prefer!

`OnlineRepository`

```Kotlin
class ReposRepository(private val service: GitHubService,
                      private val db: AppDatabase): OnlineRepository<List<RepoModel>, ReposRepository.GetRequirements, List<RepoModel>>() {

    // This property tells Teller how old cached data can be before it's determined "too old" and new data is fetched automatically by calling `fetchFreshData()`.
    // If you ever need to manually fetch fresh data and ignore this property, you may do so by calling `.sync(true)` on any `OnlineRepository` subclass to force a sync.
    override var maxAgeOfData: AgeOfData = AgeOfData(1, AgeOfData.Unit.HOURS)

    // Fetch fresh data via a network call. You are in charge of performing any error handling and parsing of the network call body.
    // After the network call, you tell Teller if the fetch was successful or a failure. If successful, Teller will cache the data and deliver it to the listeners. If it fails, Teller will deliver your error to the listeners so you can notify your users of errors if you wish.
    override fun fetchFreshData(requirements: GetRequirements): Single<FetchResponse<List<RepoModel>>> {
        return service.listRepos(requirements.username)
                .map { response ->
                    val fetchResponse: FetchResponse<List<RepoModel>>
                    if (!response.isSuccessful) {
                        fetchResponse = when (response.code()) {
                            in 500..600 -> {
                                FetchResponse.fail("The GitHub API is down. Please, try again later.")
                            }
                            404 -> {
                                FetchResponse.fail("The username ${requirements.username} does not exist. Try another one.")
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

    // Save data to a cache on the device.
    override fun saveData(data: List<RepoModel>) {
        db.reposDao().insertRepos(data)
    }

    // Using RxJava2 Observables, query cached data on the device.
    override fun observeCachedData(requirements: GetRequirements): Observable<List<RepoModel>> {
        return db.reposDao().observeReposForUser(requirements.username).toObservable()
    }

    // Help Teller to determine if data is empty or not. Teller uses this when parsing the cache to determine if a data set is empty or not.
    override fun isDataEmpty(data: List<RepoModel>): Boolean = data.isEmpty()

    class GetRequirements(val username: String): GetDataRequirements {
        override var tag: String = "${this::class.java.simpleName}_$username"
    }

}
```

This `OnlineRepository` subclass is meant to fetch, store, and query a list of GitHub repositories for a given GitHub username. Notice how Teller will even handle errors in your network fetch calls and deliver the errors to the UI of your application for you!

Now it's your turn. Create subclasses of `OnlineRepository` and `LocalRepository` for your data sets!

* The last step. Observe your data set. This is also pretty simple.

`LocalRepository`

```Kotlin
val githubUsernameRepository: GitHubUsernameRepository = GitHubUsernameRepository(context)
githubUsernameRepository.observe()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { usernameState ->
                    usernameState?.deliver(object : LocalDataStateListener<String> {
                        override fun isEmpty() {
                            // The GitHub username data set is currently empty. Update your UI here to tell your user that the GitHub username is empty.
                        }
                        override fun data(data: String) {
                            // The GitHub username has been set! You can use the `data` parameter here as the GitHub username and display it however you wish in your app's UI.
                        }
                    })
                }
```

`OnlineRepository`

```Kotlin
val reposRepository: ReposRepository = ReposRepository(service, database)
reposRepository.observe()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { reposState ->
                    reposState?.deliver(object : OnlineDataStateListener<List<RepoModel>> {
                        override fun finishedFirstFetchOfData(errorDuringFetch: Throwable?) {
                            // The first fetching of a data set has been complete!
                            // If might have succeeded or failed. This is determined by if `errorDuringFetching` is null or not.
                            // You can skip this state if you choose because `cacheEmpty()` or `cacheData()` will be called along with this function. However, this function is here if you optionally decide to give your user a special UI, such as an animation, showing the user their data for the first time.

                            if (errorDuringFetch != null) {
                                // It is a best practice to remove your loading UI, if you displayed one when `firstFetchOfData()` was called, if there was an error so that you do not have an infinite loading UI in your app.
                                errorDuringFetch.message?.let { showEmptyView(it) }

                                AlertDialog.Builder(this@MainActivity)
                                        .setTitle("Error")
                                        .setMessage(errorDuringFetch.message?: "Unknown error. Please, try again.")
                                        .setPositiveButton("Ok") { dialog, _ ->
                                            dialog.dismiss()
                                        }
                                        .create()
                                        .show()
                            }
                        }
                        override fun firstFetchOfData() {
                            // The data set has never been obtained before. This is the first fetch of data, ever. This is usually where a loading UI is shown to the user. It should be the only place that a loading UI should be shown! After this first fetch is *successful*, we can show the cached data from here on out.
                            // Do not make the loading UI blocking. This is a bad practice not only in Teller but all of Android development.
                        }
                        override fun cacheEmpty() {
                            // The cache data set is currently empty. Update your UI to tell your user that there is no data to show.
                        }
                        override fun cacheData(data: List<RepoModel>, fetched: Date) {
                            // `data` parameter is your List of GitHub repositories to display to the user. Update your UI to show this data set.
                            // `fetched` is when `data` was fetched and saved to the device cache. This determines how old the `data` is so you can show this to your user in your UI.
                        }
                        override fun fetchingFreshCacheData() {
                            // This listener callback is called when the cached data is determined "too old" and a fetch is performed to get new, fresh data. It is a good idea to update the UI of your app to tell your user that new data is being fetched right now.
                            // Do not make this a blocking UI! `cacheEmpty()` or `cacheData()` will also be called so you can display the current data stored in the cache to the user and they can still use your app while the fetch is being performed.
                        }
                        override fun finishedFetchingFreshCacheData(errorDuringFetch: Throwable?) {
                            // The fetching of fresh cache data is complete. It could have succeeded or failed. This is determined by if `errorDuringFetch` if null or not.
                            // Update your UI to alert your user that the data set has been updated if the fetch was successful, or optionally show the user an error message if it failed.
                        }
                     })
                }
```

Done! You are using Teller! When you add a listener to your `Repository` subclass, Teller kicks into gear and begins it's work parsing your cached data and fetching fresh if needed.

## Extra functionality

Teller comes with extra, but optional, features you may also enjoy.

#### Keep app data fresh in the background

You want to make sure that the data of your app is always up-to-date. When your users open your app, it's nice that they can jump right into some new content and not need to wait for a fetch to complete. Teller provides a simple method to sync your `Repository`s with your remote storage.

```Kotlin
Teller.shared.sync(listOf(ReposRepository(), OtherRepository(), AndAnotherRepository()), false)
                .subscribeOn(Schedulers.io())
                .subscribe {}
```

Teller provides this simple function: `Teller.shared.sync()` where you provide a list of `OnlineRepository`s that you want to call `.sync()` on. It is an `Observable`, so we need to subscribe to it to run the syncs. Teller will take the list and one by one, call `.sync()` on each of the `OnlineRepository`s.

Use some method of running periodic background tasks in your app such as [android-job](https://github.com/evernote/android-job) or [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) to run the above code as often as you choose in the background.

Enjoy!

## Example app

This library comes with an example app. You may open it in Android Studio to test it out and see how the code works with the library. The example app uses an `OnlineRepository` as well as a `LocalRepository` as well as some other best practices. Enter a GitHub username in the EditText in the app and watch how the app fetches a list of repos for the user, saves them to a cache, and fetches fresh if the data is too old.

## Documentation

Documentation is coming shortly. This README is all of the documentation created thus far.

~~There is a Javadoc (Kotlin doc, actually) for all of the public classes of Teller [hosted here](https://levibostian.github.io/Teller-Android/teller/) for the `master` branch.~~

~~The docs are installed in the `docs/` directory and can be generated from any branch with command: `./gradlew dokka`~~

## Are you building an offline-first mobile app?

Teller is designed for developers building offline-first mobile apps. If you are someone looking to build an offline-first mobile app, also be sure to checkout [Wendy-Android](https://github.com/levibostian/wendy-android) (there is an [iOS version too](https://github.com/levibostian/wendy-ios)). Wendy is designed to sync your device's cached data with remote storage. Think of it like this: Teller is really good at GET calls for your network API, Wendy is really good at PUT, POST, DELETE network API calls. Teller pulls data, Wendy pushes data.

## Author

* Levi Bostian - [GitHub](https://github.com/levibostian), [Twitter](https://twitter.com/levibostian), [Website/blog](http://levibostian.com)

![Levi Bostian image](https://gravatar.com/avatar/22355580305146b21508c74ff6b44bc5?s=250)

## Contribute

Teller is open for pull requests. Check out the [list of issues](https://github.com/levibostian/teller-android/issues) for tasks I am planning on working on. Check them out if you wish to contribute in that way.

**Want to add features to Teller?** Before you decide to take a bunch of time and add functionality to the library, please, [create an issue](https://github.com/levibostian/Teller-Android/issues/new) stating what you wish to add. This might save you some time in case your purpose does not fit well in the use cases of Teller.

# Where did the name come from?

This library is a powerful Repository. The Repository design pattern is commonly found in the MVVM and MVI patterns. A synonym of repository is *bank*. A *bank teller* is someone who manages your money at a bank and triggers transactions. So, since this library facilitates transactions, teller fits.

# Credits

Header photo by [Tim Evans](https://unsplash.com/photos/Uf-c4u1usFQ?utm_source=unsplash&utm_medium=referral&utm_content=creditCopyText) on [Unsplash](https://unsplash.com/?utm_source=unsplash&utm_medium=referral&utm_content=creditCopyText)
