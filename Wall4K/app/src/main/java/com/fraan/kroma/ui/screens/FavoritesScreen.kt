package com.fraan.kroma.ui.screens

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
import com.fraan.kroma.data.model.Wallpaper
import com.fraan.kroma.ui.components.WallpaperStaggeredGrid

@Composable
fun FavoritesScreen(
    favoritesList: List<Wallpaper>,
    onOpen: (Wallpaper) -> Unit,
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
                onOpen = onOpen,
                contentPadding = PaddingValues(12.dp)
            )
        }
    }
}
