package com.fraan.kroma.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fraan.kroma.data.AiEngine
import com.fraan.kroma.data.AiStyle
import com.fraan.kroma.data.AspectRatio
import com.fraan.kroma.data.model.Wallpaper
import com.fraan.kroma.ui.ExploreIdeas
import com.fraan.kroma.ui.components.WallpaperCard

@Composable
fun CreateScreen(
    prompt: String,
    styles: List<AiStyle>,
    style: AiStyle,
    aspect: AspectRatio,
    engine: AiEngine,
    results: List<Wallpaper>,
    generating: Boolean,
    isPremium: Boolean,
    onPromptChange: (String) -> Unit,
    onStyle: (AiStyle) -> Unit,
    onAspect: (AspectRatio) -> Unit,
    onEngine: (AiEngine) -> Unit,
    onGenerate: () -> Unit,
    onGenerateMore: () -> Unit,
    onOpen: (Wallpaper) -> Unit,
    onOpenPremium: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyStaggeredGridState()

    // Infinite "generate more" when scrolling near the end.
    LaunchedEffect(gridState, results.size) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { lastVisible ->
                if (results.isNotEmpty() && lastVisible >= results.size - 3) onGenerateMore()
            }
    }

    LazyVerticalStaggeredGrid(
        state = gridState,
        columns = StaggeredGridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp),
        verticalItemSpacing = 10.dp,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Controls(
                prompt = prompt,
                styles = styles,
                style = style,
                aspect = aspect,
                engine = engine,
                generating = generating,
                hasResults = results.isNotEmpty(),
                isPremium = isPremium,
                onPromptChange = onPromptChange,
                onStyle = onStyle,
                onAspect = onAspect,
                onEngine = onEngine,
                onGenerate = onGenerate,
                onOpenPremium = onOpenPremium,
                onOpenSettings = onOpenSettings
            )
        }

        if (results.isEmpty() && !generating) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Escribe una idea y pulsa Generar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(results, key = { it.id }) { wp ->
            WallpaperCard(wallpaper = wp, onClick = { onOpen(wp) })
        }

        if (generating) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        if (results.isEmpty()) {
                            Text(
                                "Creando tu fondo…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Controls(
    prompt: String,
    styles: List<AiStyle>,
    style: AiStyle,
    aspect: AspectRatio,
    engine: AiEngine,
    generating: Boolean,
    hasResults: Boolean,
    isPremium: Boolean,
    onPromptChange: (String) -> Unit,
    onStyle: (AiStyle) -> Unit,
    onAspect: (AspectRatio) -> Unit,
    onEngine: (AiEngine) -> Unit,
    onGenerate: () -> Unit,
    onOpenPremium: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("Kro", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(
                "ma", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary
            )
            Text(
                "  AI", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            ProPill(isPremium, onOpenPremium)
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, "Ajustes", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Text(
            "Describe tu fondo y la IA lo crea",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
        )

        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            minLines = 2,
            maxLines = 4,
            placeholder = { Text("Un dragón de fuego sobre una ciudad neón…") },
            modifier = Modifier.fillMaxWidth()
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(ExploreIdeas.quick) { idea ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { onPromptChange(idea) }
                ) {
                    Text(
                        idea,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Label("Estilo")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(styles) { s ->
                FilterChip(
                    selected = s == style,
                    onClick = { onStyle(s) },
                    label = { Text(s.label) },
                    colors = chipColors()
                )
            }
        }

        Label("Formato")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AspectRatio.entries.forEach { a ->
                FilterChip(
                    selected = a == aspect,
                    onClick = { onAspect(a) },
                    label = { Text(a.label) },
                    colors = chipColors()
                )
            }
        }

        Label("Motor")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AiEngine.entries.forEach { e ->
                val locked = e.premium && !isPremium
                FilterChip(
                    selected = e == engine,
                    onClick = { if (locked) onOpenPremium() else onEngine(e) },
                    label = { Text(if (e == AiEngine.HD) "HD · Nano Banana" else e.label) },
                    leadingIcon = if (e.premium) {
                        { Icon(Icons.Filled.Star, null, Modifier.size(16.dp)) }
                    } else null,
                    colors = chipColors()
                )
            }
        }

        Button(
            onClick = onGenerate,
            enabled = !generating,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(52.dp)
        ) {
            if (generating && !hasResults) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(Icons.Filled.AutoAwesome, null, Modifier.size(20.dp))
                Text(
                    "  Generar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
)

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
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
                tint = if (isPremium) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                "PRO",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isPremium) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
