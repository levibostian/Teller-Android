package com.levibostian.tellerexample.fragment

import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import android.content.Context
import android.view.*
import androidx.lifecycle.ViewModelProvider
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.levibostian.tellerexample.adapter.IssueCommentsRecyclerViewAdapter
import com.levibostian.tellerexample.extensions.closeKeyboard
import com.levibostian.tellerexample.service.provider.AppSchedulersProvider
import com.levibostian.tellerexample.util.DataDestroyerUtil
import com.levibostian.tellerexample.util.DependencyUtil
import com.levibostian.tellerexample.viewmodel.IssueCommentsViewModel
import kotlinx.android.synthetic.main.fragment_paging.*

class PagingFragment : Fragment() {

    private lateinit var issueCommentsViewModel: IssueCommentsViewModel
    private lateinit var service: GitHubService
    private lateinit var db: AppDatabase

    private val issueCommentsAdapter = IssueCommentsRecyclerViewAdapter()

    private var updateLastSyncedHandler: Handler? = null
    private var updateLastSyncedRunnable: Runnable? = null

    private var fetchingSnackbar: Snackbar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_paging, container, false)
    }

    override fun onStart() {
        super.onStart()

        service = DependencyUtil.serviceInstance()
        db = DependencyUtil.dbInstance(activity!!.application)

        issueCommentsViewModel = ViewModelProviders.of(this).get(IssueCommentsViewModel::class.java)
        issueCommentsViewModel.init(service, db, AppSchedulersProvider())

        comments_refresh_layout.apply {
            setOnRefreshListener {
                isRefreshing = true

                issueCommentsViewModel.refresh()
                        .subscribe { _ ->
                            isRefreshing = false
                        }
            }

            isEnabled = false
        }

        comments_recyclerview.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = issueCommentsAdapter
        }

        issueCommentsViewModel.observeIssueComments()
                .observe(this, Observer { commentsState ->
                    commentsState.apply {
                        val activity = this@PagingFragment.activity ?: return@apply

                        fetchError?.let { fetchError ->
                            fetchError.message?.let { showEmptyView(it) }

                            AlertDialog.Builder(activity)
                                    .setTitle("Error")
                                    .setMessage(fetchError.message?: "Unknown error. Please, try again.")
                                    .setPositiveButton("Ok") { dialog, _ ->
                                        dialog.dismiss()
                                    }
                                    .create()
                                    .show()
                        }

                        whenNoCache { isFetching, _ ->
                            comments_refresh_layout.isEnabled = false
                            // great place to show empty state where first fetch fails.

                            when {
                                isFetching -> showLoadingView()
                            }
                        }
                        whenCache { cache, lastSuccessfulFetch, isFetching, _, _ ->
                            if (cache == null) {
                                comments_refresh_layout.isEnabled = true
                                showEmptyView()
                            } else {
                                comments_refresh_layout.isEnabled = true

                                showDataView()

                                updateLastSyncedRunnable?.let { updateLastSyncedHandler?.removeCallbacks(it) }
                                updateLastSyncedHandler = Handler()
                                updateLastSyncedRunnable = Runnable {
                                    data_age_textview.text = "Downloaded ${cache.size} comments ${DateUtils.getRelativeTimeSpanString(lastSuccessfulFetch.time, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS)} ago"
                                    updateLastSyncedHandler?.postDelayed(updateLastSyncedRunnable!!, 1000)
                                }
                                updateLastSyncedHandler?.post(updateLastSyncedRunnable!!)

                                issueCommentsAdapter.submitList(cache)
                            }

                            if (isFetching) {
                                fetchingSnackbar = Snackbar.make(frag_paging_parent_view, "Updating comments list...", Snackbar.LENGTH_LONG)
                                fetchingSnackbar?.show()
                            } else {
                                fetchingSnackbar?.dismiss()
                            }
                        }
                    }
                })
    }

    override fun onDestroy() {
        super.onDestroy()

        updateLastSyncedRunnable?.let { updateLastSyncedHandler?.removeCallbacks(it) }
    }

    private fun showLoadingView() {
        loading_view.visibility = View.VISIBLE
        empty_view.visibility = View.GONE
        data_view.visibility = View.GONE
    }

    private fun showEmptyView(message: String = "There are no comments.") {
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
