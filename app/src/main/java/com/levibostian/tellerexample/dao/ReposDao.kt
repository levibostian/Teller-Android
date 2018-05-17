package com.levibostian.tellerexample.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import com.levibostian.tellerexample.model.RepoModel
import com.levibostian.tellerexample.model.RepoOwnerModel
import io.reactivex.Flowable
import io.reactivex.Observable

@Dao
interface ReposDao {

    @Query("SELECT * FROM repo WHERE repo_owner_name = :name")
    fun observeReposForUser(name: String): Flowable<List<RepoModel>>

    @Insert
    fun insertRepos(repos: List<RepoModel>)

}