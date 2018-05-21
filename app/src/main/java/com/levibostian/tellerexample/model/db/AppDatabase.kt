package com.levibostian.tellerexample.model.db

import android.app.Application
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import com.levibostian.tellerexample.dao.ReposDao
import com.levibostian.tellerexample.model.RepoModel

@Database(entities = [RepoModel::class], version = 1, exportSchema = true)
abstract class AppDatabase: RoomDatabase() {
    abstract fun reposDao(): ReposDao
}
