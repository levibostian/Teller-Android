package com.levibostian.teller.repository

import android.content.SharedPreferences
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.levibostian.teller.provider.SchedulersProvider
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

internal class LocalRepositoryStub(
        private val sharedPreferences: SharedPreferences,
        schedulersProvider: SchedulersProvider): LocalRepository<String, LocalRepositoryStub.GetRequirements>(schedulersProvider) {

    private val rxSharedPrefs = RxSharedPreferences.create(sharedPreferences)

    // Used for testing. Persist data to shared prefs immediately to be ready for tests to run.
    fun setExistingCache(cache: String) {
        sharedPreferences.edit().putString(CACHE_KEY, cache).commit()
    }

    companion object {
        private const val CACHE_KEY = "CACHE_KEY.LocalRepositoryStub"
    }

    var currentObserveCache_observable: Observable<String>? = null

    var saveCache_results = arrayListOf<String>()
    var saveCache_invoke: ((String) -> Unit)? = null
    var saveCache_count = 0
        get() = saveCache_results.size
        private set

    override fun saveCache(cache: String, requirements: GetRequirements) {
        saveCache_invoke?.invoke(cache)
        saveCache_results.add(cache)

        setExistingCache(cache)
    }

    var observeCachedData_results = arrayListOf<GetRequirements>()
    var observeCachedData_invoke: ((GetRequirements) -> Unit)? = null
    var observeCachedData_count = 0
        get() = observeCachedData_results.size
        private set
    override fun observeCache(requirements: GetRequirements): Observable<String> {
        observeCachedData_results.add(requirements)
        observeCachedData_invoke?.invoke(requirements)

        currentObserveCache_observable = rxSharedPrefs.getString(CACHE_KEY, "").asObservable()

        return currentObserveCache_observable!!
    }

    override fun isCacheEmpty(cache: String, requirements: GetRequirements): Boolean = cache.isEmpty()

    class GetRequirements: LocalRepository.GetCacheRequirements

}