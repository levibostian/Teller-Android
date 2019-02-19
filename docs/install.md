# Install

* First, you need to add Teller as a Gradle dependency to your project. 

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
implementation 'com.github.levibostian:teller-android:version-goes-here'
```

Replace `version-goes-here` with the latest release version at this time: [![Release](https://jitpack.io/v/levibostian/Teller-Android.svg)](https://jitpack.io/#levibostian/Teller-Android)

* Lastly, initialize Teller in your app's `Application` class so it can startup. 

```kotlin
class MainApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        Teller.init(this)
    }

}
```

### What's next? 

Move onto creating your [first Teller Repository](create_repository). 