[![Download](https://api.bintray.com/packages/levibostian/Teller-Android/com.levibostian.teller-android/images/download.svg) ](https://bintray.com/levibostian/Teller-Android/com.levibostian.teller-android/_latestVersion)
[![Build](https://app.bitrise.io/app/4c0b872bdaf76300/status.svg?token=PYpJBThARi6LvucXS2noVw&branch=development)](https://app.bitrise.io/app/4c0b872bdaf76300)
[![GitHub license](https://img.shields.io/github/license/levibostian/Teller-Android.svg)](https://github.com/levibostian/Teller-Android/blob/master/LICENSE)
[![Awesome](https://cdn.rawgit.com/sindresorhus/awesome/d7305f38d29fed78fa85652e3a63e154dd8e8829/media/badge.svg)](https://github.com/JStumpp/awesome-android/blob/master/readme.md#other)

# Intro 

Android library that makes your apps faster. 

Teller facilitates the downloading, saving, and reading of the cached data of your app. Keep your user's cache fresh and remove those annoying loading screens!

[Read the official announcement of Teller](https://levibostian.com/blog/manage-cached-data-teller/) to learn more about what it does and why to use it.

iOS developer? Check out the [iOS version of Teller](https://github.com/levibostian/teller-ios).

## What is Teller?

The data used in your mobile app: user profiles, a collection of photos, list of friends, etc. *all have state*. Your data is in 1 of many different states:

* Being fetched for the first time (if it comes from an async network call).
* Cache has been fetched before.
* Cache has been fetched before, but is empty.
* Cache has been fetched before but it's too old. It should be updated to show the user fresh cache in the app.

Determining what state your cache is in, keeping it up-to-date, and displaying this information to the user in the UI is a big pain. That is where Teller comes in. All you need to do is show Teller how your app's cache is saved, queried, and fetched and Teller facilities everything else for you.

Teller will **query your cache, parse the cache to determine it's state, fetch fresh cache if it's too old, and deliver the results to listeners** so you can update the UI to your users.

## Are you building an offline-first mobile app?

Teller is designed for developers building offline-first mobile apps. If you want to build fast apps without loading screens, check out another great Android library, [Wendy-Android](https://github.com/levibostian/wendy-android) (there is an [iOS version too](https://github.com/levibostian/wendy-ios)). Wendy is designed to sync your device's cache with a remote API. Think of it like this: **Teller is really good at GET calls for your network API, Wendy is really good at PUT, POST, DELETE network API calls. Teller pulls data, Wendy pushes data.**

## Why use Teller?

The use of a [cache](https://en.wikipedia.org/wiki/Cache_(computing)) in your Android app delivers a better app experience where you can...

* Remove annoying loading screens from your app. 
* Not worry about your user having a spotty Internet connection. 
* Allow your users to complete tasks in your app, faster. 
* Teller has performance improvements built-in to save your user's battery life. 

As app developers, we all understand that the fetching, saving, loading, *and maintaining* of cache is a pain. When something is a pain to do, we often ignore that task and move on with building our app.

However, with Teller this pain goes away. By using Teller you will get the benefits of using a cache while **not having to maintain that cache**. 

Here are some more benefits of Teller:

* Small - The only dependency at this time is RxJava2 ([this issue is working to make it optional](https://github.com/levibostian/Teller-Android/issues/1))
* Built in Kotlin, for Kotlin (also compatible with Java)
* Not opinionated. Use any storage method you wish
* Paging *just works*
* Full test suite, full documentation

What are you waiting for? Let's [install](install) Teller! 

## Example app

This library comes with an example app. You may open it in Android Studio to test it out and see how the code works with the library. The example app uses an `OnlineRepository` as well as a `LocalRepository`. 

Enter a GitHub username in the `EditText` in the app and watch how the app fetches a list of repos for the user, saves them to a cache, and fetches fresh if the cache is too old.

## Author

* Levi Bostian - [GitHub](https://github.com/levibostian), [Twitter](https://twitter.com/levibostian), [Website/blog](http://levibostian.com)

![Levi Bostian image](https://gravatar.com/avatar/22355580305146b21508c74ff6b44bc5?s=250)

## Stages 

!> Teller is alpha software. Even though it is used in production today with a full test suite, it needs to be used by more people in more apps to be considered more stable. 

[Check out the current plan to get Teller to a stable release](stable_release_plan)

### What's next? 

What are you waiting for? Let's [install](install) Teller! 

Enjoy!
