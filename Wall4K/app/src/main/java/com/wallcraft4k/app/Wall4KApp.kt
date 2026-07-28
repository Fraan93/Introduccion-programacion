package com.wallcraft4k.app

import android.app.Application
import com.wallcraft4k.app.data.PremiumRepository
import com.wallcraft4k.app.data.WallpaperRepository
import com.wallcraft4k.app.data.remote.FirebaseWallpaperSource

class Wall4KApp : Application() {

    val repository: WallpaperRepository by lazy {
        // Firebase auto-initialises when a valid google-services.json is present.
        // If it isn't, we fall back to fully local uploads.
        val remote = if (FirebaseWallpaperSource.isAvailable()) FirebaseWallpaperSource() else null
        WallpaperRepository(this, remote)
    }

    val premiumRepository: PremiumRepository by lazy { PremiumRepository(this) }
}
