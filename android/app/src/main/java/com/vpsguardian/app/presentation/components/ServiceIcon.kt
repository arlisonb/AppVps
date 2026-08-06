package com.vpsguardian.app.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.vpsguardian.app.domain.model.ServiceType

object ServiceIconMapper {

    fun iconFor(type: ServiceType): ImageVector = when (type) {
        ServiceType.DOCKER, ServiceType.DOCKER_COMPOSE -> Icons.Default.Storage
        ServiceType.PM2 -> Icons.Default.PlayCircle
        ServiceType.SYSTEMD -> Icons.Default.Settings
        ServiceType.FASTAPI -> Icons.Default.Api
        ServiceType.NODEJS, ServiceType.REACT, ServiceType.NEXTJS -> Icons.Default.Code
        ServiceType.PYTHON -> Icons.Default.Code
        ServiceType.REDIS -> Icons.Default.Memory
        ServiceType.POSTGRESQL, ServiceType.MYSQL, ServiceType.MARIADB,
        ServiceType.MONGODB -> Icons.Default.DataObject
        ServiceType.NGINX, ServiceType.APACHE, ServiceType.TRAEFIK -> Icons.Default.Web
        ServiceType.RABBITMQ -> Icons.Default.Hub
        ServiceType.MINIO, ServiceType.PORTAINER -> Icons.Default.Cloud
        ServiceType.WPPCONNECT, ServiceType.EVOLUTION_API -> Icons.Default.Hub
        ServiceType.TYPEBOT, ServiceType.SUPABASE -> Icons.Default.Dashboard
        ServiceType.OPENWEBUI, ServiceType.OLLAMA -> Icons.Default.Memory
        ServiceType.CUSTOM -> Icons.Default.Storage
    }

    fun colorFor(type: ServiceType): Color = when (type) {
        ServiceType.DOCKER, ServiceType.DOCKER_COMPOSE -> Color(0xFF2496ED)
        ServiceType.PM2 -> Color(0xFF2B037A)
        ServiceType.NGINX -> Color(0xFF009639)
        ServiceType.POSTGRESQL -> Color(0xFF336791)
        ServiceType.MYSQL, ServiceType.MARIADB -> Color(0xFF4479A1)
        ServiceType.MONGODB -> Color(0xFF47A248)
        ServiceType.REDIS -> Color(0xFFDC382D)
        ServiceType.FASTAPI -> Color(0xFF009688)
        ServiceType.NODEJS -> Color(0xFF339933)
        ServiceType.EVOLUTION_API, ServiceType.WPPCONNECT -> Color(0xFF25D366)
        ServiceType.TYPEBOT -> Color(0xFF0042DA)
        ServiceType.OLLAMA -> Color(0xFF000000)
        else -> Color(0xFF64B5F6)
    }
}

@Composable
fun ServiceTypeIcon(
    type: ServiceType,
    modifier: Modifier = Modifier,
    tint: Color = ServiceIconMapper.colorFor(type)
) {
    Icon(
        imageVector = ServiceIconMapper.iconFor(type),
        contentDescription = type.name,
        modifier = modifier,
        tint = tint
    )
}
