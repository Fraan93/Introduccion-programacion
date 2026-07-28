package com.fraan.kroma.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.fraan.kroma.data.model.Wallpaper
import com.fraan.kroma.ui.Category
import com.fraan.kroma.ui.components.WallpaperStaggeredGrid

@Composable
fun HomeScreen(
    categories: List<Category>,
    selected: Category,
    wallpapers: List<Wallpaper>,
    favorites: Set<String>,
    loading: Boolean,
    isPremium: Boolean,
    onSelectCategory: (Category) -> Unit,
    onSearch: (String) -> Unit,
    onOpen: (Wallpaper) -> Unit,
    onToggleFavorite: (Wallpaper) -> Unit,
    onLoadMore: () -> Unit,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }

    Column(modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 18.dp)
        ) {
            Text(
                text = "Kro",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "ma",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            // PRO pill
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isPremium) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.clickable(onClick = onOpenPremium)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = if (isPremium) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "PRO",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPremium) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
        Text(
            text = "Miles de fondos 4K y 8K",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 12.dp)
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { onSearch(query) }) {
                    Icon(Icons.Outlined.Search, contentDescription = "Buscar")
                }
            },
            placeholder = { Text("Buscar: gato, coche, anime…") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                val locked = cat.label == "8K" && !isPremium
                FilterChip(
                    selected = selected == cat,
                    onClick = { onSelectCategory(cat) },
                    label = { Text(cat.label) },
                    trailingIcon = if (locked) {
                        {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = "Requiere PRO",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        if (wallpapers.isEmpty() && loading) {
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (wallpapers.isEmpty()) {
            EmptyState(message = "No hay resultados. Prueba otra búsqueda o revisa tu conexión.")
        } else {
            WallpaperStaggeredGrid(
                wallpapers = wallpapers,
                favorites = favorites,
                onOpen = onOpen,
                onToggleFavorite = onToggleFavorite,
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
                loading = loading,
                onReachEnd = onLoadMore
            )
        }
    }
}
