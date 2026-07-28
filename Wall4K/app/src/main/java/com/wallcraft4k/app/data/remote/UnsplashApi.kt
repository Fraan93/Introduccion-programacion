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
 * Unsplash — free high-quality photo catalog with an official API
 * (https://unsplash.com/developers). Requires a free Access Key in
 * BuildConfig.UNSPLASH_ACCESS_KEY; with no key this source returns nothing
 * and the app keeps working on the other sources.
 */
object UnsplashApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun search(query: String, page: Int): List<Wallpaper> = withContext(Dispatchers.IO) {
        val key = BuildConfig.UNSPLASH_ACCESS_KEY
        if (key.isBlank()) return@withContext emptyList()
        runCatching {
            val blank = query.isBlank()
            val url = if (blank) {
                "https://api.unsplash.com/photos?per_page=24&page=$page"
            } else {
                val q = URLEncoder.encode(query, "UTF-8")
                "https://api.unsplash.com/search/photos?query=$q&orientation=portrait&per_page=24&page=$page"
            }
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Client-ID $key")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val photos = if (blank) {
                    json.decodeFromString<List<UnPhoto>>(body)
                } else {
                    json.decodeFromString<UnResponse>(body).results
                }
                photos
                    .filter { it.height > it.width } // portrait only
                    .map { it.toWallpaper(query) }
            }
        }.getOrDefault(emptyList())
    }

    @Serializable
    private data class UnResponse(val results: List<UnPhoto> = emptyList())

    @Serializable
    private data class UnPhoto(
        val id: String,
        val width: Int = 0,
        val height: Int = 0,
        val description: String? = null,
        val urls: UnUrls = UnUrls(),
        val user: UnUser = UnUser()
    ) {
        fun toWallpaper(query: String): Wallpaper = Wallpaper(
            id = "un_$id",
            title = description?.takeIf { it.isNotBlank() } ?: "Wallpaper",
            author = "${user.name} · Unsplash".trim(' ', '·'),
            category = query.ifBlank { "Popular" }.replaceFirstChar { it.uppercase() },
            thumbUrl = urls.small.ifBlank { urls.regular },
            fullUrl = urls.full.ifBlank { urls.regular },
            resolution = "${width}x$height"
        )
    }

    @Serializable
    private data class UnUrls(
        val full: String = "",
        val regular: String = "",
        val small: String = ""
    )

    @Serializable
    private data class UnUser(val name: String = "")
}
