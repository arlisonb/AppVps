package com.vpsguardian.app.data.ssh

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.vpsguardian.app.data.session.HostKeyStore
import com.vpsguardian.app.domain.model.VpsCredentials
import java.io.InputStream

class SshConnection private constructor(val session: Session) : AutoCloseable {

    override fun close() {
        if (session.isConnected) session.disconnect()
    }

    fun exec(command: String, timeoutMs: Int = 8_000): String {
        val channel = session.openChannel("exec") as ChannelExec
        channel.setCommand(command)
        channel.connect(timeoutMs)
        try {
            val output = readStream(channel.inputStream, channel, timeoutMs)
            val error = readStream(channel.errStream, channel, 2_000)
            if (channel.exitStatus != 0 && output.isBlank() && error.isNotBlank()) {
                throw Exception(error.trim())
            }
            return output.ifBlank { error }
        } finally {
            channel.disconnect()
        }
    }

    fun execQuiet(command: String, timeoutMs: Int = 6_000): String = try {
        exec(command, timeoutMs)
    } catch (_: Exception) {
        ""
    }

    private fun readStream(input: InputStream, channel: ChannelExec, timeoutMs: Int): String {
        val buffer = ByteArray(4096)
        val output = StringBuilder()
        val start = System.currentTimeMillis()

        while (true) {
            while (input.available() > 0) {
                val read = input.read(buffer)
                if (read < 0) break
                output.append(String(buffer, 0, read, Charsets.UTF_8))
            }

            if (channel.isClosed) {
                while (input.available() > 0) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.append(String(buffer, 0, read, Charsets.UTF_8))
                }
                break
            }

            if (System.currentTimeMillis() - start > timeoutMs) {
                throw Exception("Timeout (${timeoutMs / 1000}s)")
            }
            Thread.sleep(50)
        }
        return output.toString()
    }

    companion object {
        fun connect(credentials: VpsCredentials, hostKeyStore: HostKeyStore? = null): SshConnection {
            val jsch = JSch()
            val session = jsch.getSession(
                credentials.username,
                credentials.ip,
                credentials.sshPort
            )
            session.setPassword(credentials.password)
            session.setConfig("StrictHostKeyChecking", "no")
            session.setConfig("PreferredAuthentications", "password,keyboard-interactive")
            session.setConfig("ServerAliveInterval", "5000")
            session.setConfig("ServerAliveCountMax", "2")
            session.connect(15_000)
            val fingerprint = session.hostKey?.getFingerPrint(jsch).orEmpty()
            hostKeyStore?.verifyOrSave(credentials.ip, credentials.sshPort, fingerprint)
            return SshConnection(session)
        }
    }
}
