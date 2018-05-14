[![Release](https://jitpack.io/v/levibostian/Teller-Android.svg)](https://jitpack.io/#levibostian/Teller-Android)

# Teller

Android library that manages the state of your app's data. Teller also facilitates loading cached data and fetching fresh data so your app's data is always up to date.

![project logo](misc/logo.jpg)

iOS developer? The iOS version of Teller is coming soon. Keep an eye out for it.

## What is Teller?

The data used in your mobile app: user profiles, a collection of photos, list of friends, etc. *all have state*. Your data is in 1 of many different states:

* Being fetched for the first time (if it comes from an async network call)
* The data is empty
* Data exists in the device's storage (cached). Also during the empty and data states, your app could be fetching fresh data to replace the cached data that is out of date.

You tell Teller how to save your data, query your data, and how to fetch fresh data (probably with a network API call) and Teller facilities everything else for you.

## Why use Teller?

I created Teller because when I create offline-first mobile apps, keeping track of the state of my data is complex. In the UI of my app, I need to be able to tell users what state their data is in.

For example: If you are building a Twitter client app that is offline-first, when the user opens your app you should be showing them a list of cached tweets so that the user has something to interact with and not a loading screen saying "Loading tweets, please wait...". When you show this list of cached tweets, you may also be performing an API network call in the background to fetch the newest tweets for your user. In the UI of your app, you should be notifying your user that your app is fetching fresh tweets or else your user may think your app is broken. Keeping your user always informed about exactly what your app is doing is a good idea to follow. Teller helps you keep track of the state of your data and facilitates keeping it up to date.

Here are the added benefits of Teller:

* Small. The only dependency at this time is RxJava2 ([follow this issue as I work to remove this 1 dependency](https://github.com/levibostian/Teller-Android/issues/1))
* Built for Kotlin, by Kotlin. Teller is written in Kotlin which means you can expect a nice to use API.
* Not opinionated. Teller does not care where your data is stored, how it is cached, or how it is obtained. You simply tell Teller when you're done fetching, saving, and querying and Teller takes care of delivering it to the listeners.

# Install

Add this to your root build.gradle at the end of repositories:

```
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

Then, install the Teller module:

```
compile 'com.github.levibostian:teller-android:version-goes-here'
```

The latest release version at this time is: [![Release](https://jitpack.io/v/levibostian/Teller-Android.svg)](https://jitpack.io/#levibostian/Teller-Android)

**Note: Teller is early on in development.** Even though it is used in production in my own apps, it is still early on in development as I use the library more and more, it will mature.

I plan to release the library in an alpha, beta, then stable release phase.

#### Stages:

Pre-alpha (where the library is at currently):

- [ ] Build example app on how to use it.
- [ ] Documentation for README created.

Alpha:

- [ ] Make non-RxJava version of the library to make it even smaller and more portable.
- [ ] Documentation in form of JavaDoc created.
  - [ ] Documentation on how to use in MVVM, MVI setup and other setups as well.
- [ ] Fixup the API for the library if needed.

Beta:

- [ ] Library API has been used enough that the API does not have any huge changes planned for it.
- [ ] Tests written (and passing 😉) for the library.

Stable:

- [ ] Library has been running in many production apps, developers have tried it and given feedback on it.

# Getting started

This is coming very soon after the example app is done so I actually have a good example on how to use this lib....

## Example app

The example app is being built as we speak to show off how to use Teller. Check back soon!

~~This library comes with an example app. You may open it in Android Studio to test it out and see how the code works with the library.~~

## Documentation

There is a Javadoc (Kotlin doc, actually) for all of the public classes of Teller [hosted here](https://levibostian.github.io/Teller-Android/teller/) for the `master` branch.

The docs are installed in the `docs/` directory and can be generated from any branch with command: `./gradlew dokka`

## Author

* Levi Bostian - [GitHub](https://github.com/levibostian), [Twitter](https://twitter.com/levibostian), [Website/blog](http://levibostian.com)

![Levi Bostian image](https://gravatar.com/avatar/22355580305146b21508c74ff6b44bc5?s=250)

## Contribute

Teller is open for pull requests. Check out the [list of issues](https://github.com/levibostian/teller-android/issues) for tasks I am planning on working on. Check them out if you wish to contribute in that way.

**Want to add features to Teller?** Before you decide to take a bunch of time and add functionality to the library, please, [create an issue](https://github.com/levibostian/Teller-Android/issues/new) stating what you wish to add. This might save you some time in case your purpose does not fit well in the use cases of Teller.

# Where did the name come from?

This library is a powerful Repository. The Repository design pattern is commonly found in the MVVM and MVI patterns. A synonym of repository is *bank*. A *bank teller* is someone who manages your money at a bank and triggers transactions. So, since this library facilitates transactions, teller fits.

# Credits

Header photo by [Tim Evans](https://unsplash.com/photos/Uf-c4u1usFQ?utm_source=unsplash&utm_medium=referral&utm_content=creditCopyText) on [Unsplash](https://unsplash.com/?utm_source=unsplash&utm_medium=referral&utm_content=creditCopyText)
