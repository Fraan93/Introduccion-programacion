package com.fraan.kroma.data

/**
 * The generation engine chosen by the user.
 *
 *  - [FAST] is free and instant (pollinations.ai `turbo`).
 *  - [HD] is the PRO engine ("Nano Banana" = Gemini 2.5 Flash Image via the Kroma
 *    backend); when no backend is configured it falls back to a higher-quality
 *    pollinations model so the option always produces something.
 */
enum class AiEngine(val label: String, val premium: Boolean) {
    FAST("Rápido", premium = false),
    HD("HD ✦", premium = true)
}

/** Output shape. Phone (9:16) is the default for wallpapers. */
enum class AspectRatio(
    val label: String,
    val width: Int,
    val height: Int,
    val hdWidth: Int,
    val hdHeight: Int,
    val thumbW: Int,
    val thumbH: Int
) {
    PHONE("Móvil", 1080, 1920, 1440, 2560, 512, 910),
    SQUARE("Cuadrado", 1080, 1080, 1440, 1440, 620, 620)
}

/** A visual style appended to the user's prompt. */
data class AiStyle(val label: String, val suffix: String)

object AiStyles {
    val all: List<AiStyle> = listOf(
        AiStyle("Realista", "photorealistic, ultra detailed, sharp focus, cinematic lighting"),
        AiStyle("Anime", "anime style, makoto shinkai, studio ghibli, vibrant colors, detailed"),
        AiStyle("Cyberpunk", "cyberpunk, neon lights, futuristic city, rain reflections, cinematic"),
        AiStyle("Fantasía", "epic fantasy art, dramatic lighting, highly detailed, artstation"),
        AiStyle("Minimalista", "minimalist, clean composition, simple elegant shapes"),
        AiStyle("AMOLED", "pure black background, minimal glowing neon accents, amoled dark, oled"),
        AiStyle("3D", "3d render, octane render, soft studio lighting, glossy, high detail"),
        AiStyle("Óleo", "oil painting, textured brush strokes, classical fine art"),
        AiStyle("Acuarela", "watercolor painting, soft color washes, artistic, delicate"),
        AiStyle("Retro", "synthwave, retro 80s aesthetic, vaporwave, neon grid, film grain"),
        AiStyle("Ninguno", "")
    )

    val none: AiStyle = all.last()

    /** Builds the final prompt sent to the generator. */
    fun buildPrompt(userPrompt: String, style: AiStyle): String =
        listOf(
            userPrompt.ifBlank { "beautiful abstract wallpaper, colorful" },
            style.suffix,
            "phone wallpaper, highly detailed, high quality"
        ).filter { it.isNotBlank() }.joinToString(", ")
}
