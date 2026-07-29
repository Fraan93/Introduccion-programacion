package com.fraan.kroma.data.remote

import com.fraan.kroma.data.model.Wallpaper
import java.net.URLEncoder

/**
 * Free AI wallpaper generator via pollinations.ai (no API key). Builds image URLs
 * that render a wallpaper on demand.
 *
 * Speed matters: the GRID thumbnails use the fast `turbo` model at a small size so
 * the feed fills in quickly, while the full-screen image is generated at the
 * category's target resolution (only when a wallpaper is opened). Same model + seed
 * + prompt ⇒ the thumbnail and the full image match; caching makes repeats instant.
 */
object PollinationsApi {

    private const val BASE = "https://image.pollinations.ai/prompt/"
    private const val PER_PAGE = 10
    private const val MODEL = "turbo" // much faster than "flux"; good enough for wallpapers

    // Small thumbnail = fast generation for the grid.
    private const val THUMB_W = 360
    private const val THUMB_H = 640

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
            val enc = URLEncoder.encode("$prompt, phone wallpaper, ultra detailed", "UTF-8")
            val common = "nologo=true&model=$MODEL&seed=$seed"
            Wallpaper(
                id = "ai_${seed}",
                title = prompt.take(40),
                author = "Kroma AI",
                category = category,
                thumbUrl = "$BASE$enc?width=$THUMB_W&height=$THUMB_H&$common",
                fullUrl = "$BASE$enc?width=$width&height=$height&$common",
                resolution = "${width}x$height"
            )
        }
    }
}
