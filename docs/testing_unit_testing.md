# Unit testing with Teller 

Are you writing unit tests in your app? Great! 

Want to write unit tests for your `OnlineRepository` or `LocalRepository` subclasses? 

Teller provides utilities that make it easy to write unit tests involving Teller. Check out one of the sections below to write your tests. 

* [Write unit tests for `LocalRepository` or `OnlineRepository` subclass](#write-unit-tests-for-localrepository-or-onlinerepository-subclass)
* [Mock Teller objects marked `final` with Mockito](#mock-teller-objects-marked-final)
* [Mock `Teller.shared.__()` commands with Mockito](#mock-teller__-commands) such as `Teller.shared.clear()`
* [Mock Teller `LocalRepository` or `OnlineRepository` with Mockito](mock-a-localrepository-or-onlinerepository)

*Note: It is assumed you know what Mockito is, why to use it, how to write tests, etc. That is out of the scope of this document.*

## Write unit tests for `LocalRepository` or `OnlineRepository` subclass

?> TL;DR View [this code example testing a LocalRepository](https://github.com/levibostian/teller-android/blob/development/app/src/test/java/com/levibostian/tellerexample/repository/GitHubUsernameRepositoryTest.kt) and [this code example testing an OnlineRepository](https://github.com/levibostian/teller-android/blob/development/app/src/test/java/com/levibostian/tellerexample/repository/ReposRepositoryTest.kt) from the example app.

Interested in writing a unit test for your Teller repository subclass? It's pretty easy. Here are the steps to doing so:

* First, initialize Teller for unit testing. Because Teller requires to be initialized as explained in the [install docs](install), we need to tell Teller that we are writing unit tests and therefore, skip initializing Teller. 

!> Initializing Teller for unit testing will throw an exception if you attempt to call a non-abstract method on your repository subclass such as `OnlineRepository.observe()`. This is because Teller requires Android specific utilities, like `SharedPreferences`, in order to function. If you need to test your Teller repository beyond your non-abstract methods, write [instrumentation tests](testing_android_instrumentation_tests). 

To make initializing Teller for unit testing quick and easy, it's recommended to create a JUnit test rule such as the one below:

```kotlin
import com.levibostian.teller.Teller
import org.junit.rules.ExternalResource

class TellerUnitTestRule: ExternalResource() {

    override fun before() {
        Teller.initUnitTesting()
    }

}
```

and using this test rule in your test file:

```kotlin
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class YourRepositoryTest {

    @get:Rule val tellerRule = TellerUnitTestRule()

}
```

* After initializing Teller, construct your repository subclass and test it! 

Below is a little snippet of a test example file. For the full example, view [this code example testing a LocalRepository](https://github.com/levibostian/teller-android/blob/development/app/src/test/java/com/levibostian/tellerexample/repository/GitHubUsernameRepositoryTest.kt) and [this code example testing an OnlineRepository](https://github.com/levibostian/teller-android/blob/development/app/src/test/java/com/levibostian/tellerexample/repository/ReposRepositoryTest.kt) from the example app.

```kotlin
@RunWith(MockitoJUnitRunner::class)
class GitHubUsernameRepositoryTest {
    
    @Mock private lateinit var sharedPreferences: SharedPreferences

    @get:Rule val tellerRule = TellerUnitTestRule()    

    private lateinit var repository: GitHubUsernameRepository

    @Before
    fun setUp() {
        repository = GitHubUsernameRepository(sharedPreferences)
    }

    @Test
    fun writeTestHere() {
        ...
    }    

}
```

## Mock Teller objects marked `final`

Mockito is [able to mock final classes](https://github.com/mockito/mockito/wiki/What's-new-in-Mockito-2#mock-the-unmockable-opt-in-mocking-of-final-classesmethods). Teller, being built in Kotlin where everything is final by default, has lots of final classes, methods, and properties. 

Check out [the example app](https://github.com/levibostian/Teller-Android/tree/master/app) included in the Teller project. Specifically [this directory](https://github.com/levibostian/Teller-Android/tree/development/app/src/test) to see how the Teller example app mocks Teller final classes. 

## Mock `Teller.shared.__()` commands

If you would like to mock [Teller.kt](https://levibostian.github.io/Teller-Android/javadoc/teller-android/com.levibostian.teller/-teller/index.html) functions such as `Teller.shared.clear()`, it's quite simple. 

?> TL;DR view some example code in the example app for mocking [`Teller.shared` commands](https://github.com/levibostian/teller-android/blob/development/app/src/test/java/com/levibostian/tellerexample/util/DataDestroyerUtilTest.kt).

Mockito cannot mock static functions so mocking `Teller.shared.clear()` directly is not possible. However, you can replace `Teller.shared` with a `Teller` instance that gets injected into the object you are testing. 

Here is what your code looks like now:

```kotlin
class OldDeleteDataManager {
    fun clearTeller() {
        Teller.shared.clear()
    }
}
```

Here is what your code can turn into to allow you to mock `Teller.shared`:

```kotlin
class NewDeleteDataManager(private val teller: Teller) {
    fun clearTeller() {
        teller.clear()
    }
}
```

Then in your `NewDeleteDataManagerTest` class, you can mock Teller: 

```kotlin
@Mock private lateinit var teller: Teller 

private var deleteDataManager = NewDeleteDataManager(teller)

deleteDataManager.clearTeller()

verify(teller).clear()
```

## Mock a `LocalRepository` or `OnlineRepository`

If you would like to mock a `LocalRepository` or `OnlineRepository` subclass of yours, this is quite easy to do. Check out the instructions below for how to mock an `OnlineRepository` instance as the same directions can easily be applied to `LocalRepository` as well. 

?> TL;DR view some example code in the example app for mocking [an `OnlineRepository`](https://github.com/levibostian/teller-android/blob/development/app/src/test/java/com/levibostian/tellerexample/viewmodel/ReposViewModelTest.kt) or [a `LocalRepository`](https://github.com/levibostian/teller-android/blob/development/app/src/test/java/com/levibostian/tellerexample/viewmodel/GitHubUsernameViewModelTest.kt) subclass.

* Create a mocked instance of your repository instance:

```kotlin
@Mock private lateinit var reposRepository: ReposRepository
```

* Mock the functions of your new `reposRepository` mock. 

In this example, let's show off `observe()`: 

```kotlin
`when`(reposRepository.observe()).thenReturn(Observable.just(reposCache))
```

...but wait. What is `reposCache`? 

Teller provides a handy object `Testing` you can use for testing purposes to create instances of internal Teller objects. In our case, we need an instance of `OnlineCacheState<>` to take the value of our variable `reposCache`. 

```kotlin
@Mock lateinit var requirements: ReposRepository.GetReposRequirements

val reposCache = OnlineCacheState.Testing.cache<List<RepoModel>>(requirements, Date()) {
    cache(listOf(RepoModel()))
    fetching()
}
```

Notice `OnlineCacheState.Testing`. This object is for you to generate immutable instances of the internal Teller class, `OnlineCacheState`. The above code generates an instance of `OnlineCacheState` with a `requirements` property set, date when cache was last fetched, a cache consisting of a list of repos that contains 1 `RepoModel`, and it is fetching fresh cache. 

To use the `Testing` objects, you will call one of the static functions on the `Testing` class to initialize an instance and then use the lambda parameter that exposes a [Kotlin DSL](https://kotlinlang.org/docs/reference/type-safe-builders.html) as a method to build the instance. 

Here is a list of testing objects available for you to use. 

`OnlineCacheState.Testing` [javadoc](/javadoc/teller-android/com.levibostian.teller.cachestate/-online-cache-state/-testing.html)
`OnlineRepository.RefreshResult.Testing` [javadoc](/javadoc/teller-android/com.levibostian.teller.repository/-online-repository/-refresh-result/-testing.html)
`LocalCacheState.Testing` [javadoc](/javadoc/teller-android/com.levibostian.teller.cachestate/-local-cache-state/-testing.html)
