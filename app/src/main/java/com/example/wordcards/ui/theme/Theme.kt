package com.example.wordcards.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = SurfaceCard,
    primaryContainer = BrandContainer,
    onPrimaryContainer = BrandPrimaryDark,
    secondary = BrandPrimaryDark,
    onSecondary = SurfaceCard,
    background = PageBackground,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outlineVariant = BrandContainer
)

private val DarkColors = darkColorScheme(
    primary = DarkBrandPrimary,
    onPrimary = DarkPageBackground,
    primaryContainer = DarkBrandContainer,
    onPrimaryContainer = DarkTextPrimary,
    secondary = DarkBrandPrimary,
    onSecondary = DarkPageBackground,
    background = DarkPageBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurfaceCard,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary
)

/**
 * 全应用唯一主题入口。
 *
 * 刻意不做动态取色（Material You）：背单词页面依赖固定的品牌色来区分
 * 「新词/学习中/已掌握」，跟随系统壁纸变色会破坏这层语义。
 */
@Composable
fun WordCardsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = WordCardsTypography,
        content = content
    )
}
