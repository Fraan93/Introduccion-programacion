package com.wallcraft4k.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wallcraft4k.app.Wall4KApp
import com.wallcraft4k.app.data.model.Wallpaper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WallViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as Wall4KApp).repository

    val wallpapers: StateFlow<List<Wallpaper>> =
        repo.allWallpapers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<Set<String>> =
        repo.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val categories: StateFlow<List<String>> =
        repo.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteWallpapers: StateFlow<List<Wallpaper>> =
        repo.feed()
            .map { (list, favs) -> list.filter { it.id in favs } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun wallpaperById(id: String): Wallpaper? = wallpapers.value.firstOrNull { it.id == id }

    fun isFavorite(id: String): Boolean = id in favorites.value

    fun toggleFavorite(id: String) {
        viewModelScope.launch { repo.toggleFavorite(id) }
    }

    fun addUpload(
        source: Uri,
        title: String,
        author: String,
        category: String,
        onDone: (Wallpaper) -> Unit
    ) {
        viewModelScope.launch {
            runCatching { repo.addUpload(source, title, author, category) }
                .onSuccess(onDone)
        }
    }

    fun deleteUpload(id: String) {
        viewModelScope.launch { repo.deleteUpload(id) }
    }
}
