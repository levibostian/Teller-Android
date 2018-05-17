package com.levibostian.teller.repository

import android.annotation.SuppressLint
import com.levibostian.teller.Teller
import com.levibostian.teller.datastate.LocalDataState
import com.levibostian.teller.subject.LocalDataStateCompoundBehaviorSubject
import com.levibostian.teller.util.ConstantsUtil
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*

abstract class LocalRepository<DATA: Any> {

    private var stateOfDate: LocalDataStateCompoundBehaviorSubject<DATA>? = null
    private var compositeDisposable = CompositeDisposable()

    /**
     * Get an observable that gets the current state of data and all future states.
     */
    fun observe(): Observable<LocalDataState<DATA>> {
        if (stateOfDate == null) {
            stateOfDate = LocalDataStateCompoundBehaviorSubject()

            compositeDisposable.add(
                    observeData()
                            .subscribe({ cachedData ->
                                if (cachedData == null || isDataEmpty(cachedData)) {
                                    stateOfDate!!.onNextEmpty()
                                } else {
                                    stateOfDate!!.onNextData(cachedData)
                                }
                            }, { error ->
                                stateOfDate!!.onNextError(error)
                            })
            )
        }
        return stateOfDate!!.asObservable().doOnDispose {
            compositeDisposable.dispose()
        }
    }

    /**
     * Save the data to whatever storage method Repository chooses.
     *
     * It is up to you to call [saveData] when you have new data to save. A good place to do this is in a ViewModel.
     *
     * *Note:* It is up to you to run this function from a background thread. This is not done by default for you.
     */
    abstract fun saveData(data: DATA)

    /**
     * This function should be setup to trigger anytime there is a data change. So if you were to call [saveData], anyone observing the [Observable] returned here will get notified of a new update.
     */
    abstract fun observeData(): Observable<DATA>

    /**
     * DataType determines if data is empty or not. Because data can be of `Any` type, the DataType must determine when data is empty or not.
     */
    abstract fun isDataEmpty(data: DATA): Boolean

    /**
     * Get data if exists instead of observing it. Great for one off getting data.
     */
    open fun getValue(): DATA? {
        return observeData().blockingFirst()
    }

}