# Observe a LocalRepository

In your `Fragment`, `Activity`, `View`, `ViewModel` subclasses that you plan to display the cached data, follow the below quick steps:

* Create an instance of your Teller Repository you created:

```kotlin
val githubUsernameRepository: GitHubUsernameRepository = GitHubUsernameRepository(context)
```

* Observe the current state of the Repository in the UI of your app. This is the typical way that you would observe an RxJava `Observable` by subscribing to it: 

```kotlin
githubUsernameRepository.observe()
     .subscribe { usernameState ->
        
    }
```

* Whenever you are ready, specify what cached data Teller should read by setting the `requirements` on your `LocalRepository` subclass:

```kotlin
githubUsernameRepository.requirements = GitHubUsernameGetCacheRequirements()
```

!> Even if your `LocalRepository.GetCacheRequirements` subclass does not have any parameters as the example above, you still need to set the `requirements` object in your `LocalRepository` instance to instruct Teller to begin.

* Lastly, let's have Teller help us to parse the `usernameState` object and help us understand the current state of our cached data. Do that via the `deliver_` functions in `LocalDataState`. 

```kotlin
githubUsernameRepository.observe()
     .subscribe { usernameState ->
        usernameState?.deliverState(object : LocalDataStateListener<String> {
            override fun isEmpty() {
                // The GitHub username data set is currently empty. Update your UI here to tell your user to type in a username. 
            }
            override fun data(data: String) {
                // The GitHub username has been set! You can use the `data` parameter here as the GitHub username and display it however you wish in your app's UI.
            }
        })   
    }
```

How easy is that? Teller tells you if the cache data is empty (it doesn't exist) or is populated. All you need to take care of is populating the UI of your app now depending on the state of the data! 

* Lastly, as you are used to with using RxJava, make sure to call `dispose()` on your `LocalRepository` instance when you are done with it. 

```kotlin
override fun onDestroy() {
    super.onDestroy()

    githubUsernameRepository.dispose()
}
```

### What's next? 

Congrats, you are using Teller! 

The next step is to, well, keep building your app. Go write some code! 

....but if you decide you would rather do more reading, I have a couple recommendations. 

Learn about another awesome library, [Wendy](https://github.com/levibostian/Wendy-Android), to help you build fast, offline-first mobile apps. 

Learn about some of the [advanced features of Teller](beyond_the_basics).

If you find Teller helpful and want to be a part of it's positive community, consider [contributing your skills](contribute) to the project! 
