package com.englishreader.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.englishreader.ui.theme.GlassShadow
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.englishreader.ui.screens.flashcard.FlashcardScreen
import com.englishreader.ui.screens.home.HomeScreen
import com.englishreader.ui.screens.rss.RssManageScreen
import com.englishreader.ui.screens.notes.NotesScreen
import com.englishreader.ui.screens.reader.ReaderScreen
import com.englishreader.ui.screens.settings.SettingsScreen
import com.englishreader.ui.screens.stats.StatsScreen

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen("home", "首页", Icons.Filled.Home, Icons.Outlined.Home)
    data object Notes : Screen("notes", "笔记本", Icons.Filled.Book, Icons.Outlined.Book)
    data object Stats : Screen("stats", "统计", Icons.Filled.BarChart, Icons.Outlined.BarChart)
    data object Settings : Screen("settings", "设置", Icons.Filled.Settings, Icons.Outlined.Settings)
    data object Reader : Screen("reader/{articleId}", "阅读", Icons.Filled.Book, Icons.Outlined.Book) {
        fun createRoute(articleId: String) = "reader/$articleId"
    }
    data object Flashcard : Screen("flashcard", "复习", Icons.Filled.Book, Icons.Outlined.Book)
    data object RssManage : Screen("rss_manage", "RSS管理", Icons.Filled.Settings, Icons.Outlined.Settings)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Notes,
    Screen.Stats,
    Screen.Settings
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Hide bottom bar on reader, flashcard, and rss manage screens
    val showBottomBar = currentDestination?.route?.startsWith("reader") != true &&
            currentDestination?.route != Screen.Flashcard.route &&
            currentDestination?.route != Screen.RssManage.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    NavigationBar(
                        modifier = Modifier
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(28.dp),
                                ambientColor = GlassShadow,
                                spotColor = GlassShadow
                            )
                            .clip(RoundedCornerShape(28.dp)),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        bottomNavItems.forEach { screen ->
                            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = {
                                    Text(
                                        screen.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                selected = selected,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ),
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onArticleClick = { articleId ->
                        navController.navigate(Screen.Reader.createRoute(articleId))
                    }
                )
            }
            
            composable(Screen.Notes.route) {
                NotesScreen(
                    onNavigateToFlashcard = {
                        navController.navigate(Screen.Flashcard.route)
                    }
                )
            }
            
            composable(Screen.Flashcard.route) {
                FlashcardScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.Stats.route) {
                StatsScreen()
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToRssManage = {
                        navController.navigate(Screen.RssManage.route)
                    }
                )
            }
            
            composable(Screen.RssManage.route) {
                RssManageScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(
                route = Screen.Reader.route,
                arguments = listOf(navArgument("articleId") { type = NavType.StringType })
            ) { backStackEntry ->
                val articleId = backStackEntry.arguments?.getString("articleId") ?: ""
                ReaderScreen(
                    articleId = articleId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
