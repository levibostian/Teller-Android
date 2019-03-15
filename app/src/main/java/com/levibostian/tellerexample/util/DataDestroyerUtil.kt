package com.levibostian.tellerexample.util

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.AsyncTask
import com.levibostian.teller.Teller
import com.levibostian.tellerexample.model.db.AppDatabase
import java.lang.ref.WeakReference

class DataDestroyerUtil(private val teller: Teller,
                        private val database: AppDatabase,
                        private val sharedPreferences: SharedPreferences) {

    fun deleteAllData(complete: () -> Unit) {
        DataDestroyerDestroyAllAsyncTask(this) { error ->
            error?.let { throw it }
            complete()
        }.execute()
    }

    fun deleteTellerData() {
        teller.clear()
    }

    fun deleteDatabase() {
        database.clearAllTables()
    }

    @SuppressLint("ApplySharedPref")
    fun deleteSharedPreferences() {
        sharedPreferences.edit().clear().commit()
    }

    private class DataDestroyerDestroyAllAsyncTask(dataDestroyer: DataDestroyerUtil,
                                                   private val complete: (error: Throwable?) -> Unit?): AsyncTask<Unit?, Unit?, Unit?>() {

        private val dataDestroyerRef: WeakReference<DataDestroyerUtil> = WeakReference(dataDestroyer)

        private var doInBackgroundError: Throwable? = null

        override fun doInBackground(vararg p: Unit?): Unit? {
            try {
                dataDestroyerRef.get()?.let { dataDestroyer ->
                    dataDestroyer.deleteTellerData()
                    dataDestroyer.deleteDatabase()
                    dataDestroyer.deleteSharedPreferences()
                }
            } catch (e: Throwable) {
                doInBackgroundError = e
            }

            return null
        }

        override fun onPostExecute(result: Unit?) {
            super.onPostExecute(result)

            complete(doInBackgroundError)
        }

    }

}