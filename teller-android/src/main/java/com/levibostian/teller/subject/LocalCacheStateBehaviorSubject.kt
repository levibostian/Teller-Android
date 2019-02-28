package com.levibostian.teller.subject

import com.levibostian.teller.cachestate.LocalCacheState
import com.levibostian.teller.repository.LocalRepository
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

/**
 * A wrapper around [BehaviorSubject] and [LocalCacheState] to give a "compound" feature to [LocalCacheState] it did not have previously.
 *
 * [BehaviorSubject]s are great in that you can grab the very last value that was passed into it. This is a great type of [Observable] since you can always get the very last value that was emitted. This works great with [LocalCacheState] so you can always know the state of cache by grabbing it's last value.
 *
 * Maintaining the state of an instance of [LocalCacheState] is a pain. [LocalCacheState] has a state (loading, empty, cache) but it also has some other states built on top of it temporarily such as if an error occurs or if cache is currently being fetched. The UI cares about all of these states [LocalCacheState] could be in to display the best message to the user as possible. However, when an error occurs, for example, we need to pass the error to [LocalCacheState] to be handled by the UI. *Someone at some point needs to handle this error. We don't want it to go ignored*. What if we call [BehaviorSubject.onNext] with an instance of [LocalCacheState] with an error in it? That is unsafe. We could call [BehaviorSubject.onNext] shortly after with an instance of [LocalCacheState] without an error. **That error has now gone unseen!**
 *
 * Another use case to think about is fetching cache. You could call [BehaviorSubject.onNext] with an instance of [LocalCacheState] saying cache is fetching then shortly after an error occurs, the database fields changed, database rows were deleted, etc. and we will call [BehaviorSubject.onNext] again with another instance of [LocalCacheState]. Well, we need to keep track somehow of the fetching status of cache. That is a pain to maintain and make sure it is accurate. It's also error prone.
 *
 * With that in mind, we "compound" errors and status of fetching cache to the last instance of [LocalCacheState] found inside of an instance of [BehaviorSubject].
 *
 * This class is meant to work with [LocalRepository] because it has all the states cache can have, including loading and fetching of fresh cache.
 */
internal class LocalCacheStateBehaviorSubject<CACHE: Any> {

    private var cacheState: LocalCacheState<CACHE> = LocalCacheState.none()
        set(value) {
            synchronized(this) {
                field = value
                subject.onNext(field)
            }
        }
    val subject: BehaviorSubject<LocalCacheState<CACHE>> = BehaviorSubject.createDefault(cacheState)

    var currentState: LocalCacheState<CACHE>? = null
        get() {
            return synchronized(this) {
                subject.value
            }
        }
        private set

    fun resetStateToNone() {
        changeDataState(LocalCacheState.none())
    }

    /**
     * The status of cache is empty (optionally fetching new fresh cache as well).
     */
    fun onNextEmpty(requirements: LocalRepository.GetCacheRequirements) {
        changeDataState(LocalCacheState.isEmpty(requirements))
    }

    /**
     * The status of cache is cache (optionally fetching new fresh cache as well).
     */
    fun onNextCache(requirements: LocalRepository.GetCacheRequirements, data: CACHE) {
        changeDataState(LocalCacheState.cache(requirements, data))
    }

    private fun changeDataState(newValue: LocalCacheState<CACHE>) {
        synchronized(this) {
            cacheState = newValue
        }
    }

    /**
     * Get a [BehaviorSubject] as an [Observable]. Convenient as you more then likely do not need to care about the extra functionality of [BehaviorSubject] when you simply want to observe cache changes.
     */
    fun asObservable(): Observable<LocalCacheState<CACHE>> {
        return synchronized(this) {
            subject
        }
    }

}