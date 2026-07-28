package com.fraan.kroma.data.remote

import com.fraan.kroma.data.model.Wallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Reads curated phone wallpapers from Reddit's public JSON feeds — the same
 * source popular wallpaper apps use (r/MobileWallpaper, r/Amoledbackgrounds,
 * r/AnimeWallpaper…). Free, no API key. This is what gives the app the
 * "made-for-mobile, same-theme" look instead of a generic image board.
 *
 * Pagination uses Reddit's `after` cursor. `raw_json=1` returns un-escaped URLs.
 */
object RedditWallpaperApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    data class Page(val items: List<Wallpaper>, val nextAfter: String?)

    /**
     * @param multi    one or more subreddits joined with '+', e.g. "MobileWallpaper+iphonewallpapers"
     * @param sort     "hot" | "top" | "new"
     * @param time     time range for "top": "all" | "year" | "month"
     * @param after    cursor from the previous page (null for the first page)
     * @param category label to tag the resulting wallpapers with
     */
    suspend fun fetch(
        multi: String,
        sort: String,
        time: String,
        after: String?,
        category: String
    ): Page = withContext(Dispatchers.IO) {
        runCatching {
            val url = buildString {
                append("https://www.reddit.com/r/").append(multi).append("/").append(sort).append(".json")
                append("?limit=50&raw_json=1&t=").append(time)
                if (!after.isNullOrBlank()) append("&after=").append(after)
            }
            val request = Request.Builder()
                .url(url)
                // Reddit requires a unique, descriptive User-Agent or it returns 429.
                .header("User-Agent", "android:com.fraan.kroma:1.9 (wallpaper app)")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Page(emptyList(), null)
                val body = response.body?.string() ?: return@withContext Page(emptyList(), null)
                val listing = json.decodeFromString<RedditListing>(body)
                val items = listing.data.children.mapNotNull { it.data.toWallpaper(category) }
                Page(items, listing.data.after)
            }
        }.getOrDefault(Page(emptyList(), null))
    }

    private fun String.isImageExt(): Boolean =
        substringBefore('?').lowercase().let {
            it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") || it.endsWith(".webp")
        }

    // ---- JSON models ----

    @Serializable
    private data class RedditListing(val data: RedditData = RedditData())

    @Serializable
    private data class RedditData(
        val after: String? = null,
        val children: List<RedditChild> = emptyList()
    )

    @Serializable
    private data class RedditChild(val data: RedditPost = RedditPost())

    @Serializable
    private data class RedditPost(
        val id: String = "",
        val title: String = "",
        val author: String = "",
        @SerialName("over_18") val over18: Boolean = false,
        val url: String = "",
        val preview: RedditPreview? = null
    ) {
        fun toWallpaper(category: String): Wallpaper? {
            if (over18) return null
            val image = preview?.images?.firstOrNull() ?: return null
            val src = image.source
            // Portrait, phone-shaped only.
            if (src.width <= 0 || src.height <= src.width) return null

            val direct = url.takeIf { it.startsWith("https://i.redd.it") && it.isImageExt() }
            val full = direct ?: src.url.ifBlank { return null }
            val thumb = image.resolutions.minByOrNull { abs(it.width - 640) }?.url ?: full

            return Wallpaper(
                id = "rd_$id",
                title = title.ifBlank { "Wallpaper" }.take(70),
                author = "u/$author · Reddit",
                category = category,
                thumbUrl = thumb,
                fullUrl = full,
                resolution = "${src.width}x${src.height}"
            )
        }
    }

    @Serializable
    private data class RedditPreview(val images: List<RedditImage> = emptyList())

    @Serializable
    private data class RedditImage(
        val source: RedditImgSrc = RedditImgSrc(),
        val resolutions: List<RedditImgSrc> = emptyList()
    )

    @Serializable
    private data class RedditImgSrc(
        val url: String = "",
        val width: Int = 0,
        val height: Int = 0
    )
}
