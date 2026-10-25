package com.example.wordcards.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 品牌色延续脚手架里的 #4361EE，保证和原稿视觉一致。
 * 命名规则：用途 + 明暗，避免出现 blue1/blue2 这类无法检索的名字。
 */

// ---------- Light ----------
val BrandPrimary = Color(0xFF4361EE)
val BrandPrimaryDark = Color(0xFF3347C4)
val BrandContainer = Color(0xFFE0E7FF)

val PageBackground = Color(0xFFF5F7FB)
val SurfaceCard = Color(0xFFFFFFFF)

val TextPrimary = Color(0xFF1F2937)
val TextSecondary = Color(0xFF6B7280)
val TextTertiary = Color(0xFF9CA3AF)

// ---------- Dark ----------
val DarkPageBackground = Color(0xFF111318)
val DarkSurfaceCard = Color(0xFF1C1F26)
val DarkBrandPrimary = Color(0xFF9BB0FF)
val DarkBrandContainer = Color(0xFF2A3457)
val DarkTextPrimary = Color(0xFFE6E8EC)
val DarkTextSecondary = Color(0xFFA8AEB8)

// ---------- 语义色：Leitner 盒子状态 ----------
// 低盒（易忘）偏暖，高盒（掌握）偏冷，列表与复习页复用同一套
val BoxNew = Color(0xFFF59E0B)
val BoxLearning = Color(0xFF4361EE)
val BoxMastered = Color(0xFF10B981)
