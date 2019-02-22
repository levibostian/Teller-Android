# Create a Teller Repository

The Teller `Repository` is where the magic happens. `Repository`s are where you instruct Teller how your cache is fetched, saved to the device, and later queried.

The first step you need to take is decide what type of Teller `Repository` you need to create:

...TL;DR...if you need to perform a network call to obtain your cache, create a [OnlineRepository](create_onlinerepository). Else, create a [LocalRepository](create_localrepository).

* [LocalRepository](create_localrepository) is very simple as it does not require a network call to fetch cache for your app. The cache is simply obtained locally on the device and saved to the device. An example of a `LocalRepository` might be to store a history of search terms your user has typed into the search bar of your app.

* [OnlineRepository](create_onlinerepository) is used when your app requires an asynchronous network call to get the cache needed in your app. An example of a `OnlineRepository` would be to store a list of your app user's friends, storing a user's profile, timeline of activity in a social network app, downloading photos or videos for a media app for offline use.

### What's next? 

Decide, what type of Repository do you need to create? 

[LocalRepository](create_localrepository) or [OnlineRepository](create_onlinerepository) 


