package com.fraan.kroma.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, DARK, LIGHT }

/** App-level user preferences (theme, etc.). */
class SettingsRepository(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme")

    val theme: Flow<ThemeMode> = context.settingsStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[themeKey] ?: "") }.getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.settingsStore.edit { it[themeKey] = mode.name }
    }
}
