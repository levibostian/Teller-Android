package com.levibostian.tellerexample.model

import com.google.gson.annotations.SerializedName

data class RepoOwnerModel(@SerializedName("login") var name: String = "")