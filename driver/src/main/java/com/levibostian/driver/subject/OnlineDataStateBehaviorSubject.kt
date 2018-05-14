package com.levibostian.driver.subject

import com.levibostian.driver.datastate.OnlineDataState
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

/**
 * A wrapper around [BehaviorSubject] and [StateData] to give a "compound" feature to [StateData] it did not have previously.
 *
 * [BehaviorSubject]s are great in that you can grab the very last value that was passed into it. This is a great type of [Observable] since you can always get the very last value that was emitted. This works great with [StateData] so you can always know the state of data by grabbing it's last value.
 *
 * Maintaining the state of an instance of [StateData] is a pain. [StateData] has a state (loading, empty, data) but it also has some other states built on top of it temporarily such as if an error occurs or if data is currently being fetched. The UI cares about all of these states [StateData] could be in to display the best message to the user as possible. However, when an error occurs, for example, we need to pass the error to [StateData] to be handled by the UI. *Someone at some point needs to handle this error. We don't want it to go ignored*. What if we call [BehaviorSubject.onNext] with an instance of [StateData] with an error in it? That is unsafe. We could call [BehaviorSubject.onNext] shortly after with an instance of [StateData] without an error. **That error has now gone unseen!**
 *
 * Another use case to think about is fetching data. You could call [BehaviorSubject.onNext] with an instance of [StateData] saying data is fetching then shortly after an error occurs, the database fields changed, database rows were deleted, etc. and we will call [BehaviorSubject.onNext] again with another instance of [StateData]. Well, we need to keep track somehow of the fetching status of data. That is a pain to maintain and make sure it is accurate. It's also error prone.
 *
 * With that in mind, we "compound" errors and status of fetching data to the last instance of [StateData] found inside of an instance of [BehaviorSubject].
 */

// This class is meant to work with OnlineRepository because it has all the states data can have, including loading and fetching of fresh data.
internal class OnlineDataStateBehaviorSubject<DATA: Any> {

    private lateinit var dataState: OnlineDataState<DATA>
    private val subject: BehaviorSubject<OnlineDataState<DATA>> = BehaviorSubject.create()

    /**
     * The data is being fetched for the first time.
     */
    fun onNextFirstFetchOfData() {
        dataState = OnlineDataState.firstFetchOfData()
        subject.onNext(dataState)
    }

    fun hasValue(): Boolean = subject.hasValue()

    /**
     * The status of data is empty (optionally fetching new fresh data as well).
     */
    fun onNextEmpty(isFetchingFreshData: Boolean) {
        dataState = OnlineDataState.isEmpty()
        if (isFetchingFreshData) {
            onNextFetchingFreshData()
            dataState = dataState.fetchingFreshData()
        }
        subject.onNext(dataState)
    }

    /**
     * The status of data is data (optionally fetching new fresh data as well).
     */
    fun onNextData(data: DATA, isFetchingFreshData: Boolean) {
        dataState = OnlineDataState.data(data)
        if (isFetchingFreshData) {
            onNextFetchingFreshData()
            dataState = dataState.fetchingFreshData()
        }
        subject.onNext(dataState)
    }

    /**
     * Fresh data is being fetched. Compound that status to the existing [StateData] instance.
     */
    fun onNextFetchingFreshData() {
        dataState = dataState.fetchingFreshData()
        subject.onNext(dataState)
    }

    /**
     * Fresh data is done being fetched. Compound that status to the existing [StateData] instance.
     */
    fun onNextDoneFetchingFreshData(errorDuringFetch: Throwable?) {
        dataState = dataState.doneFetchingFreshData(errorDuringFetch)
        subject.onNext(dataState)
    }

    /**
     * An error occurred with this data.
     */
    fun onNextError(error: Throwable) {
        // We don't want a user to have to look at a loading screen after an error occurred therefore, we want to show them an empty view and an error. That's better then an infinite loading screen!
        if (subject.hasValue() && subject.value!!.firstFetchOfData) dataState = OnlineDataState.isEmpty()
        dataState = dataState.errorOccurred(error)
        subject.onNext(dataState)
    }

    /**
     * Get a [BehaviorSubject] as an [Observable]. Convenient as you more then likely do not need to care about the extra functionality of [BehaviorSubject] when you simply want to observe data changes.
     */
    fun asObservable(): Observable<OnlineDataState<DATA>> = subject

}