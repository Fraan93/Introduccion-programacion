package com.fraan.kroma.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.fraan.kroma.data.model.Wallpaper
import com.fraan.kroma.util.WallpaperActions
import com.fraan.kroma.util.WallpaperTarget
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DetailScreen(
    wallpaper: Wallpaper,
    isFavorite: Boolean,
    related: List<Wallpaper>,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenRelated: (Wallpaper) -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }

    // Tap the image to toggle full-screen preview (lock-screen style mock).
    var previewMode by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(interactionSource = interaction, indication = null) {
                previewMode = !previewMode
            }
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context).data(wallpaper.fullUrl).crossfade(true).build(),
            contentDescription = wallpaper.title,
            contentScale = ContentScale.Crop,
            loading = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (previewMode) {
            // Lock-screen style preview: just the wallpaper + clock, no chrome.
            val now = remember { Date() }
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(now) },
                    color = Color.White,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Light
                )
                Text(
                    text = remember {
                        SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES")).format(now)
                    },
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            return@Box
        }

        // Top bar: back + discreet author credit + optional delete.
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Volver", onClick = onBack)
            Box(Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.35f)
            ) {
                Text(
                    text = wallpaper.author,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
            if (onDelete != null) {
                Box(Modifier.padding(start = 6.dp)) {
                    CircleIconButton(Icons.Filled.Delete, "Eliminar") {
                        onDelete()
                        onBack()
                    }
                }
            }
        }

        // Bottom panel: info + compact actions + related strip.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.35f to Color.Black.copy(alpha = 0.55f),
                        1f to Color.Black.copy(alpha = 0.92f)
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            wallpaper.qualityBadge?.let { badge ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = badge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = wallpaper.category,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = listOf(wallpaper.resolution, "toca la imagen para vista previa")
                    .filter { it.isNotBlank() }
                    .joinToString("  ·  "),
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )

            // Compact action row (Wallcraft-style small buttons).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ActionButton(Icons.Filled.Download, "Descargar", primary = true, modifier = Modifier.weight(1f)) {
                    if (busy) return@ActionButton
                    busy = true
                    scope.launch {
                        val ok = WallpaperActions.saveToGallery(context, wallpaper.fullUrl, wallpaper.category)
                        busy = false
                        toast(context, if (ok) "Guardado en la galería" else "No se pudo descargar")
                    }
                }
                ActionButton(Icons.Filled.Wallpaper, "Aplicar", primary = true, modifier = Modifier.weight(1f)) {
                    if (!busy) showWallpaperDialog = true
                }
                ActionButton(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    "Favorito",
                    modifier = Modifier.weight(1f),
                    onClick = onToggleFavorite
                )
                ActionButton(Icons.Filled.Share, "Compartir", modifier = Modifier.weight(1f)) {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, wallpaper.fullUrl)
                    }
                    context.startActivity(Intent.createChooser(send, "Compartir fondo"))
                }
            }

            // Genuinely similar wallpapers (searched by the image's real tags).
            if (related.isNotEmpty()) {
                Text(
                    text = "Similares",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(related, key = { it.id }) { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .width(96.dp)
                                .height(160.dp)
                                .clickable { onOpenRelated(item) }
                        ) {
                            AsyncImage(
                                model = item.thumbUrl,
                                contentDescription = item.category,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        if (busy) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (showWallpaperDialog) {
        AlertDialog(
            onDismissRequest = { showWallpaperDialog = false },
            title = { Text("Aplicar fondo") },
            text = { Text("Elige dónde usar este fondo:") },
            confirmButton = {},
            dismissButton = {
                Column {
                    TargetRow("Pantalla de inicio") {
                        showWallpaperDialog = false
                        applyWallpaper(context, scope, wallpaper, WallpaperTarget.HOME) { busy = it }
                    }
                    TargetRow("Pantalla de bloqueo") {
                        showWallpaperDialog = false
                        applyWallpaper(context, scope, wallpaper, WallpaperTarget.LOCK) { busy = it }
                    }
                    TargetRow("Ambas pantallas") {
                        showWallpaperDialog = false
                        applyWallpaper(context, scope, wallpaper, WallpaperTarget.BOTH) { busy = it }
                    }
                    TextButton(onClick = { showWallpaperDialog = false }) { Text("Cancelar") }
                }
            }
        )
    }
}

private fun applyWallpaper(
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    wallpaper: Wallpaper,
    target: WallpaperTarget,
    setBusy: (Boolean) -> Unit
) {
    setBusy(true)
    scope.launch {
        val ok = WallpaperActions.setAsWallpaper(context, wallpaper.fullUrl, target)
        setBusy(false)
        toast(context, if (ok) "Fondo aplicado" else "No se pudo aplicar el fondo")
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = if (primary) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (primary) MaterialTheme.colorScheme.onPrimary else Color.White,
                modifier = Modifier.padding(11.dp).size(22.dp)
            )
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun TargetRow(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label)
    }
}

@Composable
private fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.35f)) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(24.dp))
        }
    }
}

private fun toast(context: android.content.Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}
