package com.levibostian.tellerexample.model.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.levibostian.tellerexample.dao.ReposDao
import com.levibostian.tellerexample.model.IssueCommentModel
import com.levibostian.tellerexample.model.RepoModel
import com.levibostian.tellerexample.model.db.converter.DateTypeConverter

@Database(entities = [
    RepoModel::class,
    IssueCommentModel::class], version = 2, exportSchema = true)
@TypeConverters(
        DateTypeConverter::class
)
abstract class AppDatabase: RoomDatabase() {
    abstract fun reposDao(): ReposDao
}
