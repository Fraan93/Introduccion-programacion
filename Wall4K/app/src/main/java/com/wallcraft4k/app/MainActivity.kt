package com.wallcraft4k.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.collectAsState
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
                Wall4KApp()
            }
        }
    }
}

@Composable
private fun Wall4KApp() {
    val navController = rememberNavController()
    val vm: WallViewModel = viewModel()

    val wallpapers by vm.wallpapers.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val categories by vm.categories.collectAsState()
    val favoriteList by vm.favoriteWallpapers.collectAsState()
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
                        val selected =
                            destination?.hierarchy?.any { it.route == dest.route } == true
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
                HomeScreen(
                    wallpapers = wallpapers,
                    favorites = favorites,
                    categories = categories,
                    onOpen = { navController.navigate(Screen.Detail.createRoute(it.id)) },
                    onToggleFavorite = { vm.toggleFavorite(it.id) }
                )
            }
            composable(TopLevelDestination.CATEGORIES.route) {
                CategoriesScreen(
                    wallpapers = wallpapers,
                    favorites = favorites,
                    categories = categories,
                    onOpen = { navController.navigate(Screen.Detail.createRoute(it.id)) },
                    onToggleFavorite = { vm.toggleFavorite(it.id) }
                )
            }
            composable(TopLevelDestination.UPLOAD.route) {
                UploadScreen(
                    categories = categories,
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
                    favorites = favorites,
                    onOpen = { navController.navigate(Screen.Detail.createRoute(it.id)) },
                    onToggleFavorite = { vm.toggleFavorite(it.id) }
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
                        isFavorite = wp.id in favorites,
                        onBack = { navController.popBackStack() },
                        onToggleFavorite = { vm.toggleFavorite(wp.id) },
                        // Delete only for on-device uploads. In shared (Firebase) mode
                        // ownership isn't tracked yet, so we don't expose delete.
                        onDelete = if (wp.source == WallpaperSource.UPLOAD && !vm.isRemote) {
                            { vm.deleteUpload(wp.id) }
                        } else null
                    )
                }
            }
        }
    }
}
