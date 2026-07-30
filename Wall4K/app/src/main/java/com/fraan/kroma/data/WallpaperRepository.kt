package com.fraan.kroma.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fraan.kroma.BuildConfig
import com.fraan.kroma.data.model.Wallpaper
import com.fraan.kroma.data.model.WallpaperSource
import com.fraan.kroma.data.remote.FirebaseWallpaperSource
import com.fraan.kroma.data.remote.NanoBananaApi
import com.fraan.kroma.data.remote.PollinationsApi
import kotlinx.coroutines.Dispatchers
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
 *  - **Generation**: the app creates wallpapers with AI from the user's prompt
 *    ([generate]) — free `turbo` or the PRO HD engine — plus a ready-made showcase
 *    feed ([exploreFeed]) and per-image variations ([variationsOf]).
 *  - **Favourites**: the full wallpaper is persisted (so it displays even when the
 *    original page is no longer in memory).
 *  - **Uploads**: kept for optional community sharing via Firebase when configured.
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

    // ---- AI generation (the heart of the app) ----

    /**
     * Generates a page of wallpapers from the user's [prompt] + [style].
     *
     *  - [AiEngine.FAST]: pollinations `turbo` — instant, free, several variations.
     *  - [AiEngine.HD]: the PRO engine. If a Kroma backend is configured it uses
     *    "Nano Banana" (Gemini 2.5 Flash Image); otherwise it falls back to the
     *    pollinations `flux` model at a higher resolution so the option always works.
     */
    suspend fun generate(
        prompt: String,
        style: AiStyle,
        aspect: AspectRatio,
        engine: AiEngine,
        page: Int
    ): List<Wallpaper> = withContext(Dispatchers.IO) {
        val styled = AiStyles.buildPrompt(prompt, style)
        when (engine) {
            AiEngine.FAST -> PollinationsApi.generate(
                styledPrompt = styled,
                fullW = aspect.width, fullH = aspect.height,
                thumbW = aspect.thumbW, thumbH = aspect.thumbH,
                model = "turbo", page = page, count = 1,
                author = "Kroma AI", category = style.label, titlePrompt = prompt
            )
            AiEngine.HD -> {
                val nano = if (BuildConfig.KROMA_BACKEND_URL.isNotBlank()) {
                    NanoBananaApi.generate(
                        BuildConfig.KROMA_BACKEND_URL, BuildConfig.KROMA_PREMIUM_TOKEN,
                        prompt, style, aspect, page, 1
                    )
                } else null
                nano ?: PollinationsApi.generate(
                    styledPrompt = styled,
                    fullW = aspect.hdWidth, fullH = aspect.hdHeight,
                    thumbW = aspect.thumbW, thumbH = aspect.thumbH,
                    model = "flux", page = page, count = 1,
                    author = "Kroma AI · HD", category = style.label, titlePrompt = prompt
                )
            }
        }
    }

    /** Ready-made showcase feed for the Explore tab (free, fast). */
    suspend fun exploreFeed(prompts: List<String>, page: Int): List<Wallpaper> =
        withContext(Dispatchers.IO) {
            val prompt = prompts[page % prompts.size]
            PollinationsApi.generate(
                styledPrompt = "$prompt, phone wallpaper, ultra detailed",
                fullW = 1080, fullH = 1920, thumbW = 512, thumbH = 910,
                model = "turbo", page = page, count = 8,
                author = "Kroma AI", category = "Explorar", titlePrompt = prompt
            )
        }

    /** More variations in the same theme as [wallpaper] (used on the detail screen). */
    suspend fun variationsOf(wallpaper: Wallpaper, page: Int): List<Wallpaper> =
        withContext(Dispatchers.IO) {
            PollinationsApi.generate(
                styledPrompt = "${wallpaper.title}, phone wallpaper, ultra detailed",
                fullW = 1080, fullH = 1920, thumbW = 512, thumbH = 910,
                model = "turbo", page = page, count = 8,
                author = "Kroma AI", category = wallpaper.category, titlePrompt = wallpaper.title
            )
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
