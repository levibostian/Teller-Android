[![Download](https://api.bintray.com/packages/levibostian/Teller-Android/com.levibostian.teller-android/images/download.svg) ](https://bintray.com/levibostian/Teller-Android/com.levibostian.teller-android/_latestVersion)
[![Build](https://app.bitrise.io/app/4c0b872bdaf76300/status.svg?token=PYpJBThARi6LvucXS2noVw&branch=development)](https://app.bitrise.io/app/4c0b872bdaf76300)
[![GitHub license](https://img.shields.io/github/license/levibostian/Teller-Android.svg)](https://github.com/levibostian/Teller-Android/blob/master/LICENSE)
[![Awesome](https://cdn.rawgit.com/sindresorhus/awesome/d7305f38d29fed78fa85652e3a63e154dd8e8829/media/badge.svg)](https://github.com/JStumpp/awesome-android/blob/master/readme.md#other)

# Teller

Android library that makes your apps faster. 

Teller facilitates the downloading, saving, and reading of the cached data of your app. Keep your user's data fresh and remove those annoying loading screens!

![project logo](misc/logo.jpg)

[Read the official announcement of Teller](https://levibostian.com/blog/manage-cached-data-teller/) to learn more about what it does and why to use it.

iOS developer? Check out the [iOS version of Teller](https://github.com/levibostian/teller-ios).

## What is Teller?

The data used in your mobile app: user profiles, a collection of photos, list of friends, etc. *all have state*. Your data is in 1 of many different states:

* Being fetched for the first time (if it comes from an async network call).
* Data has been fetched before.
* Data has been fetched before, but is empty.
* Data has been fetched before but it's too old. It should be updated to show the user fresh data in the app. 

Determining what state your data is in, keeping it up-to-date, and displaying this information to the user in the UI is a big pain. That is where Teller comes in. All you need to do is show Teller how your app's cached data is saved, queried, and fetched and Teller facilities everything else for you. 

Teller will **query your cached data, parse the cache to determine it's state, fetch fresh data if the cache is too old, and deliver the results to listeners** so you can update the UI to your users.

## Are you building an offline-first mobile app?

Teller is designed for developers building offline-first mobile apps. If you want to build fast apps without loading screens, check out another great Android library, [Wendy-Android](https://github.com/levibostian/wendy-android) (there is an [iOS version too](https://github.com/levibostian/wendy-ios)). Wendy is designed to sync your device's cached data with a remote API. Think of it like this: **Teller is really good at GET calls for your network API, Wendy is really good at PUT, POST, DELETE network API calls. Teller pulls data, Wendy pushes data.**

## Why use Teller?

The use of a [cache](https://en.wikipedia.org/wiki/Cache_(computing)) in your Android app delivers a better app experience where you can...

* Remove annoying loading screens from your app. 
* Not worry about your user having a spotty Internet connection. 
* Allow your users to complete tasks in your app, faster. 
* Teller has performance improvements built-in to save your user's battery life. 

As app developers, we all understand that the fetching, saving, loading, *and maintaining* of cache data is a pain. When something is a pain to do, we often ignore that task and move on with building our app. 

However, with Teller this pain goes away. By using Teller you will get the benefits of using a cache while **not having to maintain that cache**. 

Here are some more benefits of Teller:

* Small - The only dependency at this time is RxJava2 ([this issue is working to make it optional](https://github.com/levibostian/Teller-Android/issues/1))
* Built in Kotlin, for Kotlin (also compatible with Java)
* Not opinionated. Use any storage method you wish
* Paging *just works*
* Full test suite, full documentation

What are you waiting for? Let's [install](https://levibostian.github.io/Teller-Android/#/install) Teller! 

# Documentation 

The very detailed documentation for Teller can be found [here](http://levibostian.github.io/Teller-Android). Go on, check them out!

## Author

* Levi Bostian - [GitHub](https://github.com/levibostian), [Twitter](https://twitter.com/levibostian), [Website/blog](http://levibostian.com)

![Levi Bostian image](https://gravatar.com/avatar/22355580305146b21508c74ff6b44bc5?s=250)

## Contribute

Do you think Teller is cool? Want to help make it *more cool*? Great! We can use your help.

Open source isn't just writing code. Teller could use your help with any of the
following:

- Finding (and reporting!) bugs.
- New feature suggestions.
- Answering questions on issues.
- Documentation improvements.
- Reviewing pull requests.
- Helping to manage issue priorities.
- Fixing bugs/new features.

The Teller community is a very positive one, and the maintainers are committed to keeping things awesome. Like [in over 230,000+ other communities](https://github.com/search?l=Markdown&q=%22Contributor+Covenant%22+fork%3Afalse&type=Code), always assume positive intent; even if a comment sounds mean-spirited, give the person the benefit of the doubt.

Please note that this project is released with a Contributor Code of Conduct. By participating in this project you agree to abide by [its terms](https://github.com/levibostian/Teller-Android/blob/master/CODE_OF_CONDUCT.md).
