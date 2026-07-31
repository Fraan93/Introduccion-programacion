package com.fraan.kroma.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.fraan.kroma.data.model.Wallpaper
import com.fraan.kroma.ui.Category
import com.fraan.kroma.ui.ResFilter
import com.fraan.kroma.ui.components.WallpaperStaggeredGrid

@Composable
fun HomeScreen(
    categories: List<Category>,
    resFilters: List<ResFilter>,
    selected: Category,
    selectedRes: ResFilter,
    wallpapers: List<Wallpaper>,
    loading: Boolean,
    isPremium: Boolean,
    onSelectCategory: (Category) -> Unit,
    onSelectRes: (ResFilter) -> Unit,
    onSearch: (String) -> Unit,
    onOpen: (Wallpaper) -> Unit,
    onLoadMore: () -> Unit,
    onOpenPremium: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }

    Column(modifier.fillMaxSize()) {

        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 18.dp)
        ) {
            Column {
                Row {
                    Text("Kro", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text(
                        "ma", style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    "Fondos en 4K & FHD",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.weight(1f))
            ProPill(isPremium, onOpenPremium)
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, "Ajustes", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Search
        TextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = ""; onSearch("") }) {
                        Icon(Icons.Outlined.Close, "Borrar")
                    }
                }
            },
            placeholder = { Text("Buscar fondos…") },
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // Resolution segmented control (the headline feature).
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            resFilters.forEach { res ->
                ResChip(
                    label = res.label,
                    selected = res == selectedRes,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectRes(res) }
                )
            }
        }

        // Category chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                CategoryPill(
                    label = cat.label,
                    selected = selected.label == cat.label,
                    onClick = { onSelectCategory(cat) }
                )
            }
        }

        // Grid
        if (wallpapers.isEmpty() && loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (wallpapers.isEmpty()) {
            EmptyState("Sin resultados. Prueba otra categoría, otra resolución o revisa tu conexión.")
        } else {
            WallpaperStaggeredGrid(
                wallpapers = wallpapers,
                onOpen = onOpen,
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
                loading = loading,
                onReachEnd = onLoadMore
            )
        }
    }
}

@Composable
private fun ResChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        label = "resbg"
    )
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = fg
            )
        }
    }
}

@Composable
private fun CategoryPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else MaterialTheme.colorScheme.surface,
        border = if (selected) null
        else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ProPill(isPremium: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (isPremium) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                Icons.Filled.Star, null,
                tint = if (isPremium) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                "PRO",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isPremium) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
