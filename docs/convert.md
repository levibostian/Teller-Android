# Convert

Like `.map {}`, you can convert the data type of an instance of `OnlineDataState`.

```kotlin
data class User(val username: String,
                val userId: String,
                val friends: List<Friend>,
                val friendRequests: List<FriendRequest>)

val observeFriends = userRepository.observe() // observes `OnlineCacheState<User>`
  .map { dataState ->
    dataState.convert { user ->
       user?.friends
    }
  }

// Now, `observeFriends` is of type `Observable<OnlineCacheState<List<Friend>>>`
```

Because when you observe a Repository, you can only observe 1 data type. Convert allows you to convert the data type to another type!