package com.levibostian.tellerexample.util

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.room.Room
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.levibostian.teller.Teller
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

        fun testDbInstance(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
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

        fun rxSharedPreferences(context: Context): RxSharedPreferences {
            return RxSharedPreferences.create(sharedPreferences(context))
        }

        fun sharedPreferences(context: Context): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        fun teller(): Teller {
            return Teller.shared
        }

        fun dataDestroyerUtil(context: Context): DataDestroyerUtil {
            return DataDestroyerUtil(
                    teller(),
                    dbInstance(context),
                    sharedPreferences(context))
        }
    }

}