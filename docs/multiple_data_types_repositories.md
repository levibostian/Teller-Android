# Support multiple data types in a Teller Repository 

Let's say that you have an API endpoint to get a list of your friends on a social network app. You also have an API endpoint to get a list of your friend requests. 

The 2 API endpoints have very similar requests and responses. Wouldn't it be great if we didn't have to create an `OnlineRepository` subclass for `FriendsOnlineRepository` and another for `FriendRequestsOnlineRepository` and we could instead combine the 2 data type into 1 `FriendsAndRequestsOnlineRepository`? Well, you can! 

The secret is our use of the `OnlineRepository.GetDataRequirements` Object. Because you have full control over this Object and because that Object is passed into all of the abstract functions of `OnlineRepository` subclass, we can handle multiple data types in 1 `OnlineRepository` subclass! 

Here is some example code on how to do this: 

```kotlin
class FriendsAndRequestsOnlineRepository(private val service: RetrofitService,
                                         private val db: AppDatabase): OnlineRepository<FriendsOrRequests, ReposRepository.GetReposRequirements, FriendsOrRequests>() {

    override var maxAgeOfData: AgeOfData = AgeOfData(7, AgeOfData.Unit.DAYS)

    override fun fetchFreshData(requirements: GetReposRequirements): Single<FetchResponse<FriendsOrRequests>> {
        return when (requirements.dataType) {
            Type.FRIEND -> service.getFriends().map { response -> ... }
            Type.FRIEND_REQUEST -> service.getFriendRequests().map { response -> ... }
        }        
    }

    override fun saveData(data: FriendsOrRequests, requirements: GetReposRequirements) {
        when (requirements.dataType) {
            Type.FRIEND -> db.dao().insertFriends(data.friends)
            Type.FRIEND_REQUEST -> db.dao().insertFriendRequests(data.requests)
        }         
    }

    override fun observeCachedData(requirements: GetReposRequirements): Observable<FriendsOrRequests> {
        return when (requirements.dataType) {
            Type.FRIEND -> db.reposDao().observeFriends().toObservable().map { friends -> FriendsOrRequests(friends, null) }
            Type.FRIEND_REQUEST -> db.reposDao().observeFriendRequests().toObservable().map { requests -> FriendsOrRequests(null, requests) }
        }        
    }

    override fun isDataEmpty(cache: FriendsOrRequests, requirements: GetReposRequirements): Boolean = cache.friends?.isEmpty() ?: cache.requests?.isEmpty()

    class GetFriendsOrRequestsRequirements(val dataType: Type) : GetDataRequirements {
        override var tag: String = ""
            get() {
                return when (dataType) {
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

Now, in your app, depending on what you set for `FriendsAndRequestsOnlineRepository`'s `requirements` property will depend on what data is loaded. 

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