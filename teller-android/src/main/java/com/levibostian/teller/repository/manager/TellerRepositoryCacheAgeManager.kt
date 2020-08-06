package com.levibostian.teller.repository.manager

import android.annotation.SuppressLint
import android.content.SharedPreferences
import com.levibostian.teller.Teller
import com.levibostian.teller.repository.GetCacheRequirementsTag
import com.levibostian.teller.repository.TellerRepository
import com.levibostian.teller.type.Age
import java.util.*

/**
 * Implementation of [RepositoryCacheAgeManager] using [SharedPreferences] as storage type. [SharedPreferences] need to be stored in a separate file from the app's default [SharedPreferences].
 *
 * Implementation used by [TellerRepository] instances in the Teller library.
 */
internal class TellerRepositoryCacheAgeManager(private val sharedPreferences: SharedPreferences = Teller.shared.sharedPreferences!!): RepositoryCacheAgeManager {

    companion object {
        internal const val INVALID_LAST_FETCHED_VALUE: Long = -1L
    }

    /**
     * Is the cache old and a fetch should be performed?
     *
     * @return If cache has never been fetched or the cache age is older then Now - [maxAge], then return true.
     */
    override fun isCacheTooOld(tag: GetCacheRequirementsTag, maxAge: Age): Boolean {
        if (!hasSuccessfullyFetchedCache(tag)) return true

        return lastSuccessfulFetch(tag)!! < maxAge.toDate()
    }

    /**
     * Persists new [age] to [SharedPreferences].
     */
    @SuppressLint("ApplySharedPref")
    override fun updateLastSuccessfulFetch(tag: GetCacheRequirementsTag, age: Date) {
        sharedPreferences.edit().putLong(tag, age.time).commit()
    }

    /**
     * Determines if a tag is found in [SharedPreferences].
     */
    override fun hasSuccessfullyFetchedCache(tag: GetCacheRequirementsTag): Boolean {
        return lastSuccessfulFetch(tag) != null
    }

    /**
     * Gets a [Date] object from [SharedPreferences] for Teller lib.
     */
    override fun lastSuccessfulFetch(tag: GetCacheRequirementsTag): Date? {
        val lastFetchedTime = sharedPreferences.getLong(tag, INVALID_LAST_FETCHED_VALUE)
        if (lastFetchedTime <= INVALID_LAST_FETCHED_VALUE) return null

        return Date(lastFetchedTime)
    }

}