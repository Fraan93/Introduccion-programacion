package com.fraan.kroma.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fraan.kroma.KromaApp
import com.fraan.kroma.data.PremiumPlan
import com.fraan.kroma.data.model.Wallpaper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A browsable category mapped to a wallhaven search query + minimum PORTRAIT resolution. */
data class Category(
    val label: String,
    val query: String,
    val atleast: String = "1080x1920"
)

class WallViewModel(app: Application) : AndroidViewModel(app) {

    private val wall4kApp = app as KromaApp
    private val repo = wall4kApp.repository
    private val premiumRepo = wall4kApp.premiumRepository

    val isRemote: Boolean = repo.isRemote

    // ---- Premium ----
    val isPremium: StateFlow<Boolean> =
        premiumRepo.isPremium.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val activePlan: StateFlow<PremiumPlan?> =
        premiumRepo.activePlan.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun activatePremium(plan: PremiumPlan, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            premiumRepo.activate(plan)
            onDone()
        }
    }

    val categories: List<Category> = listOf(
        Category("Popular", ""),
        Category("8K", "", atleast = "4320x7680"),      // 8K vertical por separado
        Category("4K", "", atleast = "2160x3840"),      // 4K vertical por separado
        Category("Anime", "anime"),
        Category("AMOLED", "amoled"),
        Category("Oscuro", "dark"),
        Category("Coches", "car"),
        Category("Ciudad", "city"),
        Category("Naturaleza", "nature"),
        Category("Espacio", "space"),
        Category("Abstracto", "abstract"),
        Category("Minimalista", "minimal"),
        Category("Animales", "animal"),
        Category("Videojuegos", "video game"),
        Category("Arte", "digital art"),
        Category("Neón", "neon"),
        Category("Montañas", "mountain"),
        Category("Flores", "flower"),
        Category("Paisaje", "landscape"),
        Category("Fantasía", "fantasy")
    )

    // ---- Browse feed with infinite pagination ----
    private val _browse = MutableStateFlow<List<Wallpaper>>(emptyList())
    val browse: StateFlow<List<Wallpaper>> = _browse.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _selectedCategory = MutableStateFlow(categories.first())
    val selectedCategory: StateFlow<Category> = _selectedCategory.asStateFlow()

    private var currentQuery = ""
    private var currentAtleast = "1080x1920"
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
        startQuery(category.query, category.atleast)
    }

    fun search(text: String) {
        _selectedCategory.value = Category(text.ifBlank { "Búsqueda" }, text)
        startQuery(text, "1080x1920")
    }

    private fun startQuery(query: String, atleast: String) {
        currentQuery = query
        currentAtleast = atleast
        page = 1
        endReached = false
        _browse.value = emptyList()
        loadMore()
    }

    /** Other wallpapers of the same category as [wallpaper], for the "more like this" strip. */
    fun relatedTo(wallpaper: Wallpaper): List<Wallpaper> =
        _browse.value.filter { it.id != wallpaper.id && it.category == wallpaper.category }.take(15)

    fun loadMore() {
        if (_loading.value || endReached) return
        _loading.value = true
        viewModelScope.launch {
            val items = runCatching { repo.browse(currentQuery, page, currentAtleast) }.getOrDefault(emptyList())
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
