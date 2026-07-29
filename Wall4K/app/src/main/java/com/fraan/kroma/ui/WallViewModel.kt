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
    val redditTime: String = "all",
    /** AI-generation prompts. Used as the category's source when there are no
     *  subreddits, or as the fallback when Reddit is unreachable. */
    val aiPrompts: List<String> = emptyList(),
    // Default AI size = Full-HD for fast generation; 4K/8K categories override it.
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

    // Categories with `subreddits` pull curated phone wallpapers from Reddit (the
    // Wallcraft look); `query`/`whCategories` act as a Wallhaven fallback if Reddit
    // is unreachable. Photography themes stay on Wallhaven + stock.
    // Speed first: browsing categories use Reddit (real CDN images that load in
    // ~1-2s). AI generation (slower, on-the-fly) is reserved for the opt-in "IA"
    // and "8K" categories and for search, where a short wait is expected.
    val categories: List<Category> = listOf(
        Category(
            "Popular", "",
            subreddits = "MobileWallpaper+iphonewallpapers+WQHD_Wallpaper",
            redditSort = "hot",
            aiPrompts = AiPrompts.art
        ),
        // 4K: high-resolution REAL wallpapers from Reddit (fast); badge shows the
        // true resolution. For guaranteed 4K/8K generated art, use "8K" or "IA".
        Category(
            "4K", "",
            atleast = "1440x2560",
            subreddits = "WQHD_Wallpaper+MobileWallpaper+iphonewallpapers",
            aiPrompts = AiPrompts.art, aiWidth = 2160, aiHeight = 3840
        ),
        // 8K & IA: AI-generated (slower, on demand). 8K renders at true 8K portrait.
        Category("8K", "", aiPrompts = AiPrompts.art, aiWidth = 4320, aiHeight = 7680),
        Category("IA", "", aiPrompts = AiPrompts.art),
        Category(
            "Anime", "anime",
            whCategories = "010",
            subreddits = "Animewallpaper+AnimeWallpaper+MobileWallpaper",
            aiPrompts = AiPrompts.anime
        ),
        Category(
            "AMOLED", "amoled black",
            subreddits = "Amoledbackgrounds",
            aiPrompts = AiPrompts.amoled
        ),
        Category(
            "Minimalista", "minimal",
            subreddits = "MinimalWallpaper+minimalist",
            aiPrompts = AiPrompts.art
        ),
        Category(
            "Coches", "car",
            subreddits = "carwallpapers+CarsWallpapers",
            aiPrompts = listOf("a sleek sports car on a night city street, cinematic")
        ),
        Category(
            "Espacio", "space",
            subreddits = "spaceporn+SpaceWallpapers",
            aiPrompts = AiPrompts.art
        ),
        Category(
            "Naturaleza", "nature",
            subreddits = "EarthPorn+BackgroundArt",
            aiPrompts = AiPrompts.art
        ),
        Category(
            "Ciudad", "city",
            subreddits = "CityPorn",
            aiPrompts = listOf("futuristic city skyline at night, neon lights")
        ),
        Category(
            "Fantasía", "fantasy",
            subreddits = "ImaginaryLandscapes+ImaginaryWorlds",
            aiPrompts = AiPrompts.art
        )
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

    /** Search = generate wallpapers with the AI from whatever text the user types. */
    fun search(text: String) {
        val cat = Category(
            label = text.ifBlank { "Búsqueda" },
            query = text,
            aiPrompts = if (text.isBlank()) AiPrompts.art else listOf(text)
        )
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
                if (wallpaper.id.startsWith("rd_") && current.subreddits.isNotBlank()) {
                    // Reddit items: more from the SAME subreddits (same theme).
                    repo.browseReddit(
                        current.subreddits, "top", "all", null, current.atleast, current.label
                    ).items
                } else {
                    // Everything else: generate similar art with the AI.
                    val prompts = if (wallpaper.id.startsWith("ai_")) {
                        current.aiPrompts.ifEmpty { AiPrompts.art }
                    } else {
                        listOf(wallpaper.title.ifBlank { wallpaper.category })
                    }
                    repo.browseAi(prompts, Random.nextInt(0, 60), current.aiWidth, current.aiHeight, current.label)
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
            when {
                redditActive -> {
                    val res = runCatching {
                        repo.browseReddit(
                            current.subreddits, current.redditSort, current.redditTime,
                            redditAfter, current.atleast, current.label
                        )
                    }.getOrNull()
                    // Reddit unreachable / empty first page -> switch to AI (or Wallhaven).
                    if (res == null || (redditAfter == null && res.items.isEmpty())) {
                        redditActive = false
                        _loading.value = false
                        loadMore()
                        return@launch
                    }
                    redditAfter = res.nextAfter
                    if (res.nextAfter == null) endReached = true
                    appendItems(res.items)
                }
                else -> {
                    // AI source (also the fallback when Reddit is unreachable).
                    val prompts = current.aiPrompts.ifEmpty { AiPrompts.art }
                    val items = repo.browseAi(prompts, page, current.aiWidth, current.aiHeight, current.label)
                    appendItems(items)
                    page++
                    if (page > 40) endReached = true // safety cap; AI is otherwise endless
                }
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
