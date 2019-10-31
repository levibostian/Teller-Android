package com.levibostian.tellerexample.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.levibostian.tellerexample.service.vo.GitHubIssueCommentVo
import java.util.*

@Entity(tableName = "issue_comment")
data class IssueCommentModel(@PrimaryKey var id: Long = 0,
                        var node_id: String = "",
                        var body: String = "",
                        var github_username: String = "",
                        var repo: String = "",
                        var issue_number: Int = 0,
                        @Embedded(prefix = "user_") var user: RepoOwnerModel = RepoOwnerModel(),
                        var created_at: Date = Date(),
                        var updated_at: Date = Date()) {

    companion object {
        fun from(issueCommentVo: GitHubIssueCommentVo, githubUsername: String, repoName: String, issueNumber: Int): IssueCommentModel {
            return IssueCommentModel(
                    issueCommentVo.id,
                    issueCommentVo.node_id,
                    issueCommentVo.body,
                    githubUsername,
                    repoName,
                    issueNumber,
                    issueCommentVo.user,
                    issueCommentVo.created_at,
                    issueCommentVo.updated_at
            )
        }
    }

}