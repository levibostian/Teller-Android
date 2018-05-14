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
     * If your repository has some special ID or something to make it unique compared to other instances of your repository, set this tag.
     *
     * This tag is used to keep track of the [Date] that data was last saved to determine how old your data is. If you do not set this [tag] property, all instances of your [LocalRepository] subclass will have the same [Date].
     */
    open val tag: String = "(none)"

    private val dataLastSavedKey by lazy {
        "${ConstantsUtil.PREFIX}_${this::class.java.simpleName}_${tag}_LAST_SAVED_KEY"
    }

    /**
     * Keeps track of how old your data is.
     */
    private var timeSavedData: Date
        get() {
            return Date(Teller.shared.sharedPreferences.getLong(dataLastSavedKey, Date().time))
        }
        @SuppressLint("ApplySharedPref")
        set(value) {
            Teller.shared.sharedPreferences.edit().putLong(dataLastSavedKey, value.time).commit()
        }

    /**
     * Get an observable that gets the current state of data and all future states.
     */
    fun observe(): Observable<LocalDataState<DATA>> {
        if (stateOfDate == null) {
            stateOfDate = LocalDataStateCompoundBehaviorSubject()

            compositeDisposable.add(
                    observeData()
                            .subscribeOn(Schedulers.io())
                            .subscribe({ cachedData ->
                                if (cachedData == null || isDataEmpty(cachedData)) {
                                    stateOfDate!!.onNextEmpty()
                                } else {
                                    stateOfDate!!.onNextData(cachedData, timeSavedData)
                                }
                            }, { error ->
                                stateOfDate!!.onNextError(error)
                            })
            )
        }
        return stateOfDate!!.asObservable()
    }

    /**
     * Save the data to whatever storage method Repository chooses.
     *
     * It is up to you to call [saveData] when you have new data to save. A good place to do this is in a ViewModel.
     */
    fun save(data: DATA?): Completable {
        return Completable.fromAction { timeSavedData = Date() }
                .andThen(saveData(data))
                .subscribeOn(Schedulers.io())
    }

    protected abstract fun saveData(data: DATA?): Completable

    /**
     * This function should be setup to trigger anytime there is a data change. So if you were to call [saveData], anyone observing the [Observable] returned here will get notified of a new update.
     */
    abstract fun observeData(): Observable<DATA?>

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