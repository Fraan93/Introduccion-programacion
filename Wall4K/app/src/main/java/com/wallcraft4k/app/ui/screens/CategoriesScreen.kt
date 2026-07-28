package com.wallcraft4k.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wallcraft4k.app.ui.Category

/** Gradient pairs cycled across category tiles for a lively, Wallcraft-like look. */
private val tileGradients = listOf(
    Color(0xFF6C4DF6) to Color(0xFF2E1A8F),
    Color(0xFFF5B301) to Color(0xFF8F5B00),
    Color(0xFF00C6A7) to Color(0xFF005F52),
    Color(0xFFFF5E7A) to Color(0xFF8F1F35),
    Color(0xFF4DA3F6) to Color(0xFF1A4C8F),
    Color(0xFFB44DF6) to Color(0xFF5A1A8F)
)

@Composable
fun CategoriesScreen(
    categories: List<Category>,
    onPick: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        Text(
            text = "Categorías",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(categories) { index, cat ->
                CategoryTile(
                    cat = cat,
                    gradient = tileGradients[index % tileGradients.size],
                    onClick = { onPick(cat) }
                )
            }
        }
    }
}

@Composable
private fun CategoryTile(
    cat: Category,
    gradient: Pair<Color, Color>,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.7f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(gradient.first.copy(alpha = 0.85f), gradient.second)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = cat.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
