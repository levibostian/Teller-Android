# Write Android instrumentation tests with Teller

When using a Teller `LocalRepository` or `OnlineRepository` in an Android instrumentation test, you may want to setup your environment a bit for each test. 

If you want to...

* Test the UI of your app for an `OnlineRepository` instance that fails fetching for the first time. 
* Test your `OnlineRepository` and ViewModel work well together to refresh a cache that is too old. 
* Test a `LocalRepository.observe()` observer receives the new cache update after a `LocalRepository.newCache()` call. 
* When you pull to refresh on your `RecyclerView`, a network fetch call is performed to force refresh of a cache. 

Or anything else you wish to test, the following steps should get you on your way. 

?> TL;DR view some example code in the example app for [an Android instrumentation test](https://github.com/levibostian/teller-android/blob/development/app/src/androidTest/java/com/levibostian/tellerexample/integration/ReposIntegrationTest.kt).

* Make sure to clear Teller's internal data and initialize it before each test. 

Create a JUnit test rule that initializes and clears Teller before each test: 

```kotlin
import androidx.test.platform.app.InstrumentationRegistry
import com.levibostian.teller.Teller
import org.junit.rules.ExternalResource

class TellerInitRule: ExternalResource() {

    override fun before() {
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext.let { application ->
            Teller.init(application)
            Teller.shared.clear()
        }
    }

}
```

Then add it to your test: 

```kotlin
import org.junit.Rule

@RunWith(AndroidJUnit4::class)
class YourInstrumentationTestFile {

    @get:Rule val tellerInit = TellerInitRule()

    @Before
    fun setup() {
        ...
    }

    @Test 
    fun testHere() {
        ...
    }
}
```

* Also, do not forget to call `dispose()` on your repository subclasses after each test like this example:

```kotlin
import org.junit.After 

@RunWith(AndroidJUnit4::class)
class YourInstrumentationTestFile {

    private lateinit var repository: ReposRepository

    @Before 
    fun setup() {
        repository = ReposRepository()
    }

    @After 
    fun teardown() {
        repository.dispose()
    }

    @Test 
    fun testHere() {
        ...
    }
}
```

* Now, you want to setup the state of your cache of your app in your test. Maybe you want to test explicitly when your `OnlineRepository` instance has an empty cache but the cache is too old. 

Teller comes with a utility that allows you to setup an initial state of an `OnlineRepository` for your test!

```kotlin
    @Test
    fun doSomethingWithRepository_cacheEmptyAndTooOld() {
        val setValues = OnlineRepository.Testing.initState(repository, requirements) {
            cacheEmpty {
                cacheTooOld()
            }
        }
        
        ...
    }
```

?> Note: `OnlineRepository.Testing` comes with `initState` and `initStateAsync`. `initStateAsync` is used primarily when your instrumentation test is annotated with `@UiThreadTest` meaning your test function runs on the UI thread. 

After you use the `OnlineRepository.Testing.initState()` utility, Teller sets the initial state of the repository for you. Therefore, if you were to call `observe()` on your `repository` instance, you would expect to receive a `OnlineCacheState` instance where the cache is empty, and the cache is fetching to update. 

?> Remember to use [the testing utility for OnlineCacheState](testing_mocking_teller) to create instances of `OnlineCacheState` for comparing with what you observe from your `OnlineRepository.observe()`. 

For more information: `OnlineRepository.Testing` [javadoc](/javadoc/teller-android/com.levibostian.teller.repository/-online-repository/-testing.html)
