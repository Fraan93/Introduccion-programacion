package com.fraan.kroma.data.remote

import com.fraan.kroma.data.model.Wallpaper
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
     * @param query      search terms (empty = top list of everything)
     * @param page       1-based page index (wallhaven returns 24 per page)
     * @param atleast    minimum PORTRAIT resolution "WxH"
     *                   (e.g. 2160x3840 for 4K vertical, 4320x7680 for 8K vertical)
     * @param categories wallhaven category bits general/anime/people
     *                   ("111" = all, "010" = anime only)
     */
    suspend fun search(
        query: String,
        page: Int,
        atleast: String = "1080x1920",
        categories: String = "111",
        sorting: String? = null
    ): List<Wallpaper> = withContext(Dispatchers.IO) {
        runCatching {
            val q = URLEncoder.encode(query, "UTF-8")
            // Explicit sorting wins (e.g. "favorites" = all-time best for sparse
            // ultra-res feeds). Otherwise: query -> relevance, no query -> top of
            // the last year.
            val effectiveSorting = sorting ?: if (query.isBlank()) "toplist" else "relevance"
            val url = buildString {
                append(BASE)
                append("?q=").append(q)
                append("&categories=").append(categories)
                append("&purity=100")
                append("&sorting=").append(effectiveSorting)
                if (effectiveSorting == "toplist" && query.isBlank()) append("&topRange=1y")
                append("&order=desc")
                // Mobile-optimised: portrait-only, phone-shaped wallpapers.
                append("&ratios=portrait")
                append("&atleast=").append(atleast)
                append("&page=").append(page)
            }
            val request = Request.Builder().url(url).header("User-Agent", "Kroma/1.0").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val parsed = json.decodeFromString<WhResponse>(body)
                parsed.data.map { it.toWallpaper(query) }
            }
        }.getOrDefault(emptyList())
    }

    /**
     * Returns the real tags of a wallpaper (e.g. "anime girls", "sports car"),
     * used to find genuinely similar wallpapers. [id] is the raw wallhaven id
     * (without the "wh_" prefix).
     */
    suspend fun tags(id: String): List<String> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://wallhaven.cc/api/v1/w/$id")
                .header("User-Agent", "Kroma/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                json.decodeFromString<WhDetailResponse>(body).data.tags.map { it.name }
            }
        }.getOrDefault(emptyList())
    }

    // ---- JSON models ----

    @Serializable
    private data class WhDetailResponse(val data: WhDetail = WhDetail())

    @Serializable
    private data class WhDetail(val tags: List<WhTag> = emptyList())

    @Serializable
    private data class WhTag(val name: String = "")

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
