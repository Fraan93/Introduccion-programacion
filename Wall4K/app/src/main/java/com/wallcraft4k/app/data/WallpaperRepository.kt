package com.wallcraft4k.app.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wallcraft4k.app.data.model.Wallpaper
import com.wallcraft4k.app.data.model.WallpaperSource
import com.wallcraft4k.app.data.remote.FirebaseWallpaperSource
import com.wallcraft4k.app.data.remote.PexelsApi
import com.wallcraft4k.app.data.remote.UnsplashApi
import com.wallcraft4k.app.data.remote.WallhavenApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wall4k")

/**
 * Single source of truth.
 *
 *  - **Browse catalog**: paged, high-resolution wallpapers fetched live from the
 *    wallhaven.cc API ([browse]); falls back to the small bundled [SampleData]
 *    only if the network is unavailable on the first page.
 *  - **Uploads**: shared via Firebase when configured, else stored on-device.
 *  - **Favourites**: the full wallpaper is persisted (so it displays even when the
 *    original page is no longer in memory).
 */
class WallpaperRepository(
    private val appContext: Context,
    private val remote: FirebaseWallpaperSource? = null
) {

    val isRemote: Boolean get() = remote != null

    private val json = Json { ignoreUnknownKeys = true }

    private val favoritesKey = stringPreferencesKey("favorites_json")
    private val uploadsKey = stringPreferencesKey("uploads_json")

    private val uploadsDir: File by lazy {
        File(appContext.filesDir, "uploads").apply { mkdirs() }
    }

    private fun decode(raw: String?): List<Wallpaper> =
        raw?.let { runCatching { json.decodeFromString<List<Wallpaper>>(it) }.getOrDefault(emptyList()) }
            ?: emptyList()

    // ---- Browse (remote catalog) ----

    /**
     * Fetches page [page] (1-based) of wallpapers for [query] (empty = popular),
     * merging THREE catalogs in parallel — Wallhaven (no key needed) plus Pexels
     * and Unsplash (when their free API keys are configured). Results are
     * interleaved so the feed mixes all sources, and filtered to [atleast].
     */
    suspend fun browse(query: String, page: Int, atleast: String = "1080x1920"): List<Wallpaper> {
        val minW = atleast.substringBefore('x').toIntOrNull() ?: 1080
        val minH = atleast.substringAfter('x').toIntOrNull() ?: 1920

        fun List<Wallpaper>.bigEnough() = filter { wp ->
            val w = wp.resolution.substringBefore('x').toIntOrNull() ?: 0
            val h = wp.resolution.substringAfter('x').toIntOrNull() ?: 0
            w >= minW && h >= minH
        }

        val merged = coroutineScope {
            val wallhaven = async { runCatching { WallhavenApi.search(query, page, atleast) }.getOrDefault(emptyList()) }
            val pexels = async { runCatching { PexelsApi.search(query, page) }.getOrDefault(emptyList()) }
            val unsplash = async { runCatching { UnsplashApi.search(query, page) }.getOrDefault(emptyList()) }
            interleave(wallhaven.await(), pexels.await().bigEnough(), unsplash.await().bigEnough())
        }

        return when {
            merged.isNotEmpty() -> merged
            page == 1 -> SampleData.wallpapers // offline fallback
            else -> emptyList()
        }
    }

    /** Round-robin merge so the feed alternates between sources. */
    private fun interleave(vararg lists: List<Wallpaper>): List<Wallpaper> {
        val result = ArrayList<Wallpaper>(lists.sumOf { it.size })
        val iterators = lists.map { it.iterator() }
        var added = true
        while (added) {
            added = false
            for (it in iterators) {
                if (it.hasNext()) {
                    result += it.next()
                    added = true
                }
            }
        }
        return result
    }

    // ---- Favourites ----

    val favoriteWallpapers: Flow<List<Wallpaper>> =
        appContext.dataStore.data.map { decode(it[favoritesKey]) }

    val favoriteIds: Flow<Set<String>> =
        favoriteWallpapers.map { list -> list.map { it.id }.toSet() }

    suspend fun toggleFavorite(wallpaper: Wallpaper) {
        appContext.dataStore.edit { prefs ->
            val current = decode(prefs[favoritesKey])
            val updated = if (current.any { it.id == wallpaper.id }) {
                current.filterNot { it.id == wallpaper.id }
            } else {
                listOf(wallpaper) + current
            }
            prefs[favoritesKey] = json.encodeToString(updated)
        }
    }

    // ---- Uploads ----

    private val localUploads: Flow<List<Wallpaper>> =
        appContext.dataStore.data.map { decode(it[uploadsKey]) }

    /** Community uploads, newest first — from Firebase if configured, else local. */
    val uploads: Flow<List<Wallpaper>> = remote?.communityFeed() ?: localUploads

    /**
     * Publishes a new upload. In remote mode it goes to Firebase (shared with
     * everyone); otherwise it is copied into private storage (this device only).
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
            prefs[uploadsKey] = json.encodeToString(listOf(wallpaper) + decode(prefs[uploadsKey]))
        }
        wallpaper
    }

    suspend fun deleteUpload(id: String) {
        remote?.let {
            it.delete(id)
            removeFavorite(id)
            return
        }
        deleteLocalUpload(id)
    }

    private suspend fun deleteLocalUpload(id: String) = withContext(Dispatchers.IO) {
        File(uploadsDir, "$id.jpg").delete()
        appContext.dataStore.edit { prefs ->
            prefs[uploadsKey] = json.encodeToString(decode(prefs[uploadsKey]).filterNot { it.id == id })
            prefs[favoritesKey] = json.encodeToString(decode(prefs[favoritesKey]).filterNot { it.id == id })
        }
    }

    private suspend fun removeFavorite(id: String) {
        appContext.dataStore.edit { prefs ->
            prefs[favoritesKey] = json.encodeToString(decode(prefs[favoritesKey]).filterNot { it.id == id })
        }
    }
}
