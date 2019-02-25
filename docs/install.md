# Install

* First, you need to add Teller as a Gradle dependency to your project:

```
implementation 'com.levibostian.teller:teller-android:version-goes-here'
```

Replace `version-goes-here` with the latest release version at this time: [![Download](https://api.bintray.com/packages/levibostian/Teller-Android/com.levibostian.teller-android/images/download.svg)](https://bintray.com/levibostian/Teller-Android/com.levibostian.teller-android/_latestVersion)

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