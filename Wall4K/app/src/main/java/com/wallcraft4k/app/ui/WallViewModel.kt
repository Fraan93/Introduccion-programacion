package com.wallcraft4k.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wallcraft4k.app.Wall4KApp
import com.wallcraft4k.app.data.model.Wallpaper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A browsable category mapped to a wallhaven search query. */
data class Category(val label: String, val query: String)

class WallViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as Wall4KApp).repository

    val isRemote: Boolean = repo.isRemote

    val categories: List<Category> = listOf(
        Category("Popular", ""),
        Category("4K / 8K", "4k"),
        Category("Anime", "anime"),
        Category("Naturaleza", "nature landscape"),
        Category("Coches", "car"),
        Category("Ciudad", "city"),
        Category("Abstracto", "abstract"),
        Category("Espacio", "space galaxy"),
        Category("Oscuro / AMOLED", "dark amoled"),
        Category("Minimalista", "minimal"),
        Category("Animales", "animal"),
        Category("Videojuegos", "video game"),
        Category("Arte", "digital art"),
        Category("Neón", "neon"),
        Category("Montañas", "mountains"),
        Category("Flores", "flowers")
    )

    // ---- Browse feed with infinite pagination ----
    private val _browse = MutableStateFlow<List<Wallpaper>>(emptyList())
    val browse: StateFlow<List<Wallpaper>> = _browse.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _selectedCategory = MutableStateFlow(categories.first())
    val selectedCategory: StateFlow<Category> = _selectedCategory.asStateFlow()

    private var currentQuery = ""
    private var page = 1
    private var endReached = false

    // ---- Favourites & uploads ----
    val favoriteWallpapers: StateFlow<List<Wallpaper>> =
        repo.favoriteWallpapers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<Set<String>> =
        repo.favoriteIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val uploads: StateFlow<List<Wallpaper>> =
        repo.uploads.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading.asStateFlow()

    init {
        loadMore()
    }

    fun selectCategory(category: Category) {
        _selectedCategory.value = category
        startQuery(category.query)
    }

    fun search(text: String) {
        _selectedCategory.value = Category(text.ifBlank { "Búsqueda" }, text)
        startQuery(text)
    }

    private fun startQuery(query: String) {
        currentQuery = query
        page = 1
        endReached = false
        _browse.value = emptyList()
        loadMore()
    }

    fun loadMore() {
        if (_loading.value || endReached) return
        _loading.value = true
        viewModelScope.launch {
            val items = runCatching { repo.browse(currentQuery, page) }.getOrDefault(emptyList())
            if (items.isEmpty()) {
                endReached = true
            } else {
                val existingIds = _browse.value.mapTo(HashSet()) { it.id }
                _browse.value = _browse.value + items.filter { it.id !in existingIds }
                page++
            }
            _loading.value = false
        }
    }

    fun wallpaperById(id: String): Wallpaper? =
        _browse.value.firstOrNull { it.id == id }
            ?: favoriteWallpapers.value.firstOrNull { it.id == id }
            ?: uploads.value.firstOrNull { it.id == id }

    fun toggleFavorite(wallpaper: Wallpaper) {
        viewModelScope.launch { repo.toggleFavorite(wallpaper) }
    }

    fun addUpload(
        source: Uri,
        title: String,
        author: String,
        category: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _uploading.value = true
            val ok = runCatching { repo.addUpload(source, title, author, category) }.isSuccess
            _uploading.value = false
            onResult(ok)
        }
    }

    fun deleteUpload(id: String) {
        viewModelScope.launch { repo.deleteUpload(id) }
    }
}
