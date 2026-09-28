package com.vpsguardian.app.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Web
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object ProjectIconMapper {

    data class ProjectIcon(val icon: ImageVector, val color: Color)

    fun iconFor(name: String, path: String): ProjectIcon {
        val key = "$name $path".lowercase()
        return when {
            "iona" in key || "salgado" in key ->
                ProjectIcon(Icons.Default.Restaurant, Color(0xFFE65100))
            "appagenda" in key || "agenda" in key ->
                ProjectIcon(Icons.Default.CalendarMonth, Color(0xFF1565C0))
            "evolution" in key || "wpp" in key || "whatsapp" in key ->
                ProjectIcon(Icons.Default.Chat, Color(0xFF25D366))
            "typebot" in key ->
                ProjectIcon(Icons.Default.Chat, Color(0xFF0042DA))
            "preserv" in key ->
                ProjectIcon(Icons.Default.Store, Color(0xFF6A1B9A))
            "alvorada" in key || "self-service" in key ->
                ProjectIcon(Icons.Default.Store, Color(0xFF2E7D32))
            "vovo" in key || "vintage" in key ->
                ProjectIcon(Icons.Default.ShoppingCart, Color(0xFF795548))
            "ecommerce" in key || "loja" in key || "shop" in key ->
                ProjectIcon(Icons.Default.ShoppingCart, Color(0xFFEF6C00))
            "landing" in key || "site" in key ->
                ProjectIcon(Icons.Default.Web, Color(0xFF00897B))
            "food" in key || "delivery" in key ->
                ProjectIcon(Icons.Default.Fastfood, Color(0xFFD84315))
            else ->
                ProjectIcon(Icons.Default.Folder, Color(0xFF64B5F6))
        }
    }
}
