package com.vpsguardian.app.data.ssh

import com.vpsguardian.app.data.session.HostKeyStore
import com.vpsguardian.app.domain.model.ProjectService
import com.vpsguardian.app.domain.model.ServiceStatus
import com.vpsguardian.app.domain.model.ServiceType
import com.vpsguardian.app.domain.model.VpsCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

data class ServiceHealth(
    val healthy: Boolean,
    val message: String,
    val port: Int? = null,
    val whatsappConnected: Boolean? = null,
    val apiOnline: Boolean = false
)

data class ServiceRuntimeInfo(
    val status: ServiceStatus,
    val uptime: String = "",
    val memoryMb: Float = 0f,
    val cpuPercent: Float = 0f,
    val pid: Int? = null,
    val activeState: String = ""
)

data class ServiceSnapshot(
    val logs: List<String>,
    val runtime: ServiceRuntimeInfo,
    val health: ServiceHealth
)

enum class ServiceControl { START, STOP, RESTART }

@Singleton
class ServiceOperations @Inject constructor(
    private val hostKeyStore: HostKeyStore
) {

    suspend fun snapshot(
        credentials: VpsCredentials,
        service: ProjectService,
        logLines: Int = 80
    ): Result<ServiceSnapshot> = withSsh(credentials) { ssh ->
        snapshotOn(ssh, service, logLines)
    }

    fun snapshotOn(ssh: SshConnection, service: ProjectService, logLines: Int = 80): ServiceSnapshot {
        val runtime = runtimeOn(ssh, service)
        val logs = ssh.execQuiet(logsCommand(service, logLines), 15_000)
            .lines()
            .filter { it.isNotBlank() }
        val health = healthOn(ssh, service, runtime)
        return ServiceSnapshot(logs = logs, runtime = runtime, health = health)
    }

    suspend fun getLogs(
        credentials: VpsCredentials,
        service: ProjectService,
        lines: Int = 80
    ): Result<List<String>> = withSsh(credentials) { ssh ->
        ssh.execQuiet(logsCommand(service, lines), 15_000)
            .lines()
            .filter { it.isNotBlank() }
    }

    suspend fun getRuntimeInfo(
        credentials: VpsCredentials,
        service: ProjectService
    ): Result<ServiceRuntimeInfo> = withSsh(credentials) { ssh ->
        runtimeOn(ssh, service)
    }

    suspend fun healthCheck(
        credentials: VpsCredentials,
        service: ProjectService
    ): Result<ServiceHealth> = withSsh(credentials) { ssh ->
        healthOn(ssh, service)
    }

    fun healthOn(
        ssh: SshConnection,
        service: ProjectService,
        runtime: ServiceRuntimeInfo? = null
    ): ServiceHealth {
        val info = runtime ?: runtimeOn(ssh, service)
        return if (isWppConnect(service)) {
            checkWppConnectHealth(ssh, service, info)
        } else {
            ServiceHealth(
                healthy = info.status == ServiceStatus.ONLINE,
                message = when (info.status) {
                    ServiceStatus.ONLINE -> "Sistema ativo"
                    ServiceStatus.ERROR -> "Sistema com erro"
                    ServiceStatus.STARTING -> "Sistema iniciando"
                    ServiceStatus.OFFLINE, ServiceStatus.STOPPED -> "Sistema parado"
                    else -> "Status desconhecido"
                },
                port = service.port,
                apiOnline = info.status == ServiceStatus.ONLINE
            )
        }
    }

    suspend fun control(
        credentials: VpsCredentials,
        service: ProjectService,
        action: ServiceControl
    ): Result<String> = withSsh(credentials, timeoutMs = 120_000) { ssh ->
        val output = ssh.exec(controlCommand(service, action), 120_000)
        output.trim().ifBlank {
            when (action) {
                ServiceControl.START -> "Sistema iniciado"
                ServiceControl.STOP -> "Sistema parado"
                ServiceControl.RESTART -> "Sistema reiniciado com sucesso"
            }
        }
    }

    suspend fun restart(
        credentials: VpsCredentials,
        service: ProjectService
    ): Result<String> = control(credentials, service, ServiceControl.RESTART)

    suspend fun restartAndWait(
        credentials: VpsCredentials,
        service: ProjectService
    ): Result<String> = withContext(Dispatchers.IO) {
        control(credentials, service, ServiceControl.RESTART).mapCatching { msg ->
            repeat(8) {
                delay(3_000)
                val status = getRuntimeInfo(credentials, service).getOrNull()?.status
                if (status == ServiceStatus.ONLINE) {
                    return@mapCatching "$msg — sistema voltou online"
                }
            }
            "$msg — ainda não confirmou online. Confira os logs."
        }
    }

    fun isWppConnect(service: ProjectService): Boolean {
        if (service.type == ServiceType.WPPCONNECT) return true
        val name = service.name.lowercase()
        return "whatsapp" in name || "wppconnect" in name || "wpp-connect" in name
    }

    private fun runtimeOn(ssh: SshConnection, service: ProjectService): ServiceRuntimeInfo {
        return when (service.initMethod) {
            "systemd" -> parseSystemdRuntime(ssh, service.name)
            "docker" -> parseDockerRuntime(ssh, service.name)
            "pm2" -> parsePm2Runtime(ssh, service.name)
            else -> ServiceRuntimeInfo(status = service.status)
        }
    }

    private fun logsCommand(service: ProjectService, lines: Int): String {
        val name = shellQuote(service.name)
        return when (service.initMethod) {
            "docker" -> "docker logs --tail $lines $name 2>&1"
            "pm2" -> "pm2 logs $name --nostream --lines $lines 2>&1"
            "systemd" -> "journalctl -u $name -n $lines --no-pager 2>&1"
            else -> "journalctl -n $lines --no-pager 2>&1"
        }
    }

    private fun controlCommand(service: ProjectService, action: ServiceControl): String {
        val name = shellQuote(service.name)
        val verb = when (action) {
            ServiceControl.START -> "start"
            ServiceControl.STOP -> "stop"
            ServiceControl.RESTART -> "restart"
        }
        return when (service.initMethod) {
            "docker" -> "docker $verb $name"
            "pm2" -> "pm2 $verb $name"
            "systemd" -> "systemctl $verb $name"
            else -> "systemctl $verb $name"
        }
    }

    private fun parseSystemdRuntime(ssh: SshConnection, name: String): ServiceRuntimeInfo {
        val quoted = shellQuote(name)
        val active = ssh.execQuiet("systemctl is-active $quoted 2>/dev/null", 4_000).trim()
        val show = ssh.execQuiet(
            "systemctl show $quoted -p ActiveState,SubState,MainPID,MemoryCurrent --value 2>/dev/null",
            4_000
        ).lines().map { it.trim() }

        val status = when (active) {
            "active" -> ServiceStatus.ONLINE
            "activating" -> ServiceStatus.STARTING
            "failed" -> ServiceStatus.ERROR
            "inactive" -> ServiceStatus.OFFLINE
            else -> ServiceStatus.UNKNOWN
        }

        val pid = show.getOrNull(2)?.toIntOrNull()?.takeIf { it > 0 }
        val memoryBytes = show.getOrNull(3)?.toLongOrNull() ?: 0L
        val memoryMb = if (memoryBytes > 0) memoryBytes / (1024f * 1024f) else 0f

        var cpu = 0f
        var uptime = ""
        if (pid != null) {
            val psLine = ssh.execQuiet("ps -p $pid -o %cpu,etime --no-headers 2>/dev/null", 3_000).trim()
            val parts = psLine.split(Regex("\\s+"))
            cpu = parts.getOrNull(0)?.replace(",", ".")?.toFloatOrNull() ?: 0f
            uptime = parts.drop(1).joinToString(" ")
        }

        return ServiceRuntimeInfo(
            status = status,
            uptime = uptime,
            memoryMb = memoryMb,
            cpuPercent = cpu,
            pid = pid,
            activeState = show.getOrNull(0) ?: active
        )
    }

    private fun parseDockerRuntime(ssh: SshConnection, name: String): ServiceRuntimeInfo {
        val quoted = shellQuote(name)
        val line = ssh.execQuiet(
            "docker inspect --format '{{.State.Status}}|{{.State.Pid}}' $quoted 2>/dev/null",
            5_000
        ).trim()
        val parts = line.split("|")
        val dockerStatus = parts.getOrNull(0) ?: "unknown"
        val pid = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 }

        return ServiceRuntimeInfo(
            status = when (dockerStatus) {
                "running" -> ServiceStatus.ONLINE
                "restarting" -> ServiceStatus.STARTING
                "exited" -> ServiceStatus.OFFLINE
                else -> ServiceStatus.UNKNOWN
            },
            pid = pid,
            activeState = dockerStatus
        )
    }

    private fun parsePm2Runtime(ssh: SshConnection, name: String): ServiceRuntimeInfo {
        val statusJson = ssh.execQuiet("pm2 jlist 2>/dev/null", 8_000)
        val nameIdx = statusJson.indexOf("\"name\":\"$name\"")
        val nearby = if (nameIdx >= 0) {
            statusJson.substring(nameIdx, (nameIdx + 400).coerceAtMost(statusJson.length))
        } else ""
        val online = nearby.contains("\"status\":\"online\"")

        return ServiceRuntimeInfo(
            status = if (online) ServiceStatus.ONLINE else ServiceStatus.OFFLINE,
            activeState = if (online) "online" else "stopped"
        )
    }

    private fun checkWppConnectHealth(
        ssh: SshConnection,
        service: ProjectService,
        runtime: ServiceRuntimeInfo
    ): ServiceHealth {
        val port = service.port ?: detectPort(ssh, service.name)
        val recentLogs = ssh.execQuiet(logsCommand(service, 40), 10_000).lowercase()
        val tail = recentLogs.takeLast(800)

        val logConnected = when {
            Regex("session closed|not logged|not connected|disconnected|qrcode|waiting for qr|scan the qr")
                .containsMatchIn(tail) -> false
            Regex("is connected|status['\"]?\\s*[:=]\\s*['\"]?connected|logged in|session is connected")
                .containsMatchIn(tail) -> true
            else -> null
        }

        var httpOk = false
        var bodyHint: Boolean? = null
        if (port != null) {
            val curl = ssh.execQuiet(
                """
                for path in / /health /api/status /api/health /session/status /api/whatsapp/check-connection; do
                  code=${'$'}(curl -sf -m 2 -o /tmp/wpp_h.json -w '%{http_code}' http://127.0.0.1:$port${'$'}path 2>/dev/null || true)
                  if echo "${'$'}code" | grep -qE '^2|^3'; then
                    echo "HTTP:${'$'}code"
                    cat /tmp/wpp_h.json 2>/dev/null | head -c 400
                    break
                  fi
                done
                """.trimIndent(),
                8_000
            )
            httpOk = curl.contains("HTTP:2") || curl.contains("HTTP:3")
            val lower = curl.lowercase()
            bodyHint = when {
                "\"connected\":false" in lower || "\"status\":\"disconnected\"" in lower -> false
                "\"connected\":true" in lower || "\"status\":\"connected\"" in lower -> true
                else -> null
            }
        }

        val whatsappConnected = logConnected ?: bodyHint
        val healthy = runtime.status == ServiceStatus.ONLINE && whatsappConnected != false

        val message = when {
            runtime.status != ServiceStatus.ONLINE -> "Sistema parado ou com erro"
            whatsappConnected == false -> "API no ar, mas WhatsApp desconectado — pode precisar de QR"
            whatsappConnected == true -> "WhatsApp conectado"
            httpOk -> "API online na porta $port — sessão não confirmada"
            port != null -> "Sistema online na porta $port (sem confirmação de sessão)"
            else -> "Sistema ${runtime.activeState}"
        }

        return ServiceHealth(
            healthy = healthy,
            message = message,
            port = port,
            whatsappConnected = whatsappConnected,
            apiOnline = httpOk || runtime.status == ServiceStatus.ONLINE
        )
    }

    private fun detectPort(ssh: SshConnection, serviceName: String): Int? {
        val pid = ssh.execQuiet(
            "systemctl show ${shellQuote(serviceName)} -p MainPID --value 2>/dev/null",
            3_000
        ).trim().toIntOrNull()?.takeIf { it > 0 }

        if (pid != null) {
            val fromPid = ssh.execQuiet(
                "ss -tulpn 2>/dev/null | grep \"pid=$pid\" | grep LISTEN | head -1",
                4_000
            )
            extractPort(fromPid)?.let { return it }
        }

        val fromName = ssh.execQuiet(
            "ss -tulpn 2>/dev/null | grep -i '${serviceName.replace("'", "")}' | grep LISTEN | head -1",
            4_000
        )
        return extractPort(fromName)
    }

    private fun extractPort(line: String): Int? {
        val match = Regex(":(\\d{2,5})\\s").find(line) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    private fun shellQuote(value: String): String =
        "'" + value.replace("'", "'\\''") + "'"

    private suspend fun <T> withSsh(
        credentials: VpsCredentials,
        timeoutMs: Long = 45_000,
        block: (SshConnection) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        runCatching {
            withTimeout(timeoutMs) {
                SshConnection.connect(credentials, hostKeyStore).use { ssh ->
                    block(ssh)
                }
            }
        }
    }
}
