package com.fraan.kroma.util

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
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/** Which surface to apply the wallpaper to. */
enum class WallpaperTarget { HOME, LOCK, BOTH }

object WallpaperActions {

    /**
     * Decodes a wallpaper (remote URL or local file uri) to a software bitmap via Coil.
     * When [fitToScreen] is true, the image is decoded at (about) the device screen
     * size instead of full resolution — an 8K bitmap would otherwise use ~130 MB of
     * RAM and can crash the app; the screen can't show more pixels anyway.
     */
    private suspend fun loadBitmap(
        context: Context,
        model: String,
        fitToScreen: Boolean = false
    ): Bitmap? = withContext(Dispatchers.IO) {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(model)
            .allowHardware(false) // WallpaperManager/MediaStore need a real bitmap
            .apply {
                if (fitToScreen) {
                    val dm = context.resources.displayMetrics
                    size(dm.widthPixels, dm.heightPixels)
                    scale(coil.size.Scale.FILL) // cover the screen, keep aspect ratio
                }
            }
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
        // Screen-sized decode: applying doesn't need more pixels than the display has.
        val bitmap = loadBitmap(context, model, fitToScreen = true) ?: return false
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

    /**
     * Saves [model] into the public gallery under Pictures/Kroma.
     *
     * The ORIGINAL file bytes are streamed straight to MediaStore — no decode, no
     * re-compression. This keeps the full 4K/8K quality intact and uses almost no
     * RAM regardless of image size. Returns true on success.
     */
    suspend fun saveToGallery(
        context: Context,
        model: String,
        displayName: String
    ): Boolean = withContext(Dispatchers.IO) {
        val isPng = model.substringBefore('?').endsWith(".png", ignoreCase = true)
        val ext = if (isPng) "png" else "jpg"
        val mime = if (isPng) "image/png" else "image/jpeg"
        val safeName = displayName.replace(Regex("[^A-Za-z0-9_-]"), "_").take(40)
        val fileName = "Kroma_${safeName}_${System.currentTimeMillis()}.$ext"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Kroma")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext false

        runCatching {
            resolver.openOutputStream(uri)?.use { out ->
                openSourceStream(context, model).use { input -> input.copyTo(out) }
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

    /** Opens a raw byte stream for [model]: http(s) URL, file:// or content:// uri. */
    private fun openSourceStream(context: Context, model: String): InputStream {
        return if (model.startsWith("http://") || model.startsWith("https://")) {
            val connection = URL(model).openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("User-Agent", "Kroma/1.0")
            connection.inputStream
        } else {
            context.contentResolver.openInputStream(android.net.Uri.parse(model))
                ?: error("No se pudo abrir la imagen")
        }
    }
}
