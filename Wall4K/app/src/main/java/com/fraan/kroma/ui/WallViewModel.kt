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
 * A browsable category. [whCategories] are wallhaven category bits ("010" = anime
 * only). [useStock] controls whether Pexels/Unsplash join in — stock-photo sites
 * pollute categories like Anime or AMOLED, so those are Wallhaven-only.
 */
data class Category(
    val label: String,
    val query: String,
    val atleast: String = "1080x1920",
    val whCategories: String = "111",
    val useStock: Boolean = true,
    val whSorting: String? = null,
    /** Curated showcase themes: one is picked at random on each visit. */
    val queryPool: List<String> = emptyList(),
    /** Reddit subreddits ('+'-joined) — primary source for the Wallcraft look. */
    val subreddits: String = "",
    val redditSort: String = "top",
    val redditTime: String = "all"
)

class WallViewModel(app: Application) : AndroidViewModel(app) {

    private val kromaApp = app as KromaApp
    private val repo = kromaApp.repository
    private val premiumRepo = kromaApp.premiumRepository
    private val settingsRepo = kromaApp.settingsRepository

    /**
     * Every wallpaper that has passed through the UI, by id. Detail screens
     * resolve wallpapers from here, so an item never "disappears" mid-navigation
     * (e.g. while the similar-wallpapers list is being replaced).
     */
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

    // Categories with `subreddits` pull curated phone wallpapers from Reddit (the
    // Wallcraft look); `query`/`whCategories` act as a Wallhaven fallback if Reddit
    // is unreachable. Photography themes stay on Wallhaven + stock.
    val categories: List<Category> = listOf(
        Category(
            "Popular", "",
            subreddits = "MobileWallpaper+iphonewallpapers+WQHD_Wallpaper",
            redditSort = "hot"
        ),
        // 8K: real ultra-res from Wallhaven, best of all time, no studio portraits.
        Category("8K", "", atleast = "4320x7680", whCategories = "110", whSorting = "favorites"),
        // 4K: curated concept art (planets, space, fantasy…) rotating each visit.
        Category(
            "4K", "",
            atleast = "2160x3840",
            whCategories = "100",
            useStock = false,
            whSorting = "favorites",
            queryPool = listOf(
                "space", "planet", "galaxy", "nebula", "earth",
                "fantasy landscape", "digital art", "mountains",
                "aurora", "cyberpunk city", "abstract 3d", "underwater"
            )
        ),
        Category(
            "Anime", "anime",
            whCategories = "010", useStock = false,
            subreddits = "Animewallpaper+AnimeWallpaper+MobileWallpaper"
        ),
        Category(
            "AMOLED", "amoled black", useStock = false,
            subreddits = "Amoledbackgrounds"
        ),
        Category(
            "Minimalista", "minimal", useStock = false,
            subreddits = "MinimalWallpaper+minimalist"
        ),
        Category(
            "Coches", "car",
            subreddits = "carwallpapers+CarsWallpapers"
        ),
        Category(
            "Espacio", "space",
            subreddits = "spaceporn+SpaceWallpapers"
        ),
        Category("Oscuro", "dark", useStock = false),
        Category("Ciudad", "city"),
        Category("Naturaleza", "nature"),
        Category("Abstracto", "abstract"),
        Category("Animales", "animal"),
        Category("Videojuegos", "video game", useStock = false),
        Category("Arte", "digital art", useStock = false),
        Category("Neón", "neon"),
        Category("Montañas", "mountain"),
        Category("Flores", "flower"),
        Category("Paisaje", "landscape"),
        Category("Fantasía", "fantasy", useStock = false)
    )

    // ---- Browse feed with infinite pagination ----
    private val _browse = MutableStateFlow<List<Wallpaper>>(emptyList())
    val browse: StateFlow<List<Wallpaper>> = _browse.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _selectedCategory = MutableStateFlow(categories.first())
    val selectedCategory: StateFlow<Category> = _selectedCategory.asStateFlow()

    // The feed currently being shown (holds Reddit + Wallhaven parameters).
    private var current: Category = categories.first()
    private var page = 1
    private var endReached = false

    // Reddit pagination cursor + whether Reddit is the active source for `current`.
    private var redditAfter: String? = null
    private var redditActive = false

    /**
     * Random page offset for broad Wallhaven feeds so photos rotate between visits.
     * Not applied to searches, sorted feeds, or Reddit feeds.
     */
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
        startFeed(categories.first())
    }

    fun selectCategory(category: Category) {
        _selectedCategory.value = category
        // Showcase categories rotate among curated themes on every visit.
        val query = if (category.queryPool.isNotEmpty()) category.queryPool.random() else category.query
        startFeed(category.copy(query = query))
    }

    fun search(text: String) {
        val cat = Category(text.ifBlank { "Búsqueda" }, text)
        _selectedCategory.value = cat
        startFeed(cat)
    }

    private fun startFeed(category: Category) {
        current = category
        redditActive = category.subreddits.isNotBlank()
        redditAfter = null
        // Only broad Wallhaven feeds rotate their starting page.
        pageOffset = if (!redditActive && category.query.isBlank() &&
            category.atleast == "1080x1920" && category.whSorting == null
        ) {
            Random.nextInt(0, 10)
        } else {
            0
        }
        page = 1
        endReached = false
        _browse.value = emptyList()
        loadMore()
    }

    // ---- Genuinely similar wallpapers (by real image tags) ----
    private val _related = MutableStateFlow<List<Wallpaper>>(emptyList())
    val related: StateFlow<List<Wallpaper>> = _related.asStateFlow()
    private var relatedForId: String? = null

    fun loadRelated(wallpaper: Wallpaper) {
        cache[wallpaper.id] = wallpaper
        if (relatedForId == wallpaper.id) return
        relatedForId = wallpaper.id
        _related.value = emptyList()
        viewModelScope.launch {
            val results = runCatching {
                // Reddit items: pull more from the SAME subreddits (same theme).
                if (wallpaper.id.startsWith("rd_") && current.subreddits.isNotBlank()) {
                    repo.browseReddit(
                        current.subreddits, "top", "all", null, current.atleast, current.label
                    ).items
                } else {
                    repo.findSimilar(wallpaper)
                }
            }.getOrDefault(emptyList())
                .filter { it.id != wallpaper.id }
                .take(14)
            results.forEach { cache[it.id] = it }
            _related.value = results
        }
    }

    fun loadMore() {
        if (_loading.value || endReached) return
        _loading.value = true
        viewModelScope.launch {
            if (redditActive) {
                val res = runCatching {
                    repo.browseReddit(
                        current.subreddits, current.redditSort, current.redditTime,
                        redditAfter, current.atleast, current.label
                    )
                }.getOrNull()
                // Reddit unreachable / empty on the first page -> fall back to Wallhaven.
                if (res == null || (redditAfter == null && res.items.isEmpty())) {
                    redditActive = false
                    _loading.value = false
                    loadMore()
                    return@launch
                }
                redditAfter = res.nextAfter
                if (res.nextAfter == null) endReached = true
                appendItems(res.items)
            } else {
                val items = runCatching {
                    repo.browse(
                        current.query, page + pageOffset, current.atleast,
                        current.whCategories, current.useStock, current.whSorting
                    )
                }.getOrDefault(emptyList())
                if (items.isEmpty()) endReached = true else { appendItems(items); page++ }
            }
            _loading.value = false
        }
    }

    private fun appendItems(items: List<Wallpaper>) {
        items.forEach { cache[it.id] = it }
        val existing = _browse.value.mapTo(HashSet()) { it.id }
        _browse.value = _browse.value + items.filter { it.id !in existing }
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
