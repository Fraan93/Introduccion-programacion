package com.fraan.kroma

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.fraan.kroma.data.PremiumRepository
import com.fraan.kroma.data.SettingsRepository
import com.fraan.kroma.data.WallpaperRepository
import com.fraan.kroma.data.remote.FirebaseWallpaperSource

class KromaApp : Application(), ImageLoaderFactory {

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

    /**
     * Global Coil loader with generous memory + disk caches so wallpapers already
     * seen (including AI ones, which are expensive to generate) load instantly.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            .build()

    val repository: WallpaperRepository by lazy {
        // Firebase auto-initialises when a valid google-services.json is present.
        // If it isn't, we fall back to fully local uploads.
        val remote = if (FirebaseWallpaperSource.isAvailable()) FirebaseWallpaperSource() else null
        WallpaperRepository(this, remote)
    }

    val premiumRepository: PremiumRepository by lazy { PremiumRepository(this) }
}
