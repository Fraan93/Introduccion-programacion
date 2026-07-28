package com.wallcraft4k.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wallcraft4k.app.data.model.Wallpaper
import com.wallcraft4k.app.ui.components.WallpaperStaggeredGrid

@Composable
fun FavoritesScreen(
    favoritesList: List<Wallpaper>,
    favorites: Set<String>,
    onOpen: (Wallpaper) -> Unit,
    onToggleFavorite: (Wallpaper) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        Text(
            text = "Favoritos",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        if (favoritesList.isEmpty()) {
            EmptyState(message = "Todavía no guardaste favoritos.\nToca el corazón en cualquier fondo.")
        } else {
            WallpaperStaggeredGrid(
                wallpapers = favoritesList,
                favorites = favorites,
                onOpen = onOpen,
                onToggleFavorite = onToggleFavorite,
                contentPadding = PaddingValues(12.dp)
            )
        }
    }
}
