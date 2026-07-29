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
 * A browsable category. Wallhaven is the reliable source ([query] + [whCategories]
 * + [atleast] + [whSorting]); each category uses a distinct query so they differ.
 * When [aiOnly] is true the category is served by the AI generator instead
 * ([aiPrompts]) — reserved for the "IA" category and search.
 */
data class Category(
    val label: String,
    val query: String,
    val atleast: String = "1080x1920",
    val whCategories: String = "111",
    val whSorting: String? = null,
    val aiOnly: Boolean = false,
    val aiPrompts: List<String> = emptyList(),
    val aiWidth: Int = 1080,
    val aiHeight: Int = 1920
)

/** Prompt pools for the AI generator (pollinations.ai). */
private object AiPrompts {
    val art = listOf(
        "cosmic nebula galaxy with bright stars",
        "a lush green planet seen from space, blue atmosphere",
        "fantasy mountain landscape at sunset, epic clouds",
        "aurora borealis over snowy mountains at night",
        "cyberpunk neon city street at night, rain reflections",
        "deep space scene with a ringed planet and moons",
        "bioluminescent forest at night, glowing plants",
        "japanese torii gate, cherry blossoms, night, lanterns",
        "underwater coral reef with sun rays, vibrant colors",
        "abstract flowing liquid gradient, purple and blue",
        "volcanic dark landscape with glowing lava rivers",
        "surreal floating islands with waterfalls, dreamy sky",
        "minimalist 3d geometric shapes, soft studio lighting",
        "majestic waterfall in a tropical canyon, mist",
        "galaxy reflected in a calm mountain lake at night"
    )
    val anime = listOf(
        "anime scenery, makoto shinkai style, city at dusk, detailed",
        "anime landscape, cherry blossoms and mountains, studio ghibli style",
        "anime night sky with shooting stars over a quiet town",
        "anime girl with umbrella in neon rainy street, cinematic",
        "fantasy anime castle in the clouds, golden light"
    )
    val amoled = listOf(
        "pure black background with a single glowing neon wave, amoled",
        "pure black background minimal glowing geometric line art",
        "black background with a small vibrant galaxy, amoled minimal",
        "pure black wallpaper, subtle blue glowing particles"
    )
}

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

    // Wallhaven (reliable) powers every browsing category, each with a DISTINCT
    // query so they truly differ. "IA" and search use the AI generator (fast turbo).
    val categories: List<Category> = listOf(
        Category("Popular", ""),
        // 4K/8K: the highest-resolution portrait wallpapers Wallhaven has (real,
        // fast). Badge shows the true resolution.
        Category("4K", "", atleast = "1440x2560", whSorting = "toplist"),
        Category("8K", "", atleast = "2160x3840", whSorting = "favorites"),
        // IA: fully AI-generated art (opt-in; each image is created on the fly).
        Category("IA", "", aiOnly = true, aiPrompts = AiPrompts.art),
        Category("Anime", "anime", whCategories = "010"),
        Category("AMOLED", "amoled", whSorting = "toplist"),
        Category("Oscuro", "dark"),
        Category("Coches", "car"),
        Category("Espacio", "outer space galaxy"),
        Category("Naturaleza", "nature landscape"),
        Category("Ciudad", "city night"),
        Category("Abstracto", "abstract"),
        Category("Minimalista", "minimal"),
        Category("Neón", "neon"),
        Category("Anime chicas", "anime girl", whCategories = "010"),
        Category("Videojuegos", "video game"),
        Category("Code/Tech", "technology code"),
        Category("Fantasía", "fantasy art")
    )

    // ---- Browse feed with infinite pagination ----
    private val _browse = MutableStateFlow<List<Wallpaper>>(emptyList())
    val browse: StateFlow<List<Wallpaper>> = _browse.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _selectedCategory = MutableStateFlow(categories.first())
    val selectedCategory: StateFlow<Category> = _selectedCategory.asStateFlow()

    // The feed currently being shown.
    private var current: Category = categories.first()
    private var page = 1
    private var endReached = false

    /**
     * Random page offset for the broad Popular feed so wallpapers rotate between
     * visits. Not applied to searches, sorted feeds, or AI feeds.
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
        startFeed(category)
    }

    /** Search = generate wallpapers with the AI from whatever text the user types. */
    fun search(text: String) {
        val cat = Category(
            label = text.ifBlank { "Búsqueda" },
            query = text,
            aiOnly = true,
            aiPrompts = if (text.isBlank()) AiPrompts.art else listOf(text)
        )
        _selectedCategory.value = cat
        startFeed(cat)
    }

    private fun startFeed(category: Category) {
        current = category
        // Only the broad Popular feed rotates its starting page.
        pageOffset = if (!category.aiOnly && category.query.isBlank() &&
            category.atleast == "1080x1920" && category.whSorting == null
        ) {
            Random.nextInt(0, 8)
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
                if (wallpaper.id.startsWith("ai_")) {
                    // AI items: generate more art in the same style.
                    repo.browseAi(
                        current.aiPrompts.ifEmpty { AiPrompts.art },
                        Random.nextInt(0, 60), current.aiWidth, current.aiHeight, current.label
                    )
                } else {
                    // Wallhaven items: real similar wallpapers by the image's tags.
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
            if (current.aiOnly) {
                val prompts = current.aiPrompts.ifEmpty { AiPrompts.art }
                val items = repo.browseAi(prompts, page, current.aiWidth, current.aiHeight, current.label)
                appendItems(items)
                page++
                if (page > 40) endReached = true // safety cap; AI is otherwise endless
            } else {
                val items = runCatching {
                    repo.browse(
                        current.query, page + pageOffset, current.atleast,
                        current.whCategories, current.whSorting
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
