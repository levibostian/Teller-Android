package com.levibostian.teller.repository.manager

import com.levibostian.teller.repository.GetDataRequirementsTag
import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.teller.type.AgeOfData
import java.util.*

/**
 * Manager to manage the last time that an instance of [OnlineRepository] with a given [GetDataRequirementsTag] was last fetched for fresh data.
 *
 * The [Date]s represented in this manager represent hold old the cache data is.
 */
internal interface OnlineRepositorySyncStateManager {
    /**
     * Is the last time that the cached data identified with the given [tag] older then Now - [maxAgeOfData]?
     *
     * Answers the question: "Should cache data be fetched for?"
     */
    fun isDataTooOld(tag: GetDataRequirementsTag, maxAgeOfData: AgeOfData): Boolean

    /**
     * Update the date that cached data was fetched under the given [tag].
     */
    fun updateAgeOfData(tag: GetDataRequirementsTag, age: Date)

    /**
     *  Has there ever been a fetch for an [OnlineRepository] with the given [tag]?
     */
    fun hasEverFetchedData(tag: GetDataRequirementsTag): Boolean

    /**
     * What is the [Date] that the [OnlineRepository] with given [tag] was fetched?
     */
    fun lastTimeFetchedData(tag: GetDataRequirementsTag): Date?
}