package com.fraan.kroma.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fraan.kroma.data.PremiumPlan

@Composable
fun PaywallScreen(
    isPremium: Boolean,
    activePlan: PremiumPlan?,
    onActivate: (PremiumPlan) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf(PremiumPlan.LIFETIME) }
    val gold = MaterialTheme.colorScheme.primary

    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF1A1405),
                    0.4f to MaterialTheme.colorScheme.background,
                    1f to MaterialTheme.colorScheme.background
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }

            // Crown-ish header
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(shape = CircleShape, color = gold.copy(alpha = 0.15f)) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = gold,
                        modifier = Modifier.padding(18.dp).size(42.dp)
                    )
                }
                Row(Modifier.padding(top = 14.dp)) {
                    Text(
                        "Kroma ",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "PRO",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = gold
                    )
                }
                Text(
                    text = if (isPremium) {
                        "Ya eres PRO (${activePlan?.title ?: ""}) — gracias por apoyar la app"
                    } else {
                        "Desbloquea todo el catálogo"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )
            }

            // Benefits
            listOf(
                "Categoría 8K exclusiva",
                "Descargas en máxima calidad",
                "Sin anuncios (cuando se activen)",
                "Novedades antes que nadie"
            ).forEach { benefit ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 5.dp)
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = gold,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        benefit,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            // Plans
            PremiumPlan.entries.forEach { plan ->
                PlanCard(
                    plan = plan,
                    selected = selected == plan,
                    highlight = plan == PremiumPlan.LIFETIME,
                    onClick = { selected = plan }
                )
                Spacer(Modifier.height(10.dp))
            }

            Button(
                onClick = { onActivate(selected) },
                enabled = !isPremium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(52.dp)
            ) {
                Text(
                    text = if (isPremium) "PRO activo ✓" else "Continuar — ${selected.price}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Versión de prueba: la compra es simulada y no se cobra dinero real. " +
                    "Los pagos reales se activarán al publicar la app en Google Play.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp)
            )
        }
    }
}

@Composable
private fun PlanCard(
    plan: PremiumPlan,
    selected: Boolean,
    highlight: Boolean,
    onClick: () -> Unit
) {
    val gold = MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) gold else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        plan.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (highlight) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = gold,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                "MEJOR OFERTA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    plan.period,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                plan.price,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = if (selected) gold else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
