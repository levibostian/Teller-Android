package com.levibostian.teller.subject

import com.levibostian.teller.cachestate.CacheState
import com.levibostian.teller.cachestate.statemachine.CacheStateStateMachine
import com.levibostian.teller.repository.TellerRepository
import com.levibostian.teller.repository.RepositoryCache
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.util.*

/**
 * A wrapper around [BehaviorSubject] and [CacheState] to give a "compound" feature to [CacheState] it did not have previously.
 *
 * [BehaviorSubject]s are great in that you can grab the very last value that was passed into it. This is a great type of [Observable] since you can always get the very last value that was emitted. This works great with [CacheState] so you can always know the state of cache by grabbing it's last value.
 *
 * Maintaining the state of an instance of [CacheState] is a pain. [CacheState] has a state (loading, empty, cache) but it also has some other states built on top of it temporarily such as if an error occurs or if cache is currently being fetched. The UI cares about all of these states [CacheState] could be in to display the best message to the user as possible. However, when an error occurs, for example, we need to pass the error to [CacheState] to be handled by the UI. *Someone at some point needs to handle this error. We don't want it to go ignored*. What if we call [BehaviorSubject.onNext] with an instance of [CacheState] with an error in it? That is unsafe. We could call [BehaviorSubject.onNext] shortly after with an instance of [CacheState] without an error. **That error has now gone unseen!**
 *
 * Another use case to think about is fetching cache. You could call [BehaviorSubject.onNext] with an instance of [CacheState] saying cache is fetching then shortly after an error occurs, the database fields changed, database rows were deleted, etc. and we will call [BehaviorSubject.onNext] again with another instance of [CacheState]. Well, we need to keep track somehow of the fetching status of cache. That is a pain to maintain and make sure it is accurate. It's also error prone.
 *
 * With that in mind, we "compound" errors and status of fetching cache to the last instance of [CacheState] found inside of an instance of [BehaviorSubject].
 *
 * This class is meant to work with [TellerRepository] because it has all the states cache can have, including loading and fetching of fresh cache.
 */
internal class OnlineCacheStateBehaviorSubject<CACHE: RepositoryCache> {

    private var cacheState: CacheState<CACHE> = CacheState.none()
        set(value) {
            synchronized(this) {
                field = value

                if (!subject.hasComplete()) subject.onNext(cacheState)
            }
        }
    val subject: BehaviorSubject<CacheState<CACHE>> = BehaviorSubject.createDefault(cacheState)

    var currentState: CacheState<CACHE>? = null
        get() {
            return synchronized(this) {
                subject.value
            }
        }
        private set

    /**
    When the `OnlineRepositoryGetDataRequirements` is changed in an `OnlineRepository` to nil, we want to reset to a "none" state where the response has no state and there is nothing to keep track of. This is just like calling `init()` except we are not re-initializing this whole class. We get to keep the original `subject`.
     */
    fun resetStateToNone() {
        changeDataState(CacheState.none())
    }

    fun resetToNoCacheState(requirements: TellerRepository.GetCacheRequirements) {
        changeDataState(CacheStateStateMachine.noCacheExists(requirements))
    }

    fun resetToCacheState(requirements: TellerRepository.GetCacheRequirements, lastTimeFetched: Date) {
        changeDataState(CacheStateStateMachine.cacheExists(requirements, lastTimeFetched))
    }

    fun changeState(change: (CacheStateStateMachine<CACHE>) -> CacheState<CACHE>) {
        synchronized(this) {
            val stateMachine = cacheState.stateMachine!!
            cacheState = change(stateMachine)
        }
    }

    private fun changeDataState(newValue: CacheState<CACHE>) {
        synchronized(this) {
            cacheState = newValue
        }
    }

    /**
     * Get a [BehaviorSubject] as an [Observable]. Convenient as you more then likely do not need to care about the extra functionality of [BehaviorSubject] when you simply want to observe cache changes.
     */
    fun asObservable(): Observable<CacheState<CACHE>> {
        return synchronized(this) {
            subject
        }
    }

}