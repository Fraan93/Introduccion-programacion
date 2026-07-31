package com.fraan.kroma.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.fraan.kroma.data.model.Wallpaper

/** Subtle animated shimmer shown while a wallpaper image is still loading. */
@Composable
private fun ShimmerBox(modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -600f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-x"
    )
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(x, 0f),
        end = Offset(x + 300f, 300f)
    )
    Box(modifier.background(brush))
}

/** Staggered heights give the feed a natural masonry rhythm. */
private val ratios = listOf(0.62f, 0.56f, 0.70f, 0.60f, 0.66f)

private fun ratioFor(id: String): Float =
    ratios[(id.hashCode().and(Int.MAX_VALUE)) % ratios.size]

/**
 * A clean, image-first card: just the wallpaper plus a small resolution badge.
 * All metadata and actions live in the detail screen.
 */
@Composable
fun WallpaperCard(
    wallpaper: Wallpaper,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(ratioFor(wallpaper.id))
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(wallpaper.thumbUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = wallpaper.title,
                contentScale = ContentScale.Crop,
                loading = { ShimmerBox(Modifier.fillMaxSize()) },
                error = { ShimmerBox(Modifier.fillMaxSize()) },
                modifier = Modifier.fillMaxSize()
            )

            wallpaper.qualityBadge?.let { badge ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
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
        }
    }
}

@Composable
fun WallpaperStaggeredGrid(
    wallpapers: List<Wallpaper>,
    onOpen: (Wallpaper) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    loading: Boolean = false,
    onReachEnd: (() -> Unit)? = null
) {
    val gridState = rememberLazyStaggeredGridState()

    if (onReachEnd != null) {
        LaunchedEffect(gridState, wallpapers.size) {
            snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
                .collect { lastVisible ->
                    if (wallpapers.isNotEmpty() && lastVisible >= wallpapers.size - 4) {
                        onReachEnd()
                    }
                }
        }
    }

    LazyVerticalStaggeredGrid(
        state = gridState,
        columns = StaggeredGridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalItemSpacing = 10.dp,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(wallpapers, key = { it.id }) { wp ->
            WallpaperCard(wallpaper = wp, onClick = { onOpen(wp) })
        }
        if (loading) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
