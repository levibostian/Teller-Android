# Create LocalRepository

* The first step to creating a repository is to decide what type of data will be saved and queried in our `LocalRepository`? Perhaps `String`? `List<Int>`? Maybe an Object of your own (a [POJO](https://en.wikipedia.org/wiki/Plain_old_Java_object))? 

After you decide, create a subclass of `Teller`'s `LocalRepository` with the data type you choose as the generic parameter. *Do not worry about compile errors yet. Continue reading this page to get everything working*. 

In the example below, we will create a `LocalRepository` to store a GitHub username our app user has typed into a search bar. This way when the user closes our app and comes back into it later, we can display to them their latest search they performed. 

```kotlin
class GitHubUsernameRepository(): LocalRepository<String>() {    
}
```

* An important piece of Teller is the `GetCacheRequirements` object. Let's say that you create a note taking app that allows a user to create multiple different notes inside of it. You decide to use a `LocalRepository<Note>` to store and retrieve all of the notes stored to the user's local device. When it comes time to load a specific note to the user to view, how is Teller supposed to know what `Note` object to load? That is where `GetCacheRequirements` comes in. *You* use a `GetCacheRequirements` subclass to determine what `Note` to query and save in your `LocalRepository`. 

Let's say that you *identify* each of the `Note` objects in your app by the `Note`'s title. An example of a `GetCacheRequirements` would be: 

```kotlin
class NoteGetCacheRequirements(val title: String): LocalRepository.GetCacheRequirements
```

Inside of your `LocalRepository` subclass, (1) create a `GetCacheRequirements` subclass (2) add that subclass class as the 2nd generic parameter of `LocalRepository`:

```kotlin
class GitHubUsernameRepository(): LocalRepository<String, GitHubUsernameRepository.GitHubUsernameGetCacheRequirements>() {

    class GitHubUsernameGetCacheRequirements(): LocalRepository.GetCacheRequirements

}
```

You will notice that `GitHubUsernameGetCacheRequirements` does not have any parameters. Parameters exist to be helpful *for you* and are optional. In this for `GitHubUsernameRepository` we are only storing 1 username at any given time so no *identifier* is needed. 

* Lastly, let's implement the abstract methods of `LocalRepository`. They are each explained below: 

```kotlin
class GitHubUsernameRepository(): LocalRepository<String, GitHubUsernameRepository.GitHubUsernameGetCacheRequirements>() {

    /** 
     * Save cache to the local Android device.
     * Note: This function is guaranteed to be called on a background thread. 
     */ 
    override fun saveCache(cache: String, requirements: GitHubUsernameGetCacheRequirements) {        
        // Save `cache` parameter to the Local Android device. 
        // Use whatever method you wish! A database, SharedPreferences, a file, anything. 
    }
    
    /**
     * Return a RxJava2 `Observable` that continuously queries the locally cache and notifies any observers of changes.
     * Note: This function is guaranteed to be called on the main UI thread.      
     *
     * Do *not* send `null` in the `Observable` returned from this function. This is a rule set by RxJava and it will throw an exception. 
     * If your cache is in an "empty" state, either (1) return an empty value (Example: an empty String, "") or (2) create a POJO with an optional inside of it.
     */ 
    override fun observeCache(requirements: GitHubUsernameGetCacheRequirements): Observable<String> {        
    }

    /**
     * Help Teller determine is your cache is "empty" or not.
     * Note: This function is guaranteed to be called on the same thread as `observeCache()`. 
     *
     * In our example, an empty String (""), is what we qualify as empty. If you have a POJO, List, or any other type, you return back if the `cache` parameter is empty or not.
     */
    override fun isCacheEmpty(cache: String, requirements: GitHubUsernameGetCacheRequirements): Boolean = cache.isBlank()

    class GitHubUsernameGetCacheRequirements: LocalRepository.GetCacheRequirements

}
```

For out `GitHubUsernameRepository` example, we are storing the GitHub username in `SharedPreferences`. With that in mind, here is a full example of `GitHubUsernameRepository`:

```kotlin
class GitHubUsernameRepository(private val context: Context): LocalRepository<String, GitHubUsernameRepository.GitHubUsernameGetCacheRequirements>() {

    private val githubUsernameSharedPrefsKey = "${this::class.java.simpleName}_githubUsername_key"
    private val rxSharedPreferences: RxSharedPreferences = RxSharedPreferences.create(PreferenceManager.getDefaultSharedPreferences(context))
    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    /**
     * Use the `LocalRepository` as a regular repository. Add more functions to it to read, edit, delete the GitHub username.
     */
    var currentUsernameSaved: String? = null
        get() = sharedPreferences.getString(githubUsernameSharedPrefsKey, null)
        private set

    /**
     * Save cache to the local Android device.
     * Note: This function is guaranteed to be called on a background thread.
     */
    override fun saveCache(cache: String, requirements: GitHubUsernameGetCacheRequirements) {
        // In this case, we are using SharedPreferences to save our cache.
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(githubUsernameSharedPrefsKey, cache).apply()
    }

    /**
     * Return a RxJava2 `Observable` that continuously queries the local cache and notifies any observers of changes.
     * Note: This function is guaranteed to be called on the main UI thread.
     *
     * Do *not* send `null` in the `Observable` returned from this function. This is a rule set by RxJava and it will throw an exception.
     * If your cache is in an "empty" state, either (1) return an empty value (Example: an empty String, "") or (2) create a POJO with an optional inside of it.
     */
    override fun observeCache(requirements: GitHubUsernameGetCacheRequirements): Observable<String> {
        // Here, we are using the RxSharedPreferences library to Observe the SharedPreferences
        // Library link: https://github.com/f2prateek/rx-preferences
        return rxSharedPreferences.getString(githubUsernameSharedPrefsKey, "")
                .asObservable()
                .filter { it.isNotBlank() }
                .subscribeOn(Schedulers.io())
    }

    /**
     * Help Teller determine is your cache is "empty" or not.
     * Note: This function is guaranteed to be called on the same thread as `observeCache()`.
     *
     * In our example, an empty String (""), is what we qualify as empty. If you have a POJO, List, or any other type, you return back if the `cache` parameter is empty or not.
     */
    override fun isCacheEmpty(cache: String, requirements: GitHubUsernameGetCacheRequirements): Boolean = cache.isBlank()

    class GitHubUsernameGetCacheRequirements: LocalRepository.GetCacheRequirements

}
```

## Saving new cache

When your app user gives you new cache that you want to save to the local Android device, it is up to you to notify Teller of this new cache.

One convenient way to do this is to call the `LocalRepository.newCache()` function on your `LocalRepository` subclass. Teller will take care of saving this new cache on a background thread and restart observing of the cache all for you.

However, feel free to save the cache yourself if you need to do something special. Make sure that the `Observable` returned from `observeCache()` will update the observers of the new cache.

### What's next? 

All you need to do now is display the cache in the UI of your app. Teller takes care of everything else for you!

Let's learn how to [observe your newly created `LocalRepository` in the UI](observe_localrepository) of your app!
