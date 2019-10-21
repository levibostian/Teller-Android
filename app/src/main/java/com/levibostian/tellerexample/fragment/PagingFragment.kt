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
import com.levibostian.tellerexample.extensions.closeKeyboard
import com.levibostian.tellerexample.service.provider.AppSchedulersProvider
import com.levibostian.tellerexample.util.DataDestroyerUtil
import com.levibostian.tellerexample.util.DependencyUtil

class PagingFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_paging, container, false)
    }

    private fun initialize() {

    }

    override fun onDestroy() {
        super.onDestroy()


    }

}
