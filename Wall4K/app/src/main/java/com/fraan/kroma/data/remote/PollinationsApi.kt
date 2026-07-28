package com.fraan.kroma.data.remote

import com.fraan.kroma.data.model.Wallpaper
import java.net.URLEncoder

/**
 * Free AI wallpaper generator via pollinations.ai (no API key). Builds image URLs
 * that render a wallpaper on demand at the EXACT resolution requested — so the 4K
 * and 8K categories contain genuinely 4K/8K-sized, gorgeous, unique art instead of
 * random stock photos. Deterministic per (prompt, seed, size): the thumbnail and
 * full image match, and caching makes repeat loads instant.
 */
object PollinationsApi {

    private const val BASE = "https://image.pollinations.ai/prompt/"
    private const val PER_PAGE = 12

    /**
     * Produces a page of AI wallpapers. Infinite by design: each [page] uses fresh
     * seeds, so scrolling keeps yielding new art.
     */
    fun generate(
        prompts: List<String>,
        page: Int,
        width: Int,
        height: Int,
        category: String
    ): List<Wallpaper> {
        if (prompts.isEmpty()) return emptyList()
        return (0 until PER_PAGE).map { i ->
            val index = page * PER_PAGE + i
            val prompt = prompts[index % prompts.size]
            val seed = 1000 + index
            val enc = URLEncoder.encode("$prompt, phone wallpaper, ultra detailed, 4k", "UTF-8")
            val common = "nologo=true&model=flux&seed=$seed"
            Wallpaper(
                id = "ai_${seed}",
                title = prompt.take(40),
                author = "Kroma AI",
                category = category,
                thumbUrl = "$BASE$enc?width=480&height=854&$common",
                fullUrl = "$BASE$enc?width=$width&height=$height&$common",
                resolution = "${width}x$height"
            )
        }
    }
}
