package com.vpsguardian.app.data.ssh

import com.vpsguardian.app.domain.model.GitInfo
import com.vpsguardian.app.domain.model.GitPushStatus
import com.vpsguardian.app.domain.model.Project
import com.vpsguardian.app.domain.model.ProjectService
import com.vpsguardian.app.domain.model.Service
import com.vpsguardian.app.domain.model.ServiceStatus
import com.vpsguardian.app.domain.model.ServiceType
import com.vpsguardian.app.presentation.components.ProjectIconMapper
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectDiscovery @Inject constructor() {

    fun discover(ssh: SshConnection, allServices: List<Service>): List<Project> {
        val pm2Map = parsePm2Paths(ssh.execQuiet("pm2 jlist 2>/dev/null", 8_000))
        val projectPaths = findProjectPaths(ssh)

        return projectPaths.mapNotNull { path ->
            buildProject(ssh, path, pm2Map, allServices)
        }.sortedBy { it.name.lowercase() }
    }

    private fun shouldIgnorePath(path: String): Boolean {
        val lower = path.lowercase()
        val folder = lower.substringAfterLast('/')

        // Backups antigos (ex: /opt/vps-guardian.bak/...)
        if (lower.contains(".bak")) return true

        // Projetos excluídos manualmente
        if (folder in IGNORED_FOLDER_NAMES) return true

        return false
    }

    private fun findProjectPaths(ssh: SshConnection): List<String> {
        val script = """
            for base in /var/www /opt /home /root /srv; do
              [ -d "${'$'}base" ] || continue
              find "${'$'}base" -maxdepth 4 -name .git -type d 2>/dev/null
            done | sed 's|/.git||' | sort -u | head -40
        """.trimIndent()

        return ssh.execQuiet(script, 12_000)
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 3 }
            .filter { !shouldIgnorePath(it) }
            .let { resolveGuardianDuplicates(it) }
            .distinct()
    }

    /** Mantém só o VPS Guardian atual (/opt/vps-guardian), ignora cópias antigas. */
    private fun resolveGuardianDuplicates(paths: List<String>): List<String> {
        val guardians = paths.filter { isGuardianPath(it) }
        if (guardians.size <= 1) return paths

        val keep = guardians.firstOrNull { it == "/opt/vps-guardian" }
            ?: guardians.minBy { it.length }

        return paths.filter { !isGuardianPath(it) || it == keep }
    }

    private fun isGuardianPath(path: String): Boolean {
        val folder = path.lowercase().substringAfterLast('/')
        return folder.contains("vps-guardian") || folder == "appvps"
    }

    private fun buildProject(
        ssh: SshConnection,
        path: String,
        pm2Map: Map<String, String>,
        allServices: List<Service>
    ): Project? {
        val name = path.substringAfterLast('/').ifBlank { return null }
        if (name in IGNORED_NAMES || shouldIgnorePath(path)) return null

        val git = loadGitInfo(ssh, path)
        val linked = linkServices(name, path, pm2Map, allServices)
        val online = linked.count { it.status == ServiceStatus.ONLINE }
        val offline = linked.size - online

        return Project(
            id = makeId(path),
            name = formatDisplayName(name),
            path = path,
            iconKey = name.lowercase(),
            git = git,
            services = linked,
            servicesOnline = online,
            servicesOffline = offline
        )
    }

    private fun loadGitInfo(ssh: SshConnection, path: String): GitInfo {
        val safePath = shellQuote(path)
        val branch = ssh.execQuiet("cd $safePath && git rev-parse --abbrev-ref HEAD 2>/dev/null", 4_000).trim()
        if (branch.isBlank() || branch.contains("fatal")) {
            return GitInfo(status = GitPushStatus.NOT_A_REPO)
        }

        val dirtyCount = ssh.execQuiet(
            "cd $safePath && git status --porcelain 2>/dev/null | wc -l",
            4_000
        ).trim().toIntOrNull() ?: 0

        val ahead = ssh.execQuiet(
            "cd $safePath && git rev-list --count @{u}..HEAD 2>/dev/null || echo 0",
            4_000
        ).trim().toIntOrNull() ?: 0

        val commits = ssh.execQuiet(
            "cd $safePath && git log @{u}..HEAD --oneline 2>/dev/null | head -5",
            4_000
        ).lines().filter { it.isNotBlank() }

        val lastMsg = ssh.execQuiet(
            "cd $safePath && git log -1 --pretty=%s 2>/dev/null",
            3_000
        ).trim()

        val hasRemote = ssh.execQuiet(
            "cd $safePath && git remote get-url origin 2>/dev/null",
            3_000
        ).isNotBlank()

        val status = when {
            !hasRemote -> GitPushStatus.NO_REMOTE
            dirtyCount > 0 && ahead > 0 -> GitPushStatus.BOTH_PENDING
            dirtyCount > 0 -> GitPushStatus.CHANGES_UNCOMMITTED
            ahead > 0 -> GitPushStatus.COMMITS_PENDING
            else -> GitPushStatus.UP_TO_DATE
        }

        return GitInfo(
            branch = branch,
            uncommittedFiles = dirtyCount,
            commitsAhead = ahead,
            pendingCommits = commits,
            status = status,
            lastCommitMessage = lastMsg
        )
    }

    private fun linkServices(
        projectName: String,
        path: String,
        pm2Map: Map<String, String>,
        allServices: List<Service>
    ): List<ProjectService> {
        val key = projectName.lowercase()
        val pathLower = path.lowercase()
        val linked = mutableListOf<ProjectService>()
        val seen = mutableSetOf<String>()

        pm2Map.forEach { (appName, cwd) ->
            if (cwd.lowercase().contains(pathLower) || cwd.lowercase().contains(key)) {
                val svc = allServices.find { it.name.equals(appName, true) && it.initMethod == "pm2" }
                if (seen.add("pm2:$appName")) {
                    linked.add(ProjectService(
                        name = appName,
                        type = resolveType(appName, svc?.type ?: ServiceType.PM2),
                        status = svc?.status ?: ServiceStatus.UNKNOWN,
                        initMethod = "pm2",
                        port = svc?.port,
                        id = svc?.id ?: appName
                    ))
                }
            }
        }

        allServices.forEach { svc ->
            val svcName = svc.name.lowercase()
            val matches = svcName.contains(key) ||
                key.contains(svcName)

            if (matches && seen.add("${svc.initMethod}:${svc.name}")) {
                linked.add(ProjectService(
                    name = svc.name,
                    type = resolveType(svc.name, svc.type),
                    status = svc.status,
                    initMethod = svc.initMethod,
                    port = svc.port,
                    id = svc.id
                ))
            }
        }

        if (linked.isEmpty()) {
            linked.add(ProjectService(
                name = "Repositório",
                type = ServiceType.CUSTOM,
                status = ServiceStatus.ONLINE,
                initMethod = "git"
            ))
        }

        return linked
    }

    private fun resolveType(name: String, fallback: ServiceType): ServiceType {
        if (fallback != ServiceType.CUSTOM && fallback != ServiceType.SYSTEMD) return fallback
        val n = name.lowercase()
        return when {
            "whatsapp" in n || "wppconnect" in n || "wpp-connect" in n -> ServiceType.WPPCONNECT
            else -> fallback
        }
    }

    private fun parsePm2Paths(pm2Json: String): Map<String, String> {
        if (pm2Json.isBlank()) return emptyMap()
        val result = mutableMapOf<String, String>()
        val nameRegex = """"name"\s*:\s*"([^"]+)"""".toRegex()
        val cwdRegex = """"pm_cwd"\s*:\s*"([^"]+)"""".toRegex()
        val names = nameRegex.findAll(pm2Json).map { it.groupValues[1] }.toList()
        val cwds = cwdRegex.findAll(pm2Json).map { it.groupValues[1] }.toList()
        names.forEachIndexed { i, name ->
            cwds.getOrNull(i)?.let { result[name] = it }
        }
        return result
    }

    private fun formatDisplayName(folder: String): String {
        return folder
            .replace("-", " ")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    private fun shellQuote(path: String): String = "'" + path.replace("'", "'\\''") + "'"

    private fun makeId(path: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(path.toByteArray())
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }

    companion object {
        private val IGNORED_FOLDER_NAMES = setOf(
            "vovo-vintage",
            "vovo_vintage",
        )

        private val IGNORED_NAMES = setOf(
            ".git", "node_modules", "venv", "vendor", "dist", "build"
        )
    }
}
