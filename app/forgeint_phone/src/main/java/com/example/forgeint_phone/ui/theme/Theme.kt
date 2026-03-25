package com.example.forgeint.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalContext

@Immutable
data class ForgePalette(
    val primary: androidx.compose.ui.graphics.Color,
    val onPrimary: androidx.compose.ui.graphics.Color,
    val background: androidx.compose.ui.graphics.Color,
    val surface: androidx.compose.ui.graphics.Color,
    val onSurface: androidx.compose.ui.graphics.Color,
    val secondary: androidx.compose.ui.graphics.Color,
    val tertiary: androidx.compose.ui.graphics.Color,
    val primaryContainer: androidx.compose.ui.graphics.Color,
    val onPrimaryContainer: androidx.compose.ui.graphics.Color,
    val surfaceVariant: androidx.compose.ui.graphics.Color,
    val onSurfaceVariant: androidx.compose.ui.graphics.Color
)

private val defaultPalette = ForgePalette(
    primary = Primary,
    onPrimary = OnPrimary,
    background = Background,
    surface = Surface,
    onSurface = BotText,
    secondary = SettingsIcon,
    tertiary = ReplyIcon,
    primaryContainer = UserBubble,
    onPrimaryContainer = UserText,
    surfaceVariant = BotBubble,
    onSurfaceVariant = BotText
)

private val cyberpunkPalette = ForgePalette(
    primary = ColorCyberPrimary,
    onPrimary = ColorCyberOnPrimary,
    background = ColorCyberBackground,
    surface = ColorCyberSurface,
    onSurface = ColorCyberOnSurface,
    secondary = ColorCyberSecondary,
    tertiary = ColorCyberTertiary,
    primaryContainer = ColorCyberUserBubble,
    onPrimaryContainer = ColorCyberUserText,
    surfaceVariant = ColorCyberBotBubble,
    onSurfaceVariant = ColorCyberBotText
)

private val sunsetPalette = ForgePalette(
    primary = ColorSunsetPrimary,
    onPrimary = ColorSunsetOnPrimary,
    background = ColorSunsetBackground,
    surface = ColorSunsetSurface,
    onSurface = ColorSunsetOnSurface,
    secondary = ColorSunsetSecondary,
    tertiary = ColorSunsetTertiary,
    primaryContainer = ColorSunsetUserBubble,
    onPrimaryContainer = ColorSunsetUserText,
    surfaceVariant = ColorSunsetBotBubble,
    onSurfaceVariant = ColorSunsetBotText
)

private val forestDeepPalette = ForgePalette(
    primary = ColorForestPrimary,
    onPrimary = ColorForestOnPrimary,
    background = ColorForestBackground,
    surface = ColorForestSurface,
    onSurface = ColorForestOnSurface,
    secondary = ColorForestSecondary,
    tertiary = ColorForestTertiary,
    primaryContainer = ColorForestUserBubble,
    onPrimaryContainer = ColorForestUserText,
    surfaceVariant = ColorForestBotBubble,
    onSurfaceVariant = ColorForestBotText
)

private val highContrastPalette = ForgePalette(
    primary = ColorHighContrastPrimary,
    onPrimary = ColorHighContrastOnPrimary,
    background = ColorHighContrastBackground,
    surface = ColorHighContrastSurface,
    onSurface = ColorHighContrastOnSurface,
    secondary = ColorHighContrastSecondary,
    tertiary = ColorHighContrastTertiary,
    primaryContainer = ColorHighContrastUserBubble,
    onPrimaryContainer = ColorHighContrastUserText,
    surfaceVariant = ColorHighContrastBotBubble,
    onSurfaceVariant = ColorHighContrastBotText
)

fun appThemes(): List<String> = listOf(
    "Default",
    "Cyberpunk",
    "Sunset",
    "Forest Deep",
    "High Contrast"
)

private fun paletteByName(themeName: String): ForgePalette = when (themeName) {
    "Cyberpunk" -> cyberpunkPalette
    "Sunset" -> sunsetPalette
    "Forest Deep" -> forestDeepPalette
    "High Contrast" -> highContrastPalette
    else -> defaultPalette
}

@Composable
fun ForgeINTTheme(
    themeName: String = "Default",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = paletteByName(themeName)
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicDarkColorScheme(context)
        }
        else -> lightColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            background = palette.background,
            surface = palette.surface,
            onBackground = palette.onSurface,
            onSurface = palette.onSurface,
            secondary = palette.secondary,
            tertiary = palette.tertiary,
            primaryContainer = palette.primaryContainer,
            onPrimaryContainer = palette.onPrimaryContainer,
            surfaceVariant = palette.surfaceVariant,
            onSurfaceVariant = palette.onSurfaceVariant
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
