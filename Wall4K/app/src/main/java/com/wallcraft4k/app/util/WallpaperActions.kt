package com.wallcraft4k.app.util

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.core.graphics.drawable.toBitmapOrNull
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Which surface to apply the wallpaper to. */
enum class WallpaperTarget { HOME, LOCK, BOTH }

object WallpaperActions {

    /** Decodes a wallpaper (remote URL or local file uri) to a software bitmap via Coil. */
    private suspend fun loadBitmap(context: Context, model: String): Bitmap? =
        withContext(Dispatchers.IO) {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(model)
                .allowHardware(false) // WallpaperManager/MediaStore need a real bitmap
                .build()
            val result = loader.execute(request)
            result.drawable?.toBitmapOrNull()
        }

    /** Sets [model] as the device wallpaper. Returns true on success. */
    suspend fun setAsWallpaper(
        context: Context,
        model: String,
        target: WallpaperTarget
    ): Boolean {
        val bitmap = loadBitmap(context, model) ?: return false
        val manager = WallpaperManager.getInstance(context)
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val which = when (target) {
                    WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                    WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                    WallpaperTarget.BOTH ->
                        WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                manager.setBitmap(bitmap, null, true, which)
            } else {
                manager.setBitmap(bitmap)
            }
            true
        }.getOrDefault(false)
    }

    /** Saves [model] into the public gallery under Pictures/Wall4K. Returns true on success. */
    suspend fun saveToGallery(
        context: Context,
        model: String,
        displayName: String
    ): Boolean = withContext(Dispatchers.IO) {
        val bitmap = loadBitmap(context, model) ?: return@withContext false
        val safeName = displayName.replace(Regex("[^A-Za-z0-9_-]"), "_").take(40)
        val fileName = "Wall4K_${safeName}_${System.currentTimeMillis()}.jpg"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Wall4K")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext false

        runCatching {
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            } ?: error("No output stream")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        }.getOrElse {
            resolver.delete(uri, null, null)
            false
        }
    }
}
