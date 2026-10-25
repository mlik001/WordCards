package com.example.wordcards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.wordcards.ui.WordCardsApp
import com.example.wordcards.ui.theme.WordCardsTheme

/**
 * 单 Activity 架构的唯一入口。
 * 页面切换全部交给 Navigation Compose，不再有第二个 Activity。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            WordCardsTheme {
                WordCardsApp()
            }
        }
    }
}
