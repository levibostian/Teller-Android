package com.levibostian.tellerexample.dao

import androidx.paging.DataSource
import androidx.paging.PagedList
import androidx.room.*
import com.levibostian.tellerexample.model.IssueCommentModel
import com.levibostian.tellerexample.model.RepoModel
import com.levibostian.tellerexample.model.RepoOwnerModel
import io.reactivex.Flowable
import io.reactivex.Observable

@Dao
interface ReposDao {

    @Query("SELECT * FROM repo WHERE repo_owner_name = :name")
    fun observeReposForUser(name: String): Observable<List<RepoModel>>

    @Query("SELECT * FROM issue_comment WHERE github_username = :username AND repo = :repoName AND issue_number = :issueNumber")
    fun observeIssueCommentsForRepo(username: String, repoName: String, issueNumber: Int): DataSource.Factory<Int, IssueCommentModel>

    @Query("SELECT * FROM issue_comment WHERE github_username = :username AND repo = :repoName AND issue_number = :issueNumber")
    fun getIssueCommentsForRepo(username: String, repoName: String, issueNumber: Int): List<IssueCommentModel>

    @Delete
    fun deleteIssueComments(comments: List<IssueCommentModel>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRepos(repos: List<RepoModel>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertIssueComments(repos: List<IssueCommentModel>)

}