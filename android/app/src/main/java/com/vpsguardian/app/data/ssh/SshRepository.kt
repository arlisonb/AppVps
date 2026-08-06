package com.vpsguardian.app.data.ssh

import com.vpsguardian.app.domain.model.VpsCredentials
import com.vpsguardian.app.domain.model.VpsSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SshRepository @Inject constructor(
    private val discovery: SshDiscovery
) {
    suspend fun connect(credentials: VpsCredentials): Result<VpsSession> = withContext(Dispatchers.IO) {
        runCatching {
            SSHClient().use { ssh ->
                ssh.addHostKeyVerifier(PromiscuousVerifier())
                ssh.connect(credentials.ip, credentials.sshPort)
                ssh.authPassword(credentials.username, credentials.password)
                ssh.timeout = 30_000

                val hostname = exec(ssh, "hostname")
                val uname = exec(ssh, "uname -sr")
                val uptime = exec(ssh, "uptime -p 2>/dev/null || uptime")
                val cpuLine = exec(ssh, "top -bn1 | grep 'Cpu(s)' | awk '{print \$2}'")
                val memLine = exec(ssh, "free | awk '/Mem:/ {printf \"%.0f\", \$3/\$2 * 100}'")
                val diskLine = exec(ssh, "df / | awk 'NR==2 {print \$5}' | tr -d '%'")

                val services = discovery.discover(ssh)

                VpsSession(
                    credentials = credentials,
                    hostname = hostname.trim(),
                    osInfo = uname.trim(),
                    uptime = uptime.trim(),
                    cpuPercent = cpuLine.trim().replace(",", ".").toFloatOrNull() ?: 0f,
                    ramPercent = memLine.trim().toFloatOrNull() ?: 0f,
                    diskPercent = diskLine.trim().toFloatOrNull() ?: 0f,
                    services = services
                )
            }
        }
    }

    suspend fun execCommand(credentials: VpsCredentials, command: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                SSHClient().use { ssh ->
                    ssh.addHostKeyVerifier(PromiscuousVerifier())
                    ssh.connect(credentials.ip, credentials.sshPort)
                    ssh.authPassword(credentials.username, credentials.password)
                    exec(ssh, command)
                }
            }
        }

    private fun exec(ssh: SSHClient, command: String): String {
        ssh.startSession().use { session ->
            val cmd = session.exec(command)
            cmd.join(30, TimeUnit.SECONDS)
            val output = cmd.inputStream.bufferedReader().readText()
            val error = cmd.errorStream.bufferedReader().readText()
            if (cmd.exitStatus != 0 && output.isBlank()) {
                throw Exception(error.ifBlank { "Comando falhou: $command" })
            }
            return output.ifBlank { error }
        }
    }
}
