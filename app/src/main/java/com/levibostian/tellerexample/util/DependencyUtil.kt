package com.levibostian.tellerexample.util

import android.app.Application
import android.arch.persistence.room.Room
import android.content.Context
import com.levibostian.tellerexample.model.db.AppDatabase
import com.levibostian.tellerexample.service.GitHubService
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class DependencyUtil {

    companion object {
        fun dbInstance(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "teller-example").build()
        }

        fun serviceInstance(): GitHubService {
            val httpLoggingInterceptor = HttpLoggingInterceptor()
            httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            val client = OkHttpClient.Builder()
                    .addInterceptor(httpLoggingInterceptor)
                    .build()
            return Retrofit.Builder()
                    .client(client)
                    .baseUrl("https://api.github.com/")
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(GitHubService::class.java)
        }
    }

}