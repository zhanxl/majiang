package com.majiang.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.majiang.ui.camera.CameraScreen
import com.majiang.ui.game.GameScreen
import com.majiang.ui.history.HistoryScreen
import com.majiang.ui.settings.SettingsScreen
import com.majiang.ui.stats.StatsScreen

object Routes {
    const val GAME = "game"
    const val CAMERA = "camera"
    const val HISTORY = "history"
    const val STATS = "stats"
    const val SETTINGS = "settings"
}

@Composable
fun MajiangNavigation(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.GAME,
        modifier = modifier
    ) {
        composable(Routes.GAME) {
            GameScreen()
        }
        composable(Routes.CAMERA) {
            CameraScreen()
        }
        composable(Routes.HISTORY) {
            HistoryScreen()
        }
        composable(Routes.STATS) {
            StatsScreen()
        }
        composable(Routes.SETTINGS) {
            SettingsScreen()
        }
    }
}
