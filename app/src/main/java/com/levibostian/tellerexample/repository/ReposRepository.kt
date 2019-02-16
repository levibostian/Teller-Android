package com.levibostian.tellerexample.repository

import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.teller.type.AgeOfData
import com.levibostian.tellerexample.model.db.AppDatabase
import com.levibostian.tellerexample.model.RepoModel
import com.levibostian.tellerexample.service.GitHubService
import io.reactivex.Observable
import io.reactivex.Single

class ReposRepository(private val service: GitHubService,
                      private val db: AppDatabase): OnlineRepository<List<RepoModel>, ReposRepository.GetRequirements, List<RepoModel>>() {

    // This property tells Teller how old cached data can be before it's determined "too old" and new data is fetched automatically by calling `fetchFreshData()`.
    // If you ever need to manually fetch fresh data and ignore this property, you may do so by calling `.sync(true)` on any `OnlineRepository` subclass to force a sync.
    override var maxAgeOfData: AgeOfData = AgeOfData(1, AgeOfData.Unit.DAYS)

    // Fetch fresh data via a network call. You are in charge of performing any error handling and parsing of the network call body.
    // After the network call, you tell Teller if the fetch was successful or a failure. If successful, Teller will cache the data and deliver it to the listeners. If it fails, Teller will deliver your error to the listeners so you can notify your users of errors if you wish.
    override fun fetchFreshData(requirements: GetRequirements): Single<FetchResponse<List<RepoModel>>> {
        return service.listRepos(requirements.username)
                .map { response ->
                    val fetchResponse: FetchResponse<List<RepoModel>>
                    if (!response.isSuccessful) {
                        fetchResponse = when (response.code()) {
                            in 500..600 -> {
                                FetchResponse.fail("The GitHub API is down. Please, try again later.")
                            }
                            404 -> {
                                FetchResponse.fail("The username ${requirements.username} does not exist. Try another one.")
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

    override fun saveData(data: List<RepoModel>, requirements: GetRequirements) {
        db.reposDao().insertRepos(data)
    }

    // Using RxJava2 Observables, query cached data on the device.
    override fun observeCachedData(requirements: GetRequirements): Observable<List<RepoModel>> {
        return db.reposDao().observeReposForUser(requirements.username).toObservable()
    }

    // Help Teller to determine if data is empty or not. Teller uses this when parsing the cache to determine if a data set is empty or not.
    override fun isDataEmpty(cache: List<RepoModel>, requirements: GetRequirements): Boolean = cache.isEmpty()

    class GetRequirements(val username: String): GetDataRequirements {
        override var tag: String = "Repos for user:$username"
    }

}