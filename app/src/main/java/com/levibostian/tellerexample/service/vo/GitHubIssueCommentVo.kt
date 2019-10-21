package com.levibostian.tellerexample.service.vo

import com.levibostian.tellerexample.model.RepoOwnerModel
import java.util.*

data class GitHubIssueCommentVo(var id: Long = 0,
                                var node_id: String = "",
                                var body: String = "",
                                var user: RepoOwnerModel = RepoOwnerModel(),
                                var created_at: Date = Date(),
                                var updated_at: Date = Date())