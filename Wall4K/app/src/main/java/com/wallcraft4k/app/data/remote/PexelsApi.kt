package com.wallcraft4k.app.data.remote

import com.wallcraft4k.app.BuildConfig
import com.wallcraft4k.app.data.model.Wallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Pexels — free stock-photo catalog with an official API (https://www.pexels.com/api/).
 * Requires a free API key in BuildConfig.PEXELS_API_KEY; when the key is empty this
 * source simply returns no results and the app runs on the other sources.
 */
object PexelsApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun search(query: String, page: Int): List<Wallpaper> = withContext(Dispatchers.IO) {
        val key = BuildConfig.PEXELS_API_KEY
        if (key.isBlank()) return@withContext emptyList()
        runCatching {
            val url = if (query.isBlank()) {
                "https://api.pexels.com/v1/curated?per_page=24&page=$page"
            } else {
                val q = URLEncoder.encode(query, "UTF-8")
                "https://api.pexels.com/v1/search?query=$q&orientation=portrait&per_page=24&page=$page"
            }
            val request = Request.Builder().url(url).header("Authorization", key).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                json.decodeFromString<PxResponse>(body).photos
                    .filter { it.height > it.width } // portrait only (curated feed is mixed)
                    .map { it.toWallpaper(query) }
            }
        }.getOrDefault(emptyList())
    }

    @Serializable
    private data class PxResponse(val photos: List<PxPhoto> = emptyList())

    @Serializable
    private data class PxPhoto(
        val id: Long,
        val width: Int = 0,
        val height: Int = 0,
        val photographer: String = "",
        val alt: String? = null,
        val src: PxSrc = PxSrc()
    ) {
        fun toWallpaper(query: String): Wallpaper = Wallpaper(
            id = "px_$id",
            title = alt?.takeIf { it.isNotBlank() } ?: "Wallpaper",
            author = "$photographer · Pexels".trim(' ', '·'),
            category = query.ifBlank { "Popular" }.replaceFirstChar { it.uppercase() },
            thumbUrl = src.portrait.ifBlank { src.large },
            fullUrl = src.original,
            resolution = "${width}x$height"
        )
    }

    @Serializable
    private data class PxSrc(
        val original: String = "",
        val large: String = "",
        val portrait: String = ""
    )
}
