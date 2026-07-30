package com.fraan.kroma.data.remote

import com.fraan.kroma.data.model.Wallpaper
import java.net.URLEncoder
import kotlin.math.abs

/**
 * Free AI wallpaper generator via pollinations.ai (no API key). Builds image URLs
 * that render a wallpaper on demand.
 *
 * The GRID thumbnail uses a small size so the feed fills in fast; the full-screen
 * image uses the target resolution and is generated only when a wallpaper is opened.
 * Same model + seed + prompt ⇒ thumbnail and full image match, and Coil caches both.
 */
object PollinationsApi {

    private const val BASE = "https://image.pollinations.ai/prompt/"

    /**
     * [count] variations of [styledPrompt]. [page] shifts the seed so "generate more"
     * always yields new images. Deterministic per (prompt, page, index).
     */
    fun generate(
        styledPrompt: String,
        fullW: Int,
        fullH: Int,
        thumbW: Int,
        thumbH: Int,
        model: String,
        page: Int,
        count: Int,
        author: String,
        category: String,
        titlePrompt: String
    ): List<Wallpaper> {
        val enc = URLEncoder.encode(styledPrompt, "UTF-8")
        val promptSeed = abs(styledPrompt.hashCode()) % 100_000
        return (0 until count).map { i ->
            val seed = promptSeed + page * count + i
            val common = "nologo=true&model=$model&seed=$seed"
            Wallpaper(
                id = "ai_${model}_$seed",
                title = titlePrompt.ifBlank { category.ifBlank { "IA" } },
                author = author,
                category = category,
                thumbUrl = "$BASE$enc?width=$thumbW&height=$thumbH&$common",
                fullUrl = "$BASE$enc?width=$fullW&height=$fullH&$common",
                resolution = "${fullW}x$fullH"
            )
        }
    }
}
