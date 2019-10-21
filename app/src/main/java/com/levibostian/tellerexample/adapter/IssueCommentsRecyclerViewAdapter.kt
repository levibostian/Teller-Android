package com.levibostian.tellerexample.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.levibostian.tellerexample.R
import com.levibostian.tellerexample.model.IssueCommentModel
import com.levibostian.tellerexample.model.RepoModel

class IssueCommentsRecyclerViewAdapter: PagedListAdapter<IssueCommentModel, IssueCommentsRecyclerViewAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<IssueCommentModel>() {
            override fun areItemsTheSame(old: IssueCommentModel, new: IssueCommentModel): Boolean = old.id == new.id

            override fun areContentsTheSame(old: IssueCommentModel, new: IssueCommentModel): Boolean = areItemsTheSame(old, new)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.repo_name_textview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_repo_recyclerview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = getItem(position)

        holder.nameTextView.text = "${position + 1} ${comment?.body ?: "Loading comment..."}"
    }

}