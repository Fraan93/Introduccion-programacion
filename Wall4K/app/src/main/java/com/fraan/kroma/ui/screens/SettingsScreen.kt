package com.fraan.kroma.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fraan.kroma.BuildConfig
import com.fraan.kroma.data.PremiumPlan
import com.fraan.kroma.data.ThemeMode

@Composable
fun SettingsScreen(
    theme: ThemeMode,
    onSetTheme: (ThemeMode) -> Unit,
    isPremium: Boolean,
    activePlan: PremiumPlan?,
    onOpenPaywall: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Text(
                text = "Ajustes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        SectionTitle("Apariencia")
        ThemeOption("Seguir el sistema", theme == ThemeMode.SYSTEM) { onSetTheme(ThemeMode.SYSTEM) }
        ThemeOption("Oscuro", theme == ThemeMode.DARK) { onSetTheme(ThemeMode.DARK) }
        ThemeOption("Claro", theme == ThemeMode.LIGHT) { onSetTheme(ThemeMode.LIGHT) }

        HorizontalDivider(Modifier.padding(vertical = 12.dp, horizontal = 16.dp))

        SectionTitle("Kroma PRO")
        Text(
            text = if (isPremium) {
                "Plan activo: ${activePlan?.title ?: ""} (${activePlan?.price ?: ""})"
            } else {
                "Sin suscripción activa"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Button(
            onClick = onOpenPaywall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(if (isPremium) "Ver mi plan" else "Hazte PRO")
        }

        HorizontalDivider(Modifier.padding(vertical = 12.dp, horizontal = 16.dp))

        SectionTitle("Acerca de")
        AboutRow("Versión", BuildConfig.VERSION_NAME)
        AboutRow("Catálogos", "Wallhaven · Pexels · Unsplash")
        AboutRow("Contenido", "Solo apto para todos los públicos (SFW)")
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
