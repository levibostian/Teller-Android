package com.levibostian.tellerexample.service

import com.levibostian.tellerexample.model.RepoModel
import com.levibostian.tellerexample.service.vo.GitHubIssueCommentVo
import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubService {

    @GET("users/{user}/repos")
    fun listRepos(@Path("user") user: String): Single<Response<List<RepoModel>>>

    @GET("repos/{owner}/{repo}/issues/{issue_number}/comments")
    fun listIssueComments(@Path("owner") githubUsername: String, @Path("repo") repoName: String, @Path("issue_number") issueNumber: Int, @Query("page") pageNumber: Int): Single<Response<List<GitHubIssueCommentVo>>>

}