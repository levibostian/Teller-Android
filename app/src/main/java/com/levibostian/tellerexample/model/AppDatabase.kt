package com.levibostian.tellerexample.model

import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.Database
import com.levibostian.tellerexample.dao.ReposDao

@Database(entities = [RepoModel::class], version = 1, exportSchema = true)
abstract class AppDatabase: RoomDatabase() {
    abstract fun reposDao(): ReposDao
}
