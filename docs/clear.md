# Clear Teller data

Teller does not store a lot of data, but it does store some `SharedPreferences` to keep track of how old cached data is. 

In your app when you delete all of the data of your app, make sure to also delete all of Teller's data. 

To clear Teller's data, all you need to do is: 

```kotlin
Teller.clear()
```

### What's next? 

Check out the other [advanced functionality](beyond_the_basics) of Teller besides `clear()`. 

Learn about another awesome library, [Wendy](https://github.com/levibostian/Wendy-Android), to help you build fast, offline-first mobile apps. 

If you find Teller helpful and want to be a part of it's positive community, consider [contributing your skills](contribute) to the project! 