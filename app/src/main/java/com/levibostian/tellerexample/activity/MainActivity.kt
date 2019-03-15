package com.levibostian.tellerexample.activity

import androidx.lifecycle.Observer
import android.os.Bundle
import com.levibostian.tellerexample.R
import com.levibostian.tellerexample.model.db.AppDatabase
import com.levibostian.tellerexample.service.GitHubService
import com.levibostian.tellerexample.viewmodel.ReposViewModel
import android.os.Handler
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.levibostian.tellerexample.adapter.RepoRecyclerViewAdapter
import com.levibostian.tellerexample.viewmodel.GitHubUsernameViewModel
import kotlinx.android.synthetic.main.activity_main.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.levibostian.tellerexample.extensions.closeKeyboard
import com.levibostian.tellerexample.service.provider.AppSchedulersProvider
import com.levibostian.tellerexample.util.DataDestroyerUtil
import com.levibostian.tellerexample.util.DependencyUtil

class MainActivity : AppCompatActivity() {

    private lateinit var reposViewModel: ReposViewModel
    private lateinit var gitHubUsernameViewModel: GitHubUsernameViewModel
    private lateinit var service: GitHubService
    private lateinit var db: AppDatabase
    private lateinit var dataDestroyerUtil: DataDestroyerUtil

    private var updateLastSyncedHandler: Handler? = null
    private var updateLastSyncedRunnable: Runnable? = null

    private var fetchingSnackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        showEmptyView()

        initialize()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        MenuInflater(this).inflate(R.menu.menu_main_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.restart -> {
                dataDestroyerUtil.deleteAllData {
                    Toast.makeText(application, R.string.data_deleted, Toast.LENGTH_LONG).show()

                    val restartActivityIntent = intent
                    finish()
                    startActivity(restartActivityIntent)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initialize() {
        service = DependencyUtil.serviceInstance()
        db = DependencyUtil.dbInstance(application)
        dataDestroyerUtil = DependencyUtil.dataDestroyerUtil(application)

        reposViewModel = ViewModelProviders.of(this).get(ReposViewModel::class.java)
        gitHubUsernameViewModel = ViewModelProviders.of(this).get(GitHubUsernameViewModel::class.java)
        reposViewModel.init(service, db, AppSchedulersProvider())
        gitHubUsernameViewModel.init(this)

        repos_refresh_layout.apply {
            setOnRefreshListener {
                repos_refresh_layout.isRefreshing = true

                reposViewModel.refresh()
                        .subscribe { _ ->
                            repos_refresh_layout.isRefreshing = false
                        }
            }

            isEnabled = false
        }

        reposViewModel.observeRepos()
                .observe(this, Observer { reposState ->
                    reposState.apply {
                        fetchError?.let { fetchError ->
                            fetchError.message?.let { showEmptyView(it) }

                            AlertDialog.Builder(this@MainActivity)
                                    .setTitle("Error")
                                    .setMessage(fetchError.message?: "Unknown error. Please, try again.")
                                    .setPositiveButton("Ok") { dialog, _ ->
                                        dialog.dismiss()
                                    }
                                    .create()
                                    .show()
                        }

                        whenNoCache { isFetching, _ ->
                            repos_refresh_layout.isEnabled = false
                            // great place to show empty state where first fetch fails.

                            when {
                                isFetching -> showLoadingView()
                            }
                        }
                        whenCache { cache, lastSuccessfulFetch, isFetching, _, _ ->
                            if (cache == null) {
                                repos_refresh_layout.isEnabled = true
                                showEmptyView()
                            } else {
                                repos_refresh_layout.isEnabled = true

                                showDataView()

                                updateLastSyncedRunnable?.let { updateLastSyncedHandler?.removeCallbacks(it) }
                                updateLastSyncedHandler = Handler()
                                updateLastSyncedRunnable = Runnable {
                                    data_age_textview.text = "Data last synced ${DateUtils.getRelativeTimeSpanString(lastSuccessfulFetch.time, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS)}"
                                    updateLastSyncedHandler?.postDelayed(updateLastSyncedRunnable!!, 1000)
                                }
                                updateLastSyncedHandler?.post(updateLastSyncedRunnable!!)

                                repos_recyclerview.apply {
                                    layoutManager = LinearLayoutManager(this@MainActivity)
                                    adapter = RepoRecyclerViewAdapter(cache)
                                    setHasFixedSize(true)
                                }
                            }

                            if (isFetching) {
                                fetchingSnackbar = Snackbar.make(parent_view, "Updating repos list...", Snackbar.LENGTH_LONG)
                                fetchingSnackbar?.show()
                            } else {
                                fetchingSnackbar?.dismiss()
                            }
                        }
                    }
                })

        gitHubUsernameViewModel.observeUsername()
                .observe(this, Observer { usernameState ->
                    usernameState.apply {
                        if (cache == null) {
                            username_edittext.setText("", TextView.BufferType.EDITABLE)
                        } else {
                            username_edittext.setText(cache, TextView.BufferType.EDITABLE)
                            reposViewModel.setUsername(cache!!)
                        }
                    }
                })

        go_button.setOnClickListener {
            if (username_edittext.text.isBlank()) {
                username_edittext.error = "Enter a GitHub githubUsername"
            } else {
                gitHubUsernameViewModel.setUsername(username_edittext.text.toString())

                closeKeyboard()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        updateLastSyncedRunnable?.let { updateLastSyncedHandler?.removeCallbacks(it) }
        gitHubUsernameViewModel.dispose()
        reposViewModel.dispose()
    }

    private fun showLoadingView() {
        loading_view.visibility = View.VISIBLE
        empty_view.visibility = View.GONE
        data_view.visibility = View.GONE
    }

    private fun showEmptyView(message: String = "There are no repos.") {
        loading_view.visibility = View.GONE
        empty_view.visibility = View.VISIBLE
        data_view.visibility = View.GONE

        empty_view_textview.text = message
    }

    private fun showDataView() {
        loading_view.visibility = View.GONE
        empty_view.visibility = View.GONE
        data_view.visibility = View.VISIBLE
    }

}
