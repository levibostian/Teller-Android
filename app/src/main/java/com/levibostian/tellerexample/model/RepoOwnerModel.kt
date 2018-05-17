package com.levibostian.tellerexample.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.google.gson.annotations.SerializedName

class RepoOwnerModel(@SerializedName("login") var name: String = "")