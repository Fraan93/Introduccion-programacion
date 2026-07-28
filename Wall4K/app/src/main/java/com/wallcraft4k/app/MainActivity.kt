package com.wallcraft4k.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wallcraft4k.app.data.model.WallpaperSource
import com.wallcraft4k.app.ui.WallViewModel
import com.wallcraft4k.app.ui.navigation.Screen
import com.wallcraft4k.app.ui.navigation.TopLevelDestination
import com.wallcraft4k.app.ui.screens.CategoriesScreen
import com.wallcraft4k.app.ui.screens.DetailScreen
import com.wallcraft4k.app.ui.screens.FavoritesScreen
import com.wallcraft4k.app.ui.screens.HomeScreen
import com.wallcraft4k.app.ui.screens.UploadScreen
import com.wallcraft4k.app.ui.theme.Wall4KTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            Wall4KTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val navController = rememberNavController()
    val vm: WallViewModel = viewModel()

    val browse by vm.browse.collectAsState()
    val loading by vm.loading.collectAsState()
    val selectedCategory by vm.selectedCategory.collectAsState()
    val favoriteIds by vm.favoriteIds.collectAsState()
    val favoriteList by vm.favoriteWallpapers.collectAsState()
    val uploads by vm.uploads.collectAsState()
    val uploading by vm.uploading.collectAsState()

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
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
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
                // Show the user's uploads at the top of the "Popular" feed.
                val displayed =
                    if (selectedCategory == vm.categories.first()) uploads + browse else browse
                HomeScreen(
                    categories = vm.categories,
                    selected = selectedCategory,
                    wallpapers = displayed,
                    favorites = favoriteIds,
                    loading = loading,
                    onSelectCategory = { vm.selectCategory(it) },
                    onSearch = { vm.search(it) },
                    onOpen = { navController.navigate(Screen.Detail.createRoute(it.id)) },
                    onToggleFavorite = { vm.toggleFavorite(it) },
                    onLoadMore = { vm.loadMore() }
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
            composable(TopLevelDestination.UPLOAD.route) {
                UploadScreen(
                    categories = vm.categories.map { it.label },
                    isRemote = vm.isRemote,
                    uploading = uploading,
                    onSubmit = { uri, title, author, category, onResult ->
                        vm.addUpload(uri, title, author, category, onResult)
                    }
                )
            }
            composable(TopLevelDestination.FAVORITES.route) {
                FavoritesScreen(
                    favoritesList = favoriteList,
                    favorites = favoriteIds,
                    onOpen = { navController.navigate(Screen.Detail.createRoute(it.id)) },
                    onToggleFavorite = { vm.toggleFavorite(it) }
                )
            }
            composable(Screen.Detail.route) { entry ->
                val id = entry.arguments?.getString("id")
                val wp = id?.let { vm.wallpaperById(it) }
                if (wp == null) {
                    navController.popBackStack()
                } else {
                    DetailScreen(
                        wallpaper = wp,
                        isFavorite = wp.id in favoriteIds,
                        onBack = { navController.popBackStack() },
                        onToggleFavorite = { vm.toggleFavorite(wp) },
                        onDelete = if (wp.source == WallpaperSource.UPLOAD && !vm.isRemote) {
                            { vm.deleteUpload(wp.id) }
                        } else null
                    )
                }
            }
        }
    }
}
