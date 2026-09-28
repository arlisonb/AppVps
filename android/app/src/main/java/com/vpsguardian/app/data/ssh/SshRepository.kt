package com.vpsguardian.app.data.ssh

import com.vpsguardian.app.data.session.HostKeyStore
import com.vpsguardian.app.domain.model.ProcessInfo
import com.vpsguardian.app.domain.model.VpsCredentials
import com.vpsguardian.app.domain.model.VpsSession
import com.vpsguardian.app.domain.model.WppHealthSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SshRepository @Inject constructor(
    private val discovery: SshDiscovery,
    private val projectDiscovery: ProjectDiscovery,
    private val serviceOperations: ServiceOperations,
    private val hostKeyStore: HostKeyStore
) {
    suspend fun connect(credentials: VpsCredentials): Result<VpsSession> = withContext(Dispatchers.IO) {
        runCatching {
            withTimeout(75_000) {
                SshConnection.connect(credentials, hostKeyStore).use { ssh ->
                    val hostname = ssh.exec("hostname", 5_000)
                    val uname = ssh.exec("uname -sr", 5_000)
                    val uptime = ssh.exec("uptime -p 2>/dev/null || uptime", 5_000)

                    val cpuPercent = parseCpu(ssh)
                    val memLine = ssh.execQuiet(
                        "awk '/MemTotal/{t=\$2} /MemAvailable/{a=\$2} END{if(t>0) printf \"%.0f\", (t-a)*100/t}' /proc/meminfo",
                        4_000
                    )
                    val swapLine = ssh.execQuiet(
                        "awk '/SwapTotal/{t=\$2} /SwapFree/{f=\$2} END{if(t>0) printf \"%.0f\", (t-f)*100/t; else print 0}' /proc/meminfo",
                        4_000
                    )
                    val diskLine = ssh.execQuiet("df / | awk 'NR==2 {print \$5}' | tr -d '%'", 4_000)

                    val services = discovery.discover(ssh)
                    val projects = projectDiscovery.discover(ssh, services)
                    val wppHealth = collectWppHealth(ssh, projects)
                    val topProcesses = parseTopProcesses(ssh)

                    VpsSession(
                        credentials = credentials,
                        hostname = hostname.trim(),
                        osInfo = uname.trim(),
                        uptime = uptime.trim(),
                        cpuPercent = cpuPercent,
                        ramPercent = memLine.trim().toFloatOrNull()?.coerceIn(0f, 100f) ?: 0f,
                        diskPercent = diskLine.trim().toFloatOrNull()?.coerceIn(0f, 100f) ?: 0f,
                        swapPercent = swapLine.trim().toFloatOrNull()?.coerceIn(0f, 100f) ?: 0f,
                        services = services,
                        projects = projects,
                        topProcesses = topProcesses,
                        wppHealth = wppHealth,
                        lastCheck = System.currentTimeMillis()
                    )
                }
            }
        }
    }

    suspend fun execCommand(credentials: VpsCredentials, command: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                withTimeout(30_000) {
                    SshConnection.connect(credentials, hostKeyStore).use { ssh ->
                        ssh.exec(command)
                    }
                }
            }
        }

    private fun parseCpu(ssh: SshConnection): Float {
        val raw = ssh.execQuiet(
            "grep 'cpu ' /proc/stat; sleep 0.4; grep 'cpu ' /proc/stat",
            5_000
        )
        val lines = raw.lines().map { it.trim() }.filter { it.startsWith("cpu ") }
        if (lines.size < 2) return 0f
        fun jiffies(line: String): Pair<Long, Long> {
            val p = line.split(Regex("\\s+")).drop(1).mapNotNull { it.toLongOrNull() }
            if (p.size < 4) return 0L to 0L
            val idle = p[3]
            val total = p.sum()
            return total to idle
        }
        val (t1, i1) = jiffies(lines[0])
        val (t2, i2) = jiffies(lines[1])
        val dt = (t2 - t1).coerceAtLeast(1)
        val di = (i2 - i1).coerceAtLeast(0)
        return ((dt - di) * 100f / dt).coerceIn(0f, 100f)
    }

    private fun parseTopProcesses(ssh: SshConnection): List<ProcessInfo> {
        val raw = ssh.execQuiet(
            "ps -eo pid,pmem,rss,comm --sort=-pmem --no-headers 2>/dev/null | head -8",
            5_000
        )
        return raw.lines().mapNotNull { line ->
            val parts = line.trim().split(Regex("\\s+"), limit = 4)
            if (parts.size < 4) return@mapNotNull null
            val pid = parts[0].toIntOrNull() ?: return@mapNotNull null
            val pmem = parts[1].replace(",", ".").toFloatOrNull() ?: 0f
            val rssKb = parts[2].toFloatOrNull() ?: 0f
            ProcessInfo(
                pid = pid,
                name = parts[3],
                memoryMb = rssKb / 1024f,
                memoryPercent = pmem
            )
        }
    }

    private fun collectWppHealth(
        ssh: SshConnection,
        projects: List<com.vpsguardian.app.domain.model.Project>
    ): Map<String, WppHealthSnapshot> {
        val result = mutableMapOf<String, WppHealthSnapshot>()
        projects.forEach { project ->
            project.services.filter { serviceOperations.isWppConnect(it) }.forEach { service ->
                val health = serviceOperations.healthOn(ssh, service)
                result["${project.id}:${service.name}"] = WppHealthSnapshot(
                    healthy = health.healthy,
                    whatsappConnected = health.whatsappConnected,
                    message = health.message
                )
            }
        }
        return result
    }
}
