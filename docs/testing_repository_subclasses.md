# Write unit tests for your Teller Repository subclasses

Have a `LocalRepository` or `OnlineRepository` subclass that you would like to unit test? Awesome! 

* Create an instance of your repository in your test class:

```kotlin
private lateinit var repository: ReposRepository

// If your repos have any dependencies 
@Mock private lateinit var database: Database

@Before
fun setup() {
    repository = ReposRepository(database)
}
```

* If you want to test the abstract functions of `LocalRepository` or `OnlineRepository` such as `saveCache()`, you have a couple of options. 

1. Modify the visibility of the override `saveCache()` function in your `OnlineRepository` subclass and call it directly:

```kotlin
// Change 
protected fun saveCache() 

// to
public fun saveCache()
```

Pros:
* Quick and easy

Cons:
* Exposes `saveCache()` function for your app's code to use even though it is not recommended to use directly. It's designed for Teller to use. 

2. Create a separate function that is more descriptive in your subclass, then call that function from `saveCache()`:

```kotlin
// 1. Leave saveCache() protected
protected fun saveCache()

// 2. Create a separate function to save data:
public fun saveRepositories(repos: List<RepoModel>)

// 3. Call this new function from saveCache()
protected fun saveCache(cache: List<RepoModel>, requirements: ReposRequirements) {
    saveRepositories(cache)
}
```

Now, write a unit test for `saveRepositories()` instead of `saveCache()`. 

Pros:
* Keeps `saveCache()` protected so it's only accessible for Teller to use. 
* Promotes reusability. Anyone can call `saveRepositories()` now, if you ever need to. 

Cons:
* Requires a 2nd function to save cache instead of just using 1. 

* Besides that, that's about all you need to know to unit test your own repositories. They are plain objects you can unit test all of the functions on individually. 

### What's next? 

If you need to use a repository in an instrumentation/integration/UI test, this requires a small bit of learning. Check out [the doc](testing_instrumentation_tests) on how to do this. 
