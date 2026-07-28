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
    val queryPool: List<String> = emptyList()
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

    val categories: List<Category> = listOf(
        Category("Popular", ""),
        // 8K: sin la categoría "personas" (evita la avalancha de retratos de
        // estudio, lo único que abunda en ultra resolución) y ordenada por
        // favoritos de todos los tiempos.
        Category("8K", "", atleast = "4320x7680", whCategories = "110", whSorting = "favorites"),
        // 4K: vitrina curada de arte espectacular (planetas, espacio, fantasía,
        // arte digital...) — solo catálogo general, sin fotos de stock, ordenada
        // por favoritos históricos. Cada visita rota el tema.
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
        Category("Anime", "", whCategories = "010", useStock = false),      // catálogo anime real
        Category("Waifus", "anime girls", whCategories = "010", useStock = false),
        Category("AMOLED", "amoled black", useStock = false),
        Category("Oscuro", "dark", useStock = false),
        Category("Coches", "car"),
        Category("Ciudad", "city"),
        Category("Naturaleza", "nature"),
        Category("Espacio", "space"),
        Category("Abstracto", "abstract"),
        Category("Minimalista", "minimal"),
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

    private var currentQuery = ""
    private var currentAtleast = "1080x1920"
    private var currentWhCategories = "111"
    private var currentUseStock = true
    private var currentWhSorting: String? = null
    private var page = 1
    private var endReached = false

    /**
     * Random page offset for the broad feeds (Popular/4K…): each visit starts at a
     * different point of the top list so the photos rotate instead of always being
     * the same. Not applied to specific searches, where order relevance matters.
     */
    private var pageOffset = Random.nextInt(0, 10)

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
        // Showcase categories rotate among curated themes on every visit.
        val query = if (category.queryPool.isNotEmpty()) category.queryPool.random() else category.query
        startQuery(query, category.atleast, category.whCategories, category.useStock, category.whSorting)
    }

    fun search(text: String) {
        _selectedCategory.value = Category(text.ifBlank { "Búsqueda" }, text)
        startQuery(text, "1080x1920", "111", true, null)
    }

    private fun startQuery(
        query: String,
        atleast: String,
        whCategories: String,
        useStock: Boolean,
        whSorting: String?
    ) {
        currentQuery = query
        currentAtleast = atleast
        currentWhCategories = whCategories
        currentUseStock = useStock
        currentWhSorting = whSorting
        // Only the broad Popular feed rotates its starting page; sparse feeds like
        // 8K have few pages and a random offset could leave them empty.
        pageOffset = if (query.isBlank() && atleast == "1080x1920" && whSorting == null) {
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
            val results = runCatching { repo.findSimilar(wallpaper) }.getOrDefault(emptyList())
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
            val items = runCatching {
                repo.browse(
                    currentQuery,
                    page + pageOffset,
                    currentAtleast,
                    currentWhCategories,
                    currentUseStock,
                    currentWhSorting
                )
            }.getOrDefault(emptyList())
            if (items.isEmpty()) {
                endReached = true
            } else {
                items.forEach { cache[it.id] = it }
                val existingIds = _browse.value.mapTo(HashSet()) { it.id }
                _browse.value = _browse.value + items.filter { it.id !in existingIds }
                page++
            }
            _loading.value = false
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
