package com.levibostian.teller.repository.manager

import android.annotation.SuppressLint
import android.content.SharedPreferences
import com.levibostian.teller.Teller
import com.levibostian.teller.repository.GetDataRequirementsTag
import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.teller.type.AgeOfData
import java.util.*

/**
 * Implementation of [OnlineRepositorySyncStateManager] using [SharedPreferences] as storage type. [SharedPreferences] need to be stored in a separate file from the app's default [SharedPreferences].
 *
 * Implementation used by [OnlineRepository] instances in the Teller library.
 */
internal class TellerOnlineRepositorySyncStateManager(private val sharedPreferences: SharedPreferences = Teller.shared.sharedPreferences): OnlineRepositorySyncStateManager {

    companion object {
        internal const val INVALID_LAST_FETCHED_VALUE: Long = -1L
    }

    /**
     * Is the cache data old and a fetch should be performed?
     *
     * @return If data has never been fetched or the cache data age is older then Now - [maxAgeOfData], then return true.
     */
    override fun isDataTooOld(tag: GetDataRequirementsTag, maxAgeOfData: AgeOfData): Boolean {
        if (!hasEverFetchedData(tag)) return true

        return lastTimeFetchedData(tag)!! < maxAgeOfData.toDate()
    }

    /**
     * Persists new [age] to [SharedPreferences].
     */
    @SuppressLint("ApplySharedPref")
    override fun updateAgeOfData(tag: GetDataRequirementsTag, age: Date) {
        sharedPreferences.edit().putLong(tag, age.time).commit()
    }

    /**
     * Determines if a tag is found in [SharedPreferences].
     */
    override fun hasEverFetchedData(tag: GetDataRequirementsTag): Boolean {
        return lastTimeFetchedData(tag) != null
    }

    /**
     * Gets a [Date] object from [SharedPreferences] for Teller lib.
     */
    override fun lastTimeFetchedData(tag: GetDataRequirementsTag): Date? {
        val lastFetchedTime = sharedPreferences.getLong(tag, INVALID_LAST_FETCHED_VALUE)
        if (lastFetchedTime <= INVALID_LAST_FETCHED_VALUE) return null

        return Date(lastFetchedTime)
    }

}