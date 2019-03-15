package com.levibostian.tellerexample.util

import android.content.Context
import androidx.room.Room
import com.levibostian.tellerexample.model.db.AppDatabase
import com.levibostian.tellerexample.service.GitHubService
import io.reactivex.schedulers.Schedulers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class TestDependencyUtil {

    companion object {
        fun testDbInstance(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        }

        fun testServiceInstance(baseUrl: HttpUrl): GitHubService {
            return Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.trampoline()))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(GitHubService::class.java)
        }
    }

}