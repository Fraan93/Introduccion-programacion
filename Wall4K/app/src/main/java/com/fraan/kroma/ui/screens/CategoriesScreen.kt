package com.fraan.kroma.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fraan.kroma.ui.Category

/** A distinct gradient per category so the grid looks designed, not generated. */
private val gradients = listOf(
    listOf(Color(0xFF7F5AF0), Color(0xFF2CB5E8)),
    listOf(Color(0xFFFF6B6B), Color(0xFFFFD93D)),
    listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
    listOf(Color(0xFFEE0979), Color(0xFFFF6A00)),
    listOf(Color(0xFF4776E6), Color(0xFF8E54E9)),
    listOf(Color(0xFF141E30), Color(0xFF243B55)),
    listOf(Color(0xFFF7971E), Color(0xFFFFD200)),
    listOf(Color(0xFF00C6FF), Color(0xFF0072FF)),
    listOf(Color(0xFFDA22FF), Color(0xFF9733EE)),
    listOf(Color(0xFF1D976C), Color(0xFF93F9B9))
)

@Composable
fun CategoriesScreen(
    categories: List<Category>,
    onPick: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
    ) {
        items(categories) { cat ->
            val idx = categories.indexOf(cat)
            val colors = gradients[idx % gradients.size]
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(colors))
                    .clickable { onPick(cat) }
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(
                    cat.label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}
