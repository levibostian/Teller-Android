package com.levibostian.tellerexample.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.levibostian.tellerexample.R
import com.levibostian.tellerexample.model.RepoModel

class RepoRecyclerViewAdapter(private val repos: List<RepoModel>): RecyclerView.Adapter<RepoRecyclerViewAdapter.ViewHolder>() {

    override fun getItemCount(): Int = repos.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.repo_name_textview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RepoRecyclerViewAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_repo_recyclerview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val repo = repos[position]

        holder.nameTextView.text = repo.name
    }

}