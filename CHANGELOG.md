## [0.2.0-alpha] - 2019-03-15

Testing utilities and improved API for CacheState objects!

### Changed
- **Breaking Change** The `deliver_()` functions in `OnlineCacheState` and `LocalCacheState` have been replaced with a more Kotlin like callback API with lambdas. [47](https://github.com/levibostian/Teller-Android/issues/47)
- Remove Java close compatibility. Because Kotlin and Java work nicely together, Teller may still work nicely with Java, but Teller is no longer focusing on making sure that is the case. [46](https://github.com/levibostian/Teller-Android/issues/46)
- Expose public properties to `OnlineCacheState`. 

### Added 
- Create code of conduct and add to the docs. 
- Create set of utilities into Teller to assist in creating unit, integration, and instrumentation tests. 
- Create section in docs on how to test with Teller in your app. 
- Create tests in example to show examples on how to use Teller's testing utilities and to test the API as it's developed. 

### Fixed
- Fixed bug in `OnlineCacheState` where refresh calls may not call `refreshComplete()` after a fetch finished successfully. 

## [0.1.0-alpha] - 2019-02-21

Changes to the API, docs, full test suite, thread safety and bug fixes.

### Changed
- **Breaking Change** `OnlineRepository`'s abstract functions have all been renamed. This is to try and be more descriptive of what Teller is doing to help developers implement faster.
- **Breaking Change** `OnlineDataState.deliver__()` listeners has been renamed.
- **Breaking Change** `OnlineRepository.observeCachedData()` and `OnlineRepository.isDataEmpty()` gets called on UI thread. `OnlineRepository.saveData()` gets called on background thread.
- `OnlineRepository.observe()` and `LocalRepository.observe()` no longer `throws`. Observe anytime you wish and receive event updates through all changes of the repository.
- `OnlineRepository.refresh()` calls are shared between all `OnlineRepository` instances. This saves on the amount of network calls performed. `OnlineRepository.observe()` observers are all notified when `OnlineRepository.refresh()` actions are performed.
- `OnlineRepository` is thread safe.
- Removed `Teller.sync()`

### Added
- `OnlineRepository` now supports observing 2+ queries, as long as it's the same type of data.
- Delete Teller data for development purposes or when someone logs out of your app and the data is cleared via `Teller.clear()`.
- Full test suite for unit testing and implementation testing!
- Full documentation website!

# Contributors changelog

### Changed
- `OnlineDataState` has been refactored to using a finite state machine implementation. See `OnlineDataStateStateMachine`. It's a immutable object that represents an immutable `OnlineDataState` instance.
- Fetching fresh cache data and the state of the cache data have been decoupled in `OnlineDataState`. Each of these 2 different states (fetching and cache) are updated independently from each other in the `OnlineRepository` so decoupling them fixes bugs that have been reported.
- `OnlineRepository` is designed to be "useless" after `.dispose()` is called. All aync tasks are cancelled or the results are ignored when they are complete. Calling any function on `OnlineRepository` after `dispose()` will throw an exception.

## [0.0.1] - 2018-05-21
First release of Teller!

### Added
- `LocalRepository` for saving and querying locally cached data.
- `OnlineRepository` for saving, querying, and fetching remote data that is cached locally.
- `Teller.shared.sync()` to sync a list of `OnlineRepository` instances at once.
- Example Android app to show how to use Teller and, for now until tests are written, to test Teller.
- README.md documentation on status of project and how to use it.