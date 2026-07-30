package com.fraan.kroma.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Detail : Screen("detail/{id}") {
        fun createRoute(id: String) = "detail/$id"
    }

    data object Paywall : Screen("paywall")

    data object Settings : Screen("settings")
}

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    CREATE("create", "Crear", Icons.Outlined.AutoAwesome),
    EXPLORE("explore", "Explorar", Icons.Outlined.Explore),
    FAVORITES("favorites", "Favoritos", Icons.Outlined.Favorite)
}
