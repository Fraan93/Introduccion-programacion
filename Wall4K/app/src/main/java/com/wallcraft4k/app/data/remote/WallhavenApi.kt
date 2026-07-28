package com.wallcraft4k.app.data.remote

import com.wallcraft4k.app.data.model.Wallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Public wallpaper catalog powered by the free wallhaven.cc API (no key needed).
 * Provides effectively unlimited high-resolution (up to 8K) wallpapers, searchable
 * by tag/category, with pagination — this is what makes the app feel like Wallcraft.
 *
 * Docs: https://wallhaven.cc/help/api
 */
object WallhavenApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    private const val BASE = "https://wallhaven.cc/api/v1/search"

    /**
     * @param query search terms (empty = top list of everything)
     * @param page  1-based page index (wallhaven returns 24 per page)
     */
    suspend fun search(query: String, page: Int): List<Wallpaper> = withContext(Dispatchers.IO) {
        runCatching {
            val q = URLEncoder.encode(query, "UTF-8")
            // categories=111 (general+anime+people), purity=100 (SFW only),
            // at least Full-HD, sorted by top list. Kept broad so results are never empty.
            val url = buildString {
                append(BASE)
                append("?q=").append(q)
                append("&categories=111&purity=100")
                append("&sorting=toplist&order=desc")
                append("&atleast=1920x1080")
                append("&page=").append(page)
            }
            val request = Request.Builder().url(url).header("User-Agent", "Wall4K/1.0").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val parsed = json.decodeFromString<WhResponse>(body)
                parsed.data.map { it.toWallpaper(query) }
            }
        }.getOrDefault(emptyList())
    }

    // ---- JSON models ----

    @Serializable
    private data class WhResponse(val data: List<WhItem> = emptyList())

    @Serializable
    private data class WhItem(
        val id: String,
        val path: String,
        val resolution: String = "",
        @SerialName("file_size") val fileSize: Long = 0,
        val category: String = "",
        val thumbs: WhThumbs = WhThumbs()
    ) {
        fun toWallpaper(query: String): Wallpaper = Wallpaper(
            id = "wh_$id",
            title = resolution.ifBlank { "Wallpaper" },
            author = "wallhaven",
            category = query.ifBlank { "Popular" }.replaceFirstChar { it.uppercase() },
            thumbUrl = thumbs.original.ifBlank { thumbs.small },
            fullUrl = path,
            resolution = resolution
        )
    }

    @Serializable
    private data class WhThumbs(
        val original: String = "",
        val small: String = "",
        val large: String = ""
    )
}
