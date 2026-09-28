package com.vpsguardian.app.domain.model

enum class GitPushStatus {
    UP_TO_DATE,
    COMMITS_PENDING,
    CHANGES_UNCOMMITTED,
    BOTH_PENDING,
    NO_REMOTE,
    NOT_A_REPO
}

data class GitInfo(
    val branch: String = "",
    val uncommittedFiles: Int = 0,
    val commitsAhead: Int = 0,
    val pendingCommits: List<String> = emptyList(),
    val status: GitPushStatus = GitPushStatus.UP_TO_DATE,
    val lastCommitMessage: String = ""
)

data class ProjectService(
    val name: String,
    val type: ServiceType,
    val status: ServiceStatus,
    val initMethod: String = "",
    val port: Int? = null,
    val id: String = name
)

data class Project(
    val id: String,
    val name: String,
    val path: String,
    val iconKey: String,
    val git: GitInfo = GitInfo(),
    val services: List<ProjectService> = emptyList(),
    val servicesOnline: Int = 0,
    val servicesOffline: Int = 0
) {
    val hasPendingPush: Boolean
        get() = git.status in listOf(
            GitPushStatus.COMMITS_PENDING,
            GitPushStatus.BOTH_PENDING
        )

    val hasLocalChanges: Boolean
        get() = git.uncommittedFiles > 0 || git.commitsAhead > 0

    val overallOnline: Boolean
        get() = services.isNotEmpty() && servicesOffline == 0
}
