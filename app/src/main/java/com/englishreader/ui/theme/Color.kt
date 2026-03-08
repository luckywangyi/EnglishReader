package com.englishreader.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// Apple-Inspired Light Theme Colors
// =============================================================================

// Primary - iOS Blue
val Primary = Color(0xFF007AFF)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFE5F1FF)
val OnPrimaryContainer = Color(0xFF001D36)

// Secondary - Neutral Gray
val Secondary = Color(0xFF8E8E93)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFF2F2F7)
val OnSecondaryContainer = Color(0xFF1D1D1F)

// Tertiary - iOS Purple
val Tertiary = Color(0xFF5856D6)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFEEEEFF)
val OnTertiaryContainer = Color(0xFF1C1B3A)

// Error - iOS Red
val Error = Color(0xFFFF3B30)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFEDEB)
val OnErrorContainer = Color(0xFF410002)

// Surfaces - Warm tones for comfortable reading
val Background = Color(0xFFFAFAF8)          // 暖白，替代纯白 #F2F2F7
val OnBackground = Color(0xFF1A1A1A)         // 深灰，替代纯黑 #1D1D1F
val Surface = Color(0xFFFCFCFA)              // 暖白表面，替代纯白
val OnSurface = Color(0xFF1A1A1A)            // 深灰正文
val SurfaceVariant = Color(0xFFF5F5F3)       // 暖灰变体
val OnSurfaceVariant = Color(0xFF86868B)
val Outline = Color(0xFFD1D1D6)
val OutlineVariant = Color(0xFFE5E5EA)

// =============================================================================
// Apple-Inspired Dark Theme Colors
// =============================================================================

val PrimaryDark = Color(0xFF0A84FF)
val OnPrimaryDark = Color(0xFFFFFFFF)
val PrimaryContainerDark = Color(0xFF003A70)
val OnPrimaryContainerDark = Color(0xFFD1E4FF)

val SecondaryDark = Color(0xFF98989D)
val OnSecondaryDark = Color(0xFFFFFFFF)
val SecondaryContainerDark = Color(0xFF2C2C2E)
val OnSecondaryContainerDark = Color(0xFFE5E5EA)

val TertiaryDark = Color(0xFF5E5CE6)
val OnTertiaryDark = Color(0xFFFFFFFF)
val TertiaryContainerDark = Color(0xFF3A3A5C)
val OnTertiaryContainerDark = Color(0xFFEEEEFF)

val ErrorDark = Color(0xFFFF453A)
val OnErrorDark = Color(0xFFFFFFFF)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val BackgroundDark = Color(0xFF121212)          // 深灰，替代纯黑，避免 OLED 拖影
val OnBackgroundDark = Color(0xFFE0E0E0)         // 柔白，替代纯白，保护眼睛
val SurfaceDark = Color(0xFF1C1C1E)
val OnSurfaceDark = Color(0xFFE0E0E0)            // 柔白正文
val SurfaceVariantDark = Color(0xFF2C2C2E)
val OnSurfaceVariantDark = Color(0xFFAEAEB2)     // 提亮辅助文字，增强可读性
val OutlineDark = Color(0xFF48484A)
val OutlineVariantDark = Color(0xFF38383A)

// =============================================================================
// Glassmorphism Colors
// =============================================================================

val GlassWhite = Color(0xFFFFFFFF)
val GlassBackground = Color(0xE6FFFFFF)      // 90% white - frosted glass
val GlassBackgroundLight = Color(0xCCFFFFFF) // 80% white - lighter glass
val GlassBorder = Color(0x33FFFFFF)          // 20% white border
val GlassShadow = Color(0x14000000)          // 8% black shadow

// Dark mode glass
val GlassBackgroundDark = Color(0xCC1C1C1E)  // 80% dark surface
val GlassBorderDark = Color(0x33FFFFFF)      // 20% white border

// =============================================================================
// Sepia / Eye-Care Mode Colors (Kindle-like warm tones)
// =============================================================================

val SepiaBackground = Color(0xFFF5EDDC)          // 暖黄纸张背景
val SepiaOnBackground = Color(0xFF3D3229)         // 深褐正文
val SepiaSurface = Color(0xFFF8F0E0)              // 暖黄表面
val SepiaOnSurface = Color(0xFF3D3229)            // 深褐正文
val SepiaSurfaceVariant = Color(0xFFEDE5D4)       // 暖黄变体
val SepiaOnSurfaceVariant = Color(0xFF6B5D4E)     // 中褐辅助文字
val SepiaPrimary = Color(0xFF8B6914)              // 暖金主题色
val SepiaOnPrimary = Color(0xFFFFFFFF)
val SepiaPrimaryContainer = Color(0xFFEDE0C0)
val SepiaOnPrimaryContainer = Color(0xFF3D2E00)
val SepiaOutline = Color(0xFFD4C8B0)
val SepiaOutlineVariant = Color(0xFFE0D6C2)

// =============================================================================
// Refined Difficulty Colors (Muted, Apple-like)
// =============================================================================

val DifficultyEasy = Color(0xFF34C759)       // iOS Green
val DifficultyMedium = Color(0xFFFF9500)     // iOS Orange
val DifficultyHard = Color(0xFFFF3B30)       // iOS Red
val DifficultyAdvanced = Color(0xFFAF52DE)   // iOS Purple

// =============================================================================
// Category Colors (Muted, Low Saturation)
// =============================================================================

val CategoryNews = Color(0xFF007AFF)         // iOS Blue
val CategoryTech = Color(0xFF5856D6)         // iOS Purple
val CategoryBusiness = Color(0xFF34C759)     // iOS Green
val CategoryScience = Color(0xFFFF9500)      // iOS Orange
val CategorySpeech = Color(0xFF5AC8FA)       // iOS Teal
val CategoryCulture = Color(0xFFFF2D55)      // iOS Pink
