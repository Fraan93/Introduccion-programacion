package com.wallcraft4k.app

import android.app.Application
import com.wallcraft4k.app.data.WallpaperRepository

class Wall4KApp : Application() {
    val repository: WallpaperRepository by lazy { WallpaperRepository(this) }
}
