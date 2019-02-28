package com.levibostian.tellerexample.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.levibostian.tellerexample.model.RepoModel
import com.levibostian.tellerexample.model.RepoOwnerModel
import io.reactivex.Flowable
import io.reactivex.Observable

@Dao
interface ReposDao {

    @Query("SELECT * FROM repo WHERE repo_owner_name = :name")
    fun observeReposForUser(name: String): Flowable<List<RepoModel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRepos(repos: List<RepoModel>)

}