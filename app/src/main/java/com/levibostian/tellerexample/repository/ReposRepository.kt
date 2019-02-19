package com.levibostian.tellerexample.repository

import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.teller.type.AgeOfData
import com.levibostian.tellerexample.model.db.AppDatabase
import com.levibostian.tellerexample.model.RepoModel
import com.levibostian.tellerexample.service.GitHubService
import io.reactivex.Observable
import io.reactivex.Single

class ReposRepository(private val service: GitHubService,
                      private val db: AppDatabase): OnlineRepository<List<RepoModel>, ReposRepository.GetReposRequirements, List<RepoModel>>() {

    /**
     * Tells Teller how old cached data can be before it's determined "too old" and new data is fetched automatically by calling `fetchFreshData()`.
     */
    override var maxAgeOfData: AgeOfData = AgeOfData(7, AgeOfData.Unit.DAYS)

    /**
     * Fetch fresh data via a network call.
     * Note: This function is guaranteed to be called on a background thread.
     *
     * You are in charge of performing any error handling and parsing of the network response body.
     *
     * After the network call, you tell Teller if the fetch was successful or a failure. Do this by returning a `FetchResponse` object.
     *
     * If the network call was successful, Teller will cache the data and deliver it to the listeners. If it failed, Teller will deliver your error to the listeners so you can notify your users of errors if you wish.
     */
    override fun fetchFreshData(requirements: GetReposRequirements): Single<FetchResponse<List<RepoModel>>> {
        return service.listRepos(requirements.githubUsername)
                .map { response ->
                    val fetchResponse: FetchResponse<List<RepoModel>>
                    if (!response.isSuccessful) {
                        fetchResponse = when (response.code()) {
                            in 500..600 -> {
                                FetchResponse.fail("The GitHub API is down. Please, try again later.")
                            }
                            404 -> {
                                FetchResponse.fail("The githubUsername ${requirements.githubUsername} does not exist. Try another one.")
                            }
                            else -> {
                                // I do not like when apps say, "Unknown error. Please try again". It's terrible to do. But if it ever happens, that means you need to handle more HTTP status codes. Above are the only ones that I know GitHub will return. They don't document the rest of them, I don't think?
                                FetchResponse.fail("Unknown error. Please, try again.")
                            }
                        }
                    } else {
                        fetchResponse = FetchResponse.success(response.body()!!)
                    }

                    fetchResponse
                }
    }

    /**
     * Save cache data to the local Android device.
     * Note: This function is guaranteed to be called on a background thread.
     *
     * Teller is not opinionated about how you save your new cache data. Use SQLite/Room/Realm, SharedPreferences, Files, Images, whatever!
     */
    override fun saveData(data: List<RepoModel>, requirements: GetReposRequirements) {
        db.reposDao().insertRepos(data)
    }

    /**
     * Return a RxJava2 `Observable` that continuously queries the locally cached data and notifies any observers of changes.
     * Note: This function is guaranteed to be called on the main UI thread.
     *
     * Do *not* send `null` in the `Observable` returned from this function. This is a rule set by RxJava and it will throw an exception.
     * If your cache data is in an "empty" state, either (1) return an empty value (Example: an empty String, "") or (2) create a POJO with an optional inside of it.
     */
    override fun observeCachedData(requirements: GetReposRequirements): Observable<List<RepoModel>> {
        return db.reposDao().observeReposForUser(requirements.githubUsername).toObservable()
    }

    /**
     * Help Teller determine is your cached data is "empty" or not.
     * Note: This function is guaranteed to be called on the same thread as `observeCachedData()`.
     *
     * In our example, an empty List, is what we qualify as empty. If you have a POJO, String, or any other data type, you return back if the `cache` parameter is empty or not.
     */
    override fun isDataEmpty(cache: List<RepoModel>, requirements: GetReposRequirements): Boolean = cache.isEmpty()

    class GetReposRequirements(val githubUsername: String): GetDataRequirements {
        override var tag = "Repos for user:$githubUsername"
    }

}