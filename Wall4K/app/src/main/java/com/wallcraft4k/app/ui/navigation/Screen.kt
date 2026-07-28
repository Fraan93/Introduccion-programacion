package com.wallcraft4k.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Detail : Screen("detail/{id}") {
        fun createRoute(id: String) = "detail/$id"
    }
}

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Explorar", Icons.Outlined.Explore),
    CATEGORIES("categories", "Categorías", Icons.Outlined.GridView),
    UPLOAD("upload", "Subir", Icons.Outlined.CloudUpload),
    FAVORITES("favorites", "Favoritos", Icons.Outlined.Favorite)
}
