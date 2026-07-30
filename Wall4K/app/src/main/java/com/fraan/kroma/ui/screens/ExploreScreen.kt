package com.fraan.kroma.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fraan.kroma.data.model.Wallpaper
import com.fraan.kroma.ui.components.WallpaperStaggeredGrid

@Composable
fun ExploreScreen(
    wallpapers: List<Wallpaper>,
    loading: Boolean,
    onOpen: (Wallpaper) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        ) {
            Text(
                "Explorar",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            "Fondos creados con IA — toca uno para abrirlo o crea el tuyo en la pestaña Crear.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
        )
        WallpaperStaggeredGrid(
            wallpapers = wallpapers,
            onOpen = onOpen,
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
            loading = loading,
            onReachEnd = onLoadMore
        )
    }
}
