package com.levibostian.tellerexample.activity

import android.os.Bundle
import com.levibostian.tellerexample.R
import com.levibostian.tellerexample.model.db.AppDatabase
import com.levibostian.tellerexample.service.GitHubService
import com.levibostian.tellerexample.viewmodel.ReposViewModel
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.levibostian.tellerexample.util.DataDestroyerUtil
import com.levibostian.tellerexample.util.DependencyUtil
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var dataDestroyerUtil: DataDestroyerUtil

    private val navController: NavController
        get() = findNavController(R.id.nav_host_fragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        initialize()
        setupView()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.restart -> {
                dataDestroyerUtil.deleteAllData {
                    Toast.makeText(application, R.string.data_deleted, Toast.LENGTH_LONG).show()

                    val restartActivityIntent = intent
                    finish()
                    startActivity(restartActivityIntent)
                }
                true
            }
            else -> item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
        }
    }

    private fun setupView() {
        setSupportActionBar(toolbar)

        val topLevelDestinations = setOf(R.id.reposForUserFragment, R.id.paging)
        val appBarConfiguration = AppBarConfiguration(topLevelDestinations, drawer_layout)

        setupActionBarWithNavController(navController, appBarConfiguration)
        toolbar.setupWithNavController(navController, appBarConfiguration)
        nav_view.setupWithNavController(navController)
    }

    private fun initialize() {
        dataDestroyerUtil = DependencyUtil.dataDestroyerUtil(application)
    }

}
