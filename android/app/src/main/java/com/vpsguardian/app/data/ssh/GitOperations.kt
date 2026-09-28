package com.vpsguardian.app.data.ssh

import com.vpsguardian.app.domain.model.VpsCredentials
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitOperations @Inject constructor(
    private val sshRepository: SshRepository
) {
    suspend fun push(credentials: VpsCredentials, projectPath: String, branch: String): Result<String> {
        val safePath = quote(projectPath)
        val safeBranch = quote(branch.ifBlank { "main" }.replace(Regex("[^\\w./-]"), ""))
        return sshRepository.execCommand(
            credentials,
            "cd $safePath && git push origin $safeBranch 2>&1"
        )
    }

    suspend fun commitAndPush(
        credentials: VpsCredentials,
        projectPath: String,
        branch: String,
        message: String
    ): Result<String> {
        val safePath = quote(projectPath)
        val safeBranch = quote(branch.ifBlank { "main" }.replace(Regex("[^\\w./-]"), ""))
        val safeMsg = message.replace("'", "'\\''").ifBlank { "chore: sync from VPS Guardian" }
        return sshRepository.execCommand(
            credentials,
            """
            cd $safePath &&
            git add -A &&
            if git diff --cached --quiet; then
              echo 'Nada para commitar'
            else
              git -c user.email='vps-guardian@local' -c user.name='VPS Guardian' commit -m '$safeMsg' 2>&1
            fi &&
            git push origin $safeBranch 2>&1
            """.trimIndent()
        )
    }

    private fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
}
