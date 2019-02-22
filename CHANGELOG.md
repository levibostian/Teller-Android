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