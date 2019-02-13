package com.levibostian.teller.subject

import com.levibostian.teller.datastate.LocalDataState
import com.levibostian.teller.repository.LocalRepository
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.util.*

/**
 * A wrapper around [BehaviorSubject] and [LocalDataState] to give a "compound" feature to [LocalDataState] it did not have previously.
 *
 * [BehaviorSubject]s are great in that you can grab the very last value that was passed into it. This is a great type of [Observable] since you can always get the very last value that was emitted. This works great with [LocalDataState] so you can always know the state of cacheData by grabbing it's last value.
 *
 * Maintaining the state of an instance of [LocalDataState] is a pain. [LocalDataState] has a state (loading, empty, cacheData) but it also has some other states built on top of it temporarily such as if an error occurs or if cacheData is currently being fetched. The UI cares about all of these states [LocalDataState] could be in to display the best message to the user as possible. However, when an error occurs, for example, we need to pass the error to [LocalDataState] to be handled by the UI. *Someone at some point needs to handle this error. We don't want it to go ignored*. What if we call [BehaviorSubject.onNext] with an instance of [LocalDataState] with an error in it? That is unsafe. We could call [BehaviorSubject.onNext] shortly after with an instance of [LocalDataState] without an error. **That error has now gone unseen!**
 *
 * Another use case to think about is fetching cacheData. You could call [BehaviorSubject.onNext] with an instance of [LocalDataState] saying cacheData is fetching then shortly after an error occurs, the database fields changed, database rows were deleted, etc. and we will call [BehaviorSubject.onNext] again with another instance of [LocalDataState]. Well, we need to keep track somehow of the fetching status of cacheData. That is a pain to maintain and make sure it is accurate. It's also error prone.
 *
 * With that in mind, we "compound" errors and status of fetching cacheData to the last instance of [LocalDataState] found inside of an instance of [BehaviorSubject].
 *
 * This class is meant to work with [LocalRepository] because it has all the states cacheData can have, including loading and fetching of fresh cacheData.
 */
internal class LocalDataStateCompoundBehaviorSubject<CACHE: Any> {

    private var dataState: LocalDataState<CACHE> = LocalDataState.none()
        set(value) {
            synchronized(this) {
                field = value
                subject.onNext(field)
            }
        }
    private val subject: BehaviorSubject<LocalDataState<CACHE>> = BehaviorSubject.createDefault(dataState)

    var currentState: LocalDataState<CACHE> = LocalDataState.none()
        get() {
            return synchronized(this) {
                subject.value!!
            }
        }

    fun resetStateToNone() {
        changeDataState(LocalDataState.none())
    }

    /**
     * The status of cacheData is empty (optionally fetching new fresh cacheData as well).
     */
    fun onNextEmpty() {
        changeDataState(LocalDataState.isEmpty())
    }

    /**
     * The status of cacheData is cacheData (optionally fetching new fresh cacheData as well).
     */
    fun onNextData(data: CACHE) {
        changeDataState(LocalDataState.data(data))
    }

    private fun changeDataState(newValue: LocalDataState<CACHE>) {
        synchronized(this) {
            dataState = newValue
        }
    }

    /**
     * Get a [BehaviorSubject] as an [Observable]. Convenient as you more then likely do not need to care about the extra functionality of [BehaviorSubject] when you simply want to observe cacheData changes.
     */
    fun asObservable(): Observable<LocalDataState<CACHE>> {
        return synchronized(this) {
            subject
        }
    }

}