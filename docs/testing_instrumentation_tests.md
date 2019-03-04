# Write instrumentation tests with Teller

When using a Teller `LocalRepository` or `OnlineRepository` in a integration test, you may want to setup your environment a bit for each test. 

If you want to...

* Test the UI of your app for an `OnlineRepository` instance that fails fetching for the first time. 
* Test your `OnlineRepository` and ViewModel work well together to refresh a cache that is too old. 
* Test a `LocalRepository.observe()` observer receives the new cache update after a `LocalRepository.newCache()` call. 
* When you pull to refresh on your `RecyclerView`, a network fetch call is performed to force refresh of a cache. 

Or anything else you wish to test, the following steps should get you on your way. 

* Make sure to clear Teller's internal data and initialize it before each test. 

TODO. 

* Also, do not forget to call `dispose()` on your repository subclasses after each test. 

TODO. explain how you can do this in your activity, or if you inject the instances, dispose of them in the @After. 

* Now, you want to setup the state of your cache of your app in your test. 

TODO 