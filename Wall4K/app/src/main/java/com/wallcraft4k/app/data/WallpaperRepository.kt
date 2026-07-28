package com.wallcraft4k.app.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wallcraft4k.app.data.model.Wallpaper
import com.wallcraft4k.app.data.model.WallpaperSource
import com.wallcraft4k.app.data.remote.FirebaseWallpaperSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wall4k")

/**
 * Single source of truth. Combines the built-in [SampleData] catalog with
 * community uploads, and tracks favourites.
 *
 * Uploads have two modes, chosen automatically:
 *  - **Remote** (when [remote] != null, i.e. Firebase is configured): uploads are
 *    stored in Cloud Storage + Firestore and shared with every user.
 *  - **Local** (fallback): uploads are copied into the app's private storage and
 *    only visible on this device. Favourites always persist locally via DataStore.
 */
class WallpaperRepository(
    private val appContext: Context,
    private val remote: FirebaseWallpaperSource? = null
) {

    val isRemote: Boolean get() = remote != null

    private val json = Json { ignoreUnknownKeys = true }

    private val favoritesKey = stringSetPreferencesKey("favorites")
    private val uploadsKey = stringPreferencesKey("uploads_json")

    private val uploadsDir: File by lazy {
        File(appContext.filesDir, "uploads").apply { mkdirs() }
    }

    /** Ids the user marked as favourite. */
    val favorites: Flow<Set<String>> =
        appContext.dataStore.data.map { it[favoritesKey] ?: emptySet() }

    private val localUploads: Flow<List<Wallpaper>> =
        appContext.dataStore.data.map { prefs ->
            prefs[uploadsKey]?.let { raw ->
                runCatching { json.decodeFromString<List<Wallpaper>>(raw) }.getOrDefault(emptyList())
            } ?: emptyList()
        }

    /** Community uploads, newest first — from Firebase if configured, else local. */
    val uploads: Flow<List<Wallpaper>> = remote?.communityFeed() ?: localUploads

    /** Catalog + uploads, uploads shown first so new content is visible. */
    val allWallpapers: Flow<List<Wallpaper>> =
        uploads.map { up -> up + SampleData.wallpapers }

    val categories: Flow<List<String>> =
        allWallpapers.map { list -> list.map { it.category }.distinct().sorted() }

    /** Emits list + favourites together for convenient UI consumption. */
    fun feed(): Flow<Pair<List<Wallpaper>, Set<String>>> =
        combine(allWallpapers, favorites) { list, favs -> list to favs }

    suspend fun toggleFavorite(id: String) {
        appContext.dataStore.edit { prefs ->
            val current = prefs[favoritesKey] ?: emptySet()
            prefs[favoritesKey] = if (id in current) current - id else current + id
        }
    }

    /**
     * Publishes a new upload. In remote mode it goes to Firebase (shared with
     * everyone); otherwise it is copied into private storage (this device only).
     * Returns the created [Wallpaper].
     */
    suspend fun addUpload(
        source: Uri,
        title: String,
        author: String,
        category: String
    ): Wallpaper {
        remote?.let { return it.upload(source, title, author, category) }
        return addLocalUpload(source, title, author, category)
    }

    private suspend fun addLocalUpload(
        source: Uri,
        title: String,
        author: String,
        category: String
    ): Wallpaper = withContext(Dispatchers.IO) {
        val id = "up_${UUID.randomUUID()}"
        val dest = File(uploadsDir, "$id.jpg")
        appContext.contentResolver.openInputStream(source)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: error("No se pudo leer la imagen seleccionada")

        val fileUri = Uri.fromFile(dest).toString()
        val wallpaper = Wallpaper(
            id = id,
            title = title.ifBlank { "Sin título" },
            author = author.ifBlank { "Anónimo" },
            category = category.ifBlank { "Comunidad" },
            thumbUrl = fileUri,
            fullUrl = fileUri,
            source = WallpaperSource.UPLOAD
        )

        appContext.dataStore.edit { prefs ->
            val existing = prefs[uploadsKey]?.let {
                runCatching { json.decodeFromString<List<Wallpaper>>(it) }.getOrDefault(emptyList())
            } ?: emptyList()
            prefs[uploadsKey] = json.encodeToString(listOf(wallpaper) + existing)
        }
        wallpaper
    }

    suspend fun deleteUpload(id: String) {
        remote?.let {
            it.delete(id)
            appContext.dataStore.edit { prefs ->
                val favs = prefs[favoritesKey] ?: emptySet()
                if (id in favs) prefs[favoritesKey] = favs - id
            }
            return
        }
        deleteLocalUpload(id)
    }

    private suspend fun deleteLocalUpload(id: String) = withContext(Dispatchers.IO) {
        File(uploadsDir, "$id.jpg").delete()
        appContext.dataStore.edit { prefs ->
            val existing = prefs[uploadsKey]?.let {
                runCatching { json.decodeFromString<List<Wallpaper>>(it) }.getOrDefault(emptyList())
            } ?: emptyList()
            prefs[uploadsKey] = json.encodeToString(existing.filterNot { it.id == id })
            val favs = prefs[favoritesKey] ?: emptySet()
            if (id in favs) prefs[favoritesKey] = favs - id
        }
    }
}
