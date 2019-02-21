package com.levibostian.teller.subject

import com.levibostian.teller.datastate.OnlineDataState
import com.levibostian.teller.datastate.listener.OnlineDataStateNoCacheStateListener
import com.levibostian.teller.datastate.online.statemachine.OnlineDataStateStateMachine
import com.levibostian.teller.repository.OnlineRepository
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.util.*

/**
 * A wrapper around [BehaviorSubject] and [OnlineDataState] to give a "compound" feature to [OnlineDataState] it did not have previously.
 *
 * [BehaviorSubject]s are great in that you can grab the very last value that was passed into it. This is a great type of [Observable] since you can always get the very last value that was emitted. This works great with [OnlineDataState] so you can always know the state of cacheData by grabbing it's last value.
 *
 * Maintaining the state of an instance of [OnlineDataState] is a pain. [OnlineDataState] has a state (loading, empty, cacheData) but it also has some other states built on top of it temporarily such as if an error occurs or if cacheData is currently being fetched. The UI cares about all of these states [OnlineDataState] could be in to display the best message to the user as possible. However, when an error occurs, for example, we need to pass the error to [OnlineDataState] to be handled by the UI. *Someone at some point needs to handle this error. We don't want it to go ignored*. What if we call [BehaviorSubject.onNext] with an instance of [OnlineDataState] with an error in it? That is unsafe. We could call [BehaviorSubject.onNext] shortly after with an instance of [OnlineDataState] without an error. **That error has now gone unseen!**
 *
 * Another use case to think about is fetching cacheData. You could call [BehaviorSubject.onNext] with an instance of [OnlineDataState] saying cacheData is fetching then shortly after an error occurs, the database fields changed, database rows were deleted, etc. and we will call [BehaviorSubject.onNext] again with another instance of [OnlineDataState]. Well, we need to keep track somehow of the fetching status of cacheData. That is a pain to maintain and make sure it is accurate. It's also error prone.
 *
 * With that in mind, we "compound" errors and status of fetching cacheData to the last instance of [OnlineDataState] found inside of an instance of [BehaviorSubject].
 *
 * This class is meant to work with [OnlineRepository] because it has all the states cacheData can have, including loading and fetching of fresh cacheData.
 */
internal class OnlineDataStateBehaviorSubject<CACHE: Any> {

    private var dataState: OnlineDataState<CACHE> = OnlineDataState.none()
        set(value) {
            synchronized(this) {
                field = value

                if (!subject.hasComplete()) subject.onNext(dataState)
            }
        }
    val subject: BehaviorSubject<OnlineDataState<CACHE>> = BehaviorSubject.createDefault(dataState)

    var currentState: OnlineDataState<CACHE>? = null
        get() {
            return synchronized(this) {
                subject.value
            }
        }
        private set

    /**
    When the `OnlineRepositoryGetDataRequirements` is changed in an `OnlineRepository` to nil, we want to reset to a "none" state where the data has no state and there is nothing to keep track of. This is just like calling `init()` except we are not re-initializing this whole class. We get to keep the original `subject`.
     */
    fun resetStateToNone() {
        changeDataState(OnlineDataState.none())
    }

    fun resetToNoCacheState(requirements: OnlineRepository.GetDataRequirements) {
        changeDataState(OnlineDataStateStateMachine.noCacheExists(requirements))
    }

    fun resetToCacheState(requirements: OnlineRepository.GetDataRequirements, lastTimeFetched: Date) {
        changeDataState(OnlineDataStateStateMachine.cacheExists(requirements, lastTimeFetched))
    }

    fun changeState(change: (OnlineDataStateStateMachine<CACHE>) -> OnlineDataState<CACHE>) {
        synchronized(this) {
            val stateMachine = dataState.stateMachine!!
            dataState = change(stateMachine)
        }
    }

    private fun changeDataState(newValue: OnlineDataState<CACHE>) {
        synchronized(this) {
            dataState = newValue
        }
    }

    /**
     * Get a [BehaviorSubject] as an [Observable]. Convenient as you more then likely do not need to care about the extra functionality of [BehaviorSubject] when you simply want to observe cacheData changes.
     */
    fun asObservable(): Observable<OnlineDataState<CACHE>> {
        return synchronized(this) {
            subject
        }
    }

}