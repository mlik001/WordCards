package com.example.wordcards.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.wordcards.ui.addedit.AddEditScreen
import com.example.wordcards.ui.addedit.AddEditViewModel
import com.example.wordcards.ui.list.WordListScreen
import com.example.wordcards.ui.settings.SettingsScreen
import com.example.wordcards.ui.stats.StatsScreen
import com.example.wordcards.ui.study.StudyScreen
import com.example.wordcards.ui.wordbook.WordBookScreen

/**
 * 导航图。
 *
 * 所有跳转都带 `launchSingleTop = true`：连点两下按钮不会压两层同样的页面，
 * 否则用户返回时要按两次才回到列表，是最容易被忽略的体验 bug。
 */
@Composable
fun WordCardsNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.WORDS,
        modifier = modifier
    ) {
        composable(Routes.WORDS) {
            WordListScreen(
                onAddWord = { navController.singleTop(Routes.wordEdit()) },
                onEditWord = { id -> navController.singleTop(Routes.wordEdit(id)) },
                onStartStudy = { navController.singleTop(Routes.STUDY) },
                onOpenWordBooks = { navController.singleTop(Routes.WORDBOOKS) },
                onOpenStats = { navController.singleTop(Routes.STATS) },
                onOpenSettings = { navController.singleTop(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.WORD_EDIT,
            arguments = listOf(
                navArgument(Routes.ARG_WORD_ID) {
                    type = NavType.LongType
                    defaultValue = AddEditViewModel.NEW_WORD_ID
                }
            )
        ) {
            AddEditScreen(onDone = { navController.popBackStack() })
        }

        composable(Routes.STUDY) {
            StudyScreen(onExit = { navController.popBackStack() })
        }

        composable(Routes.STATS) {
            StatsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.WORDBOOKS) {
            WordBookScreen(onBack = { navController.popBackStack() })
        }
    }
}

private fun NavHostController.singleTop(route: String) {
    navigate(route) { launchSingleTop = true }
}
