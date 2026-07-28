package com.wallcraft4k.app.data.model

import kotlinx.serialization.Serializable

/** Where a wallpaper comes from. */
@Serializable
enum class WallpaperSource { CATALOG, UPLOAD }

/**
 * A single wallpaper. [thumbUrl] and [fullUrl] can be remote http(s) URLs
 * (catalog) or local `file://` URIs (user uploads). Coil loads both.
 */
@Serializable
data class Wallpaper(
    val id: String,
    val title: String,
    val author: String,
    val category: String,
    val thumbUrl: String,
    val fullUrl: String,
    val resolution: String = "",
    val source: WallpaperSource = WallpaperSource.CATALOG
) {
    /** "8K" / "4K" / "2K" / "HD" badge derived from [resolution] (e.g. "3840x2160"). */
    val qualityBadge: String?
        get() {
            val w = resolution.substringBefore('x').trim().toIntOrNull() ?: return null
            val h = resolution.substringAfter('x').trim().toIntOrNull() ?: return null
            val maxSide = maxOf(w, h)
            return when {
                maxSide >= 7680 -> "8K"
                maxSide >= 3840 -> "4K"
                maxSide >= 2560 -> "2K"
                maxSide >= 1920 -> "FHD"
                else -> null
            }
        }
}
