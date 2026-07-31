package com.fraan.kroma

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fraan.kroma.data.ThemeMode
import com.fraan.kroma.ui.WallViewModel
import com.fraan.kroma.ui.navigation.Screen
import com.fraan.kroma.ui.navigation.TopLevelDestination
import com.fraan.kroma.ui.screens.CategoriesScreen
import com.fraan.kroma.ui.screens.DetailScreen
import com.fraan.kroma.ui.screens.FavoritesScreen
import com.fraan.kroma.ui.screens.HomeScreen
import com.fraan.kroma.ui.screens.PaywallScreen
import com.fraan.kroma.ui.screens.SettingsScreen
import com.fraan.kroma.ui.theme.KromaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val theme by (application as KromaApp).settingsRepository.theme
                .collectAsState(initial = ThemeMode.SYSTEM)
            KromaTheme(
                darkTheme = when (theme) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                }
            ) {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val navController = rememberNavController()
    val vm: WallViewModel = viewModel()
    val context = LocalContext.current

    val browse by vm.browse.collectAsState()
    val loading by vm.loading.collectAsState()
    val selectedCategory by vm.selectedCategory.collectAsState()
    val selectedRes by vm.selectedRes.collectAsState()
    val favoriteIds by vm.favoriteIds.collectAsState()
    val favoriteList by vm.favoriteWallpapers.collectAsState()
    val isPremium by vm.isPremium.collectAsState()
    val activePlan by vm.activePlan.collectAsState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = TopLevelDestination.entries.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val destination = backStackEntry?.destination
                    TopLevelDestination.entries.forEach { dest ->
                        val selected = destination?.hierarchy?.any { it.route == dest.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.HOME.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(TopLevelDestination.HOME.route) {
                HomeScreen(
                    categories = vm.categories,
                    resFilters = vm.resFilters,
                    selected = selectedCategory,
                    selectedRes = selectedRes,
                    wallpapers = browse,
                    loading = loading,
                    isPremium = isPremium,
                    onSelectCategory = { vm.selectCategory(it) },
                    onSelectRes = { vm.selectRes(it) },
                    onSearch = { vm.search(it) },
                    onOpen = { navController.navigate(Screen.Detail.createRoute(it.id)) },
                    onLoadMore = { vm.loadMore() },
                    onOpenPremium = { navController.navigate(Screen.Paywall.route) },
                    onOpenSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(TopLevelDestination.CATEGORIES.route) {
                CategoriesScreen(
                    categories = vm.categories,
                    onPick = { category ->
                        vm.selectCategory(category)
                        navController.navigate(TopLevelDestination.HOME.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(TopLevelDestination.FAVORITES.route) {
                FavoritesScreen(
                    favoritesList = favoriteList,
                    onOpen = { navController.navigate(Screen.Detail.createRoute(it.id)) }
                )
            }
            composable(Screen.Paywall.route) {
                PaywallScreen(
                    isPremium = isPremium,
                    activePlan = activePlan,
                    onActivate = { plan ->
                        vm.activatePremium(plan) {
                            Toast.makeText(
                                context,
                                "¡PRO activado (${plan.title})! Compra simulada — sin cargo real.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Settings.route) {
                val currentTheme by vm.theme.collectAsState()
                SettingsScreen(
                    theme = currentTheme,
                    onSetTheme = { vm.setTheme(it) },
                    isPremium = isPremium,
                    activePlan = activePlan,
                    onOpenPaywall = { navController.navigate(Screen.Paywall.route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Detail.route) { entry ->
                val id = entry.arguments?.getString("id")
                val wp = id?.let { vm.wallpaperById(it) }
                if (wp == null) {
                    navController.popBackStack()
                } else {
                    val related by vm.related.collectAsState()
                    LaunchedEffect(wp.id) { vm.loadRelated(wp) }
                    DetailScreen(
                        wallpaper = wp,
                        isFavorite = wp.id in favoriteIds,
                        related = related,
                        onBack = { navController.popBackStack() },
                        onToggleFavorite = { vm.toggleFavorite(wp) },
                        onOpenRelated = { next ->
                            navController.navigate(Screen.Detail.createRoute(next.id)) {
                                popUpTo(Screen.Detail.route) { inclusive = true }
                            }
                        },
                        onDelete = null
                    )
                }
            }
        }
    }
}
