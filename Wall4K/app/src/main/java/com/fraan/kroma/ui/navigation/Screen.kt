package com.fraan.kroma.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
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
    HOME("home", "Explorar", Icons.Outlined.Home),
    CATEGORIES("categories", "Categorías", Icons.Outlined.GridView),
    FAVORITES("favorites", "Favoritos", Icons.Outlined.Favorite)
}
