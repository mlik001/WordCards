package com.example.wordcards.ui

import androidx.compose.runtime.Composable
import com.example.wordcards.ui.navigation.WordCardsNavHost

/**
 * 应用根容器。
 *
 * 现在就只是导航宿主，但保留这一层是有意义的：
 * 将来要加「全局 Snackbar」「首启动引导」「崩溃兜底」时，
 * 都有地方可放，不必改动 MainActivity。
 */
@Composable
fun WordCardsApp() {
    WordCardsNavHost()
}
