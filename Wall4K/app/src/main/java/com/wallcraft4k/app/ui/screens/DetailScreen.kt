package com.wallcraft4k.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wallcraft4k.app.data.model.Wallpaper
import com.wallcraft4k.app.data.model.WallpaperSource
import com.wallcraft4k.app.util.WallpaperActions
import com.wallcraft4k.app.util.WallpaperTarget
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    wallpaper: Wallpaper,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(wallpaper.fullUrl)
                .crossfade(true)
                .build(),
            contentDescription = wallpaper.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Volver", onClick = onBack)
            Box(Modifier.weight(1f))
            if (onDelete != null) {
                CircleIconButton(Icons.Filled.Delete, "Eliminar") {
                    onDelete()
                    onBack()
                }
            }
        }

        // Bottom info + actions
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.75f)
                    )
                )
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            Text(
                text = wallpaper.title,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${wallpaper.category}  ·  por ${wallpaper.author}",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (busy) return@Button
                        busy = true
                        scope.launch {
                            val ok = WallpaperActions.saveToGallery(context, wallpaper.fullUrl, wallpaper.title)
                            busy = false
                            toast(context, if (ok) "Guardado en la galería" else "No se pudo descargar")
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Text("  Descargar")
                }
                Button(
                    onClick = { if (!busy) showWallpaperDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Wallpaper, contentDescription = null)
                    Text("  Aplicar")
                }
                CircleIconButton(
                    icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorito",
                    tint = if (isFavorite) MaterialTheme.colorScheme.secondary else Color.White,
                    onClick = onToggleFavorite
                )
            }
        }

        if (busy) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }

    if (showWallpaperDialog) {
        AlertDialog(
            onDismissRequest = { showWallpaperDialog = false },
            title = { Text("Aplicar fondo") },
            text = { Text("¿Dónde quieres usar este fondo?") },
            confirmButton = {},
            dismissButton = {
                Column {
                    WallpaperTargetRow("Pantalla de inicio") {
                        showWallpaperDialog = false
                        applyWallpaper(context, scope, wallpaper, WallpaperTarget.HOME) { busy = it }
                    }
                    WallpaperTargetRow("Pantalla de bloqueo") {
                        showWallpaperDialog = false
                        applyWallpaper(context, scope, wallpaper, WallpaperTarget.LOCK) { busy = it }
                    }
                    WallpaperTargetRow("Ambas") {
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
private fun WallpaperTargetRow(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label)
    }
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.35f)
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(24.dp))
        }
    }
}

private fun toast(context: android.content.Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

/** Marker so the compiler keeps [WallpaperSource] import meaningful for uploads. */
internal fun Wallpaper.isUpload() = source == WallpaperSource.UPLOAD
