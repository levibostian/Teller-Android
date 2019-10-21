package com.levibostian.tellerexample.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration1: Migration(1, 2) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `issue_comment` (`id` INTEGER NOT NULL, `node_id` TEXT NOT NULL, `body` TEXT NOT NULL, `github_username` TEXT NOT NULL, `repo` TEXT NOT NULL, `issue_number` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `user_name` TEXT NOT NULL, PRIMARY KEY(`id`))")
    }

}