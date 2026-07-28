package com.fraan.kroma

import android.app.Application
import com.fraan.kroma.data.PremiumRepository
import com.fraan.kroma.data.WallpaperRepository
import com.fraan.kroma.data.remote.FirebaseWallpaperSource

class KromaApp : Application() {

    val repository: WallpaperRepository by lazy {
        // Firebase auto-initialises when a valid google-services.json is present.
        // If it isn't, we fall back to fully local uploads.
        val remote = if (FirebaseWallpaperSource.isAvailable()) FirebaseWallpaperSource() else null
        WallpaperRepository(this, remote)
    }

    val premiumRepository: PremiumRepository by lazy { PremiumRepository(this) }
}
