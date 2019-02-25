package com.levibostian.teller.repository.manager

import com.levibostian.teller.repository.GetCacheRequirementsTag
import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.teller.type.Age
import java.util.*

/**
 * Manager to manage the last time that an instance of [OnlineRepository] with a given [GetCacheRequirementsTag] was last fetched for fresh cache.
 *
 * The [Date]s represented in this manager represent hold old the cache is.
 */
internal interface OnlineRepositoryCacheAgeManager {
    /**
     * Is the last time that the cache identified with the given [tag] older then Now - [maxAge]?
     *
     * Answers the question: "Should cache be refreshed for?"
     */
    fun isCacheTooOld(tag: GetCacheRequirementsTag, maxAge: Age): Boolean

    /**
     * Update the date that cache was fetched under the given [tag].
     */
    fun updateLastSuccessfulFetch(tag: GetCacheRequirementsTag, age: Date)

    /**
     *  Has there ever been a fetch for an [OnlineRepository] with the given [tag]?
     */
    fun hasSuccessfullyFetchedCache(tag: GetCacheRequirementsTag): Boolean

    /**
     * What is the [Date] that the [OnlineRepository] with given [tag] was fetched?
     */
    fun lastSuccessfulFetch(tag: GetCacheRequirementsTag): Date?
}