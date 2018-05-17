package com.levibostian.tellerexample.repository

import com.levibostian.teller.repository.OnlineRepository
import com.levibostian.teller.type.AgeOfData
import com.levibostian.tellerexample.model.AppDatabase
import com.levibostian.tellerexample.model.RepoModel
import com.levibostian.tellerexample.service.GitHubService
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import retrofit2.Retrofit

class ReposRepository(private val service: GitHubService,
                      private val db: AppDatabase): OnlineRepository<List<RepoModel>, ReposRepository.GetRequirements, List<RepoModel>>() {

    override var maxAgeOfData: AgeOfData = AgeOfData(1, AgeOfData.Unit.HOURS)

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

    override fun saveData(data: List<RepoModel>) {
        db.reposDao().insertRepos(data)
    }

    override fun observeCachedData(requirements: GetRequirements): Observable<List<RepoModel>> {
        return db.reposDao().observeReposForUser(requirements.username).toObservable()
    }

    override fun isDataEmpty(data: List<RepoModel>): Boolean = data.isEmpty()

    class GetRequirements(val username: String): GetDataRequirements {
        override var tag: String = "${this::class.java.simpleName}_$username"
    }

}