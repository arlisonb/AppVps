package com.vpsguardian.app.data.ssh

import com.vpsguardian.app.domain.model.Service
import com.vpsguardian.app.domain.model.ServiceStatus
import com.vpsguardian.app.domain.model.ServiceType
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SshDiscovery @Inject constructor() {

    fun discover(ssh: SshConnection): List<Service> {
        val services = mutableListOf<Service>()
        services.addAll(parseSystemd(ssh.execQuiet("systemctl list-units --type=service --state=active,failed,inactive --no-pager --plain --no-legend 2>/dev/null")))
        services.addAll(parseDocker(ssh.execQuiet("docker ps -a --format '{{.Names}}|{{.Image}}|{{.Status}}|{{.Ports}}' 2>/dev/null")))
        services.addAll(parsePm2(ssh.execQuiet("pm2 jlist 2>/dev/null")))
        services.addAll(parsePorts(ssh.execQuiet("ss -tulpn 2>/dev/null | grep LISTEN")))

        return deduplicate(services)
    }

    private fun parseSystemd(output: String): List<Service> {
        if (output.isBlank()) return emptyList()
        return output.lines().mapNotNull { line ->
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 4) return@mapNotNull null
            val name = parts[0].removeSuffix(".service")
            if (name.contains("@") || name.startsWith("system-")) return@mapNotNull null
            val active = parts[2]
            val type = detectType(name)
            Service(
                id = makeId("systemd", name),
                vpsId = 0,
                name = name,
                type = type,
                icon = type.name,
                status = when (active) {
                    "active" -> ServiceStatus.ONLINE
                    "failed" -> ServiceStatus.ERROR
                    else -> ServiceStatus.OFFLINE
                },
                initMethod = "systemd"
            )
        }
    }

    private fun parseDocker(output: String): List<Service> {
        if (output.isBlank()) return emptyList()
        return output.lines().mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size < 3) return@mapNotNull null
            val name = parts[0].removePrefix("/")
            val image = parts[1]
            val status = parts[2].lowercase()
            val type = detectType("$name $image")
            Service(
                id = makeId("docker", name),
                vpsId = 0,
                name = name,
                type = if (type == ServiceType.CUSTOM) ServiceType.DOCKER else type,
                icon = type.name,
                status = when {
                    status.contains("up") -> ServiceStatus.ONLINE
                    status.contains("restart") -> ServiceStatus.STARTING
                    else -> ServiceStatus.OFFLINE
                },
                initMethod = "docker",
                version = image.substringAfter(":", ""),
                port = extractPort(parts.getOrElse(3) { "" })
            )
        }
    }

    private fun parsePm2(output: String): List<Service> {
        if (output.isBlank() || !output.trimStart().startsWith("[")) return emptyList()
        val services = mutableListOf<Service>()
        val nameRegex = """"name"\s*:\s*"([^"]+)"""".toRegex()
        val statusRegex = """"status"\s*:\s*"([^"]+)"""".toRegex()
        var idx = 0
        nameRegex.findAll(output).forEach { nameMatch ->
            val name = nameMatch.groupValues[1]
            val statusMatch = statusRegex.findAll(output).drop(idx).firstOrNull()
            idx++
            val status = statusMatch?.groupValues?.get(1) ?: "stopped"
            val type = detectType(name)
            services.add(Service(
                id = makeId("pm2", name),
                vpsId = 0,
                name = name,
                type = if (type == ServiceType.CUSTOM) ServiceType.PM2 else type,
                icon = type.name,
                status = when (status) {
                    "online" -> ServiceStatus.ONLINE
                    "errored" -> ServiceStatus.ERROR
                    "launching" -> ServiceStatus.STARTING
                    else -> ServiceStatus.OFFLINE
                },
                initMethod = "pm2"
            ))
        }
        return services
    }

    private fun parsePorts(output: String): List<Service> {
        if (output.isBlank()) return emptyList()
        return output.lines().mapNotNull { line ->
            val portMatch = Regex(":(\\d{2,5})\\s").find(line) ?: return@mapNotNull null
            val port = portMatch.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            if (port in listOf(22, 53, 8443)) return@mapNotNull null
            val procMatch = Regex("\"([^\"]+)\"").findAll(line).lastOrNull()
            val procName = procMatch?.groupValues?.get(1)?.substringAfterLast("/") ?: "port-$port"
            val type = detectType(procName)
            Service(
                id = makeId("port", "$procName-$port"),
                vpsId = 0,
                name = procName,
                type = type,
                icon = type.name,
                status = ServiceStatus.ONLINE,
                initMethod = "process",
                port = port
            )
        }
    }

    private fun detectType(text: String): ServiceType {
        val t = text.lowercase()
        return when {
            "nginx" in t -> ServiceType.NGINX
            "apache" in t || "httpd" in t -> ServiceType.APACHE
            "postgres" in t -> ServiceType.POSTGRESQL
            "mysql" in t -> ServiceType.MYSQL
            "mariadb" in t -> ServiceType.MARIADB
            "mongo" in t -> ServiceType.MONGODB
            "redis" in t -> ServiceType.REDIS
            "rabbitmq" in t -> ServiceType.RABBITMQ
            "traefik" in t -> ServiceType.TRAEFIK
            "minio" in t -> ServiceType.MINIO
            "portainer" in t -> ServiceType.PORTAINER
            "wppconnect" in t || "wpp-connect" in t || "whatsapp" in t -> ServiceType.WPPCONNECT
            "evolution" in t -> ServiceType.EVOLUTION_API
            "typebot" in t -> ServiceType.TYPEBOT
            "supabase" in t -> ServiceType.SUPABASE
            "open-webui" in t || "openwebui" in t -> ServiceType.OPENWEBUI
            "ollama" in t -> ServiceType.OLLAMA
            "fastapi" in t || "uvicorn" in t -> ServiceType.FASTAPI
            "node" in t -> ServiceType.NODEJS
            "next" in t -> ServiceType.NEXTJS
            "react" in t -> ServiceType.REACT
            "python" in t || "gunicorn" in t -> ServiceType.PYTHON
            "docker-compose" in t || "compose" in t -> ServiceType.DOCKER_COMPOSE
            "docker" in t -> ServiceType.DOCKER
            "pm2" in t -> ServiceType.PM2
            else -> ServiceType.CUSTOM
        }
    }

    private fun extractPort(text: String): Int? {
        val match = Regex(":(\\d{2,5})").find(text) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    private fun makeId(source: String, name: String): String {
        val raw = "$source:$name"
        val digest = MessageDigest.getInstance("MD5").digest(raw.toByteArray())
        return digest.take(6).joinToString("") { "%02x".format(it) }
    }

    private fun deduplicate(services: List<Service>): List<Service> {
        val seen = mutableSetOf<String>()
        return services.filter { svc ->
            val key = "${svc.name.lowercase()}:${svc.type}"
            seen.add(key)
        }.sortedBy { it.name.lowercase() }
    }
}
