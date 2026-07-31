package com.fraan.kroma.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fraan.kroma.KromaApp
import com.fraan.kroma.data.PremiumPlan
import com.fraan.kroma.data.ThemeMode
import com.fraan.kroma.data.model.Wallpaper
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A browsable category. Wallhaven powers every feed; each category uses a distinct
 * [query] (+ [whCategories]/[whSorting]) so they genuinely differ.
 */
data class Category(
    val label: String,
    val query: String,
    val whCategories: String = "111",
    val whSorting: String? = null
)

/**
 * Minimum-resolution filter. Real portrait sizes so "4K" and "FHD" actually mean
 * something; the card badge shows each wallpaper's true quality.
 */
enum class ResFilter(val label: String, val atleast: String) {
    ALL("Todos", "720x1280"),
    FHD("FHD", "1080x1920"),
    QHD("2K", "1440x2560"),
    UHD("4K", "2160x3840")
}

class WallViewModel(app: Application) : AndroidViewModel(app) {

    private val kromaApp = app as KromaApp
    private val repo = kromaApp.repository
    private val premiumRepo = kromaApp.premiumRepository
    private val settingsRepo = kromaApp.settingsRepository

    /** Every wallpaper seen by the UI, by id, so detail navigation never loses one. */
    private val cache = HashMap<String, Wallpaper>()

    // ---- Settings ----
    val theme: StateFlow<ThemeMode> =
        settingsRepo.theme.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsRepo.setTheme(mode) }
    }

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

    // ---- Catalog ----
    val categories: List<Category> = listOf(
        Category("Popular", ""),
        Category("Naturaleza", "nature landscape"),
        Category("Espacio", "outer space galaxy"),
        Category("Anime", "anime", whCategories = "010"),
        Category("Coches", "sports car"),
        Category("Ciudad", "city night"),
        Category("Montañas", "mountains"),
        Category("Playa", "beach ocean"),
        Category("Abstracto", "abstract"),
        Category("Oscuro", "dark"),
        Category("AMOLED", "amoled black", whSorting = "toplist"),
        Category("Minimalista", "minimal"),
        Category("Neón", "neon"),
        Category("Flores", "flowers"),
        Category("Animales", "animals"),
        Category("Videojuegos", "video game"),
        Category("Fantasía", "fantasy art")
    )

    val resFilters: List<ResFilter> = ResFilter.entries

    private val _browse = MutableStateFlow<List<Wallpaper>>(emptyList())
    val browse: StateFlow<List<Wallpaper>> = _browse.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _selectedCategory = MutableStateFlow(categories.first())
    val selectedCategory: StateFlow<Category> = _selectedCategory.asStateFlow()

    private val _selectedRes = MutableStateFlow(ResFilter.ALL)
    val selectedRes: StateFlow<ResFilter> = _selectedRes.asStateFlow()

    private var current: Category = categories.first()
    private var currentRes: ResFilter = ResFilter.ALL
    private var page = 1
    private var endReached = false
    private var pageOffset = 0

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
        startFeed(categories.first(), ResFilter.ALL)
    }

    fun selectCategory(category: Category) {
        _selectedCategory.value = category
        startFeed(category, currentRes)
    }

    fun selectRes(res: ResFilter) {
        _selectedRes.value = res
        startFeed(current, res)
    }

    /** Search the real catalog. */
    fun search(text: String) {
        if (text.isBlank()) {
            selectCategory(categories.first())
            return
        }
        val cat = Category(label = text, query = text)
        _selectedCategory.value = cat
        startFeed(cat, currentRes)
    }

    private fun startFeed(category: Category, res: ResFilter) {
        current = category
        currentRes = res
        // Only the broad Popular feed (no query, no sorting, no res filter) rotates.
        pageOffset = if (category.query.isBlank() && category.whSorting == null && res == ResFilter.ALL) {
            Random.nextInt(0, 8)
        } else 0
        page = 1
        endReached = false
        _browse.value = emptyList()
        loadMore()
    }

    fun loadMore() {
        if (_loading.value || endReached) return
        _loading.value = true
        viewModelScope.launch {
            val items = runCatching {
                repo.browse(
                    current.query, page + pageOffset, currentRes.atleast,
                    current.whCategories, current.whSorting
                )
            }.getOrDefault(emptyList())
            if (items.isEmpty()) endReached = true else { appendItems(items); page++ }
            _loading.value = false
        }
    }

    private fun appendItems(items: List<Wallpaper>) {
        items.forEach { cache[it.id] = it }
        val existing = _browse.value.mapTo(HashSet()) { it.id }
        _browse.value = _browse.value + items.filter { it.id !in existing }
    }

    // ---- Similar wallpapers (by real tags) ----
    private val _related = MutableStateFlow<List<Wallpaper>>(emptyList())
    val related: StateFlow<List<Wallpaper>> = _related.asStateFlow()
    private var relatedForId: String? = null

    fun loadRelated(wallpaper: Wallpaper) {
        cache[wallpaper.id] = wallpaper
        if (relatedForId == wallpaper.id) return
        relatedForId = wallpaper.id
        _related.value = emptyList()
        viewModelScope.launch {
            val results = runCatching { repo.findSimilar(wallpaper) }
                .getOrDefault(emptyList())
                .filter { it.id != wallpaper.id }
                .take(14)
            results.forEach { cache[it.id] = it }
            _related.value = results
        }
    }

    fun wallpaperById(id: String): Wallpaper? =
        cache[id]
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
