package com.fraan.kroma.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fraan.kroma.KromaApp
import com.fraan.kroma.data.AiEngine
import com.fraan.kroma.data.AiStyle
import com.fraan.kroma.data.AiStyles
import com.fraan.kroma.data.AspectRatio
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

/** Ready-made ideas shown in the Explore tab and as prompt suggestions. */
object ExploreIdeas {
    val prompts: List<String> = listOf(
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
        "anime city at dusk, makoto shinkai style, detailed",
        "pure black background with a single glowing neon wave, amoled",
        "samurai warrior under a red moon, cinematic",
        "dragon flying over a burning neon city",
        "retro synthwave sunset with palm trees and grid",
        "golden luxury marble and geometric pattern"
    )

    /** Short chips users can tap to fill the prompt box. */
    val quick: List<String> = listOf(
        "Galaxia y nebulosa",
        "Ciudad cyberpunk",
        "Dragón de fuego",
        "Paisaje anime",
        "Montañas al atardecer",
        "Coche deportivo",
        "Abstracto de colores",
        "Bosque mágico",
        "Samurái"
    )
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

    // ---- Generation controls (kept in the VM so results survive navigation) ----
    val styles: List<AiStyle> = AiStyles.all

    private val _prompt = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt.asStateFlow()

    private val _style = MutableStateFlow(AiStyles.none)
    val style: StateFlow<AiStyle> = _style.asStateFlow()

    private val _aspect = MutableStateFlow(AspectRatio.PHONE)
    val aspect: StateFlow<AspectRatio> = _aspect.asStateFlow()

    private val _engine = MutableStateFlow(AiEngine.FAST)
    val engine: StateFlow<AiEngine> = _engine.asStateFlow()

    fun setPrompt(text: String) { _prompt.value = text }
    fun setStyle(s: AiStyle) { _style.value = s }
    fun setAspect(a: AspectRatio) { _aspect.value = a }
    fun setEngine(e: AiEngine) { _engine.value = e }

    // ---- Generated results feed ----
    private val _generated = MutableStateFlow<List<Wallpaper>>(emptyList())
    val generated: StateFlow<List<Wallpaper>> = _generated.asStateFlow()

    private val _generating = MutableStateFlow(false)
    val generating: StateFlow<Boolean> = _generating.asStateFlow()

    // Increments on every press so each generation gets a fresh seed (a new image).
    private var genCounter = 0

    /**
     * Creates exactly ONE image from the current controls and adds it to the top of
     * the results. Press again to create another one — one press, one wallpaper.
     */
    fun generate() {
        if (_generating.value) return
        val p = _prompt.value
        val s = _style.value
        val a = _aspect.value
        val e = _engine.value
        genCounter++
        val nonce = genCounter
        _generating.value = true
        viewModelScope.launch {
            val one = runCatching { repo.generate(p, s, a, e, nonce) }
                .getOrDefault(emptyList())
                .firstOrNull()
            if (one != null) {
                cache[one.id] = one
                _generated.value = listOf(one) + _generated.value.filterNot { it.id == one.id }
            }
            _generating.value = false
        }
    }

    // ---- Explore (ready-made AI showcase) ----
    private val _explore = MutableStateFlow<List<Wallpaper>>(emptyList())
    val explore: StateFlow<List<Wallpaper>> = _explore.asStateFlow()

    private val _exploreLoading = MutableStateFlow(false)
    val exploreLoading: StateFlow<Boolean> = _exploreLoading.asStateFlow()

    private var explorePage = 0

    fun loadMoreExplore() {
        if (_exploreLoading.value) return
        _exploreLoading.value = true
        viewModelScope.launch {
            val items = runCatching { repo.exploreFeed(ExploreIdeas.prompts, explorePage) }
                .getOrDefault(emptyList())
            explorePage++
            append(_explore, items)
            _exploreLoading.value = false
        }
    }

    // ---- Detail: more variations ----
    private val _related = MutableStateFlow<List<Wallpaper>>(emptyList())
    val related: StateFlow<List<Wallpaper>> = _related.asStateFlow()
    private var relatedForId: String? = null

    fun loadRelated(wallpaper: Wallpaper) {
        cache[wallpaper.id] = wallpaper
        if (relatedForId == wallpaper.id) return
        relatedForId = wallpaper.id
        _related.value = emptyList()
        viewModelScope.launch {
            val results = runCatching { repo.variationsOf(wallpaper, Random.nextInt(1, 80)) }
                .getOrDefault(emptyList())
                .filter { it.id != wallpaper.id }
                .take(12)
            results.forEach { cache[it.id] = it }
            _related.value = results
        }
    }

    // ---- Favourites ----
    val favoriteWallpapers: StateFlow<List<Wallpaper>> =
        repo.favoriteWallpapers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<Set<String>> =
        repo.favoriteIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggleFavorite(wallpaper: Wallpaper) {
        viewModelScope.launch { repo.toggleFavorite(wallpaper) }
    }

    fun wallpaperById(id: String): Wallpaper? =
        cache[id] ?: favoriteWallpapers.value.firstOrNull { it.id == id }

    init {
        loadMoreExplore()
    }

    private fun append(flow: MutableStateFlow<List<Wallpaper>>, items: List<Wallpaper>) {
        items.forEach { cache[it.id] = it }
        val existing = flow.value.mapTo(HashSet()) { it.id }
        flow.value = flow.value + items.filter { it.id !in existing }
    }
}
