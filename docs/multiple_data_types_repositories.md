# Support multiple cache data types in a Teller Repository

Let's say that you have an API endpoint to get a list of your friends on a social network app. You also have an API endpoint to get a list of your friend requests. 

The 2 API endpoints have very similar requests and responses. Wouldn't it be great if we didn't have to create an `OnlineRepository` subclass for `FriendsOnlineRepository` and another for `FriendRequestsOnlineRepository` and we could instead combine the 2 data type into 1 `FriendsAndRequestsOnlineRepository`? Well, you can! 

The secret is our use of the `OnlineRepository.GetCacheRequirements` Object. Because you have full control over this Object and because that Object is passed into all of the abstract functions of `OnlineRepository` subclass, we can handle multiple data types in 1 `OnlineRepository` subclass!

Here is some example code on how to do this: 

```kotlin
class FriendsAndRequestsOnlineRepository(private val service: RetrofitService,
                                         private val db: AppDatabase): OnlineRepository<FriendsOrRequests, ReposRepository.GetReposRequirements, FriendsOrRequests>() {

    override var maxAgeOfCache: Age = Age(7, Age.Unit.DAYS)

    override fun fetchFreshCache(requirements: GetReposRequirements): Single<FetchResponse<FriendsOrRequests>> {
        return when (requirements.type) {
            Type.FRIEND -> service.getFriends().map { response -> ... }
            Type.FRIEND_REQUEST -> service.getFriendRequests().map { response -> ... }
        }        
    }

    override fun saveCache(cache: FriendsOrRequests, requirements: GetReposRequirements) {
        when (requirements.type) {
            Type.FRIEND -> db.dao().insertFriends(cache.friends)
            Type.FRIEND_REQUEST -> db.dao().insertFriendRequests(cache.requests)
        }         
    }

    override fun observeCache(requirements: GetReposRequirements): Observable<FriendsOrRequests> {
        return when (requirements.type) {
            Type.FRIEND -> db.reposDao().observeFriends().toObservable().map { friends -> FriendsOrRequests(friends, null) }
            Type.FRIEND_REQUEST -> db.reposDao().observeFriendRequests().toObservable().map { requests -> FriendsOrRequests(null, requests) }
        }        
    }

    override fun isCacheEmpty(cache: FriendsOrRequests, requirements: GetReposRequirements): Boolean = cache.friends?.isEmpty() ?: cache.requests?.isEmpty()

    class GetFriendsOrRequestsRequirements(val type: Type) : GetCacheRequirements {
        override var tag: String = ""
            get() {
                return when (type) {
                    Type.FRIEND -> "Friends list"
                    Type.FRIEND_REQUEST -> "Friend requests list"
                }
            }
    }

    enum class Type {
        FRIEND,
        FRIEND_REQUEST
    }

    data class FriendsOrRequests(val friends: List<FriendModel>?, val requests: List<FriendRequestModel>?)

}
```

Now, in your app, depending on what you set for `FriendsAndRequestsOnlineRepository`'s `requirements` property will depend on what cache is loaded.

```kotlin
val friendsRepository = FriendsAndRequestsOnlineRepository(service, database).apply {
    requirements = FriendsAndRequestsOnlineRepository.GetFriendsOrRequestsRequirements(FriendsAndRequestsOnlineRepository.Type.FRIEND)
}

val friendRequestsRepository = FriendsAndRequestsOnlineRepository(service, database).apply {
    requirements = FriendsAndRequestsOnlineRepository.GetFriendsOrRequestsRequirements(FriendsAndRequestsOnlineRepository.Type.FRIEND_REQUEST)
}
```

You can even share the same repository and the same `OnlineRepository.observe()` function since the data type is the same (`FriendsAndRequestsOnlineRepositoryFriendsOrRequests`)!

```kotlin
val friendOrRequestsRepository = FriendsAndRequestsOnlineRepository(service, database)
friendOrRequestsRepository.observe() 
    .subscribe { friendsOrRequestsCacheState ->
        ...
    }

// Show friends to start with. 
friendOrRequestsRepository.requirements = FriendsAndRequestsOnlineRepository.GetFriendsOrRequestsRequirements(FriendsAndRequestsOnlineRepository.Type.FRIEND)

// Then, let's say the user clicks a button in the UI to view the friend requests. All you need to do is set the `requirements` property to something else and leave `observe()` alone! 

friendOrRequestsRepository.requirements = FriendsAndRequestsOnlineRepository.GetFriendsOrRequestsRequirements(FriendsAndRequestsOnlineRepository.Type.FRIEND_REQUEST)
```