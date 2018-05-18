package com.levibostian.teller.datastate

import com.levibostian.teller.repository.OnlineRepository
import java.util.*

interface OnlineDataStateListener<in DATA> {
    fun firstFetchOfData()
    /**
     * @param errorDuringFetch Error that occurred during the fetch of getting first set of cacheData. It is up to you to capture this error and determine how to show it to the user. It's best practice that when there is an error here, you will dismiss a loading UI if you are showing one since [firstFetchOfData] was called before.
     */
    fun finishedFirstFetchOfData(errorDuringFetch: Throwable?)
    fun cacheEmpty()
    fun cacheData(data: DATA, fetched: Date)
    fun fetchingFreshData()
    /**
     * @param errorDuringFetch Error that occurred during the fetch of getting first set of cacheData. It is up to you to capture this error and determine how to show it to the user. Since it is during the fetch only, it's best practice here to show this error to the user, but in a UI independent from showing the empty or cached data UI since [cacheEmpty] or [cacheData] will have been called as well in the listener. If you have a situation where an error occur during fetching fresh data but it's not super important to show to the user, it's something you only want to show for a few seconds and then dismiss, or you need to show the user in a UI that forces them to acknowledge it, it's best practice for you to create a [Throwable] subclass and return that in [OnlineRepository.FetchResponse.fail] which will then show up here. You can then parse the [Throwable] subclass in your listener to determine what to do with the error. It's also best practice to create custom views/custom dialog fragments to handle errors so you only need to write code once to handle specific errors across your app.
     */
    fun finishedFetchingFreshData(errorDuringFetch: Throwable?)
}