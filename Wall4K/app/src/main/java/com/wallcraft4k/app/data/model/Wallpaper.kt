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
    val source: WallpaperSource = WallpaperSource.CATALOG
)
