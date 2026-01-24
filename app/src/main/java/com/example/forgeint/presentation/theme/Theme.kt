package com.example.forgeint.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

// 1. Define the custom color system
@Immutable
data class ForgeIntColors(
    val background: Color,
    val surface: Color,
    val primary: Color, // Accent
    val onPrimary: Color,
    val userBubble: Color,
    val botBubble: Color,
    val userText: Color,
    val botText: Color,
    val settingsIcon: Color,
    val replyIcon: Color
)

// 2. Define the Themes
val DefaultTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF121212),
    primary = Color(0xFFDCB17E),
    onPrimary = Color.Black,
    userBubble = Color(0xFF1E222C),
    botBubble = Color(0xFF0F1113),
    userText = Color(0xFFD1E1FF),
    botText = Color.White,
    settingsIcon = Color(0xFF75AEFD),
    replyIcon = Color(0xFF86AFF0)
)

val CyberpunkTheme = ForgeIntColors(
    background = Color(0xFF020205),
    surface = Color(0xFF0B0B15),
    primary = Color(0xFF00FF9D),
    onPrimary = Color.Black,
    userBubble = Color(0xFF1A002E),
    botBubble = Color(0xFF000D1A),
    userText = Color(0xFFFF71FF),
    botText = Color(0xFF00FFFF),
    settingsIcon = Color(0xFFFF0055),
    replyIcon = Color(0xFF00FF9D)
)

val NatureTheme = ForgeIntColors(
    background = Color(0xFF050805),
    surface = Color(0xFF0E140E),
    primary = Color(0xFF81C784),
    onPrimary = Color.Black,
    userBubble = Color(0xFF1B241B),
    botBubble = Color(0xFF0D110D),
    userText = Color(0xFFE8F5E9),
    botText = Color.White,
    settingsIcon = Color(0xFFAED581),
    replyIcon = Color(0xFF81C784)
)

val MinimalTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF0D0D0D),
    primary = Color(0xFFFFFFFF),
    onPrimary = Color.Black,
    userBubble = Color(0xFF1A1A1A),
    botBubble = Color(0xFF0D0D0D),
    userText = Color.White,
    botText = Color(0xFFE0E0E0),
    settingsIcon = Color.White,
    replyIcon = Color.LightGray
)

val OledTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color.Black,
    primary = Color(0xFFFFD700),
    onPrimary = Color.Black,
    userBubble = Color(0xFF111111),
    botBubble = Color.Black,
    userText = Color.White,
    botText = Color(0xFFF0F0F0),
    settingsIcon = Color(0xFFFFD700),
    replyIcon = Color.White
)

val MatrixTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF050505),
    primary = Color(0xFF00FF41),
    onPrimary = Color.Black,
    userBubble = Color(0xFF001A00),
    botBubble = Color.Black,
    userText = Color(0xFF00FF41),
    botText = Color(0xFF00FF41),
    settingsIcon = Color(0xFF00FF41),
    replyIcon = Color(0xFF00FF41)
)

val SolarizedTheme = ForgeIntColors(
    background = Color(0xFF00141A),
    surface = Color(0xFF00212B),
    primary = Color(0xFF268BD2),
    onPrimary = Color.White,
    userBubble = Color(0xFF073642),
    botBubble = Color(0xFF001B22),
    userText = Color(0xFFFDF6E3),
    botText = Color(0xFFEEE8D5),
    settingsIcon = Color(0xFFD33682),
    replyIcon = Color(0xFF2AA198)
)

val CottonCandyTheme = ForgeIntColors(
    background = Color(0xFF0D0D10),
    surface = Color(0xFF16161C),
    primary = Color(0xFFFFB7B2),
    onPrimary = Color.Black,
    userBubble = Color(0xFF252533),
    botBubble = Color(0xFF16161C),
    userText = Color(0xFFFFE4E1),
    botText = Color(0xFFE0FFE0),
    settingsIcon = Color(0xFFB5EAD7),
    replyIcon = Color(0xFFC7CEEA)
)

val HighContrastTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color.Black,
    primary = Color.White,
    onPrimary = Color.Black,
    userBubble = Color(0xFF0F0F0F),
    botBubble = Color.Black,
    userText = Color.Yellow,
    botText = Color.White,
    settingsIcon = Color.White,
    replyIcon = Color.Yellow
)

val CrimsonTheme = ForgeIntColors(
    background = Color(0xFF0F0505),
    surface = Color(0xFF1C0A0A),
    primary = Color(0xFFDC143C),
    onPrimary = Color.White,
    userBubble = Color(0xFF2D0E0E),
    botBubble = Color(0xFF140707),
    userText = Color(0xFFFFEAEA),
    botText = Color(0xFFE8E8E8),
    settingsIcon = Color(0xFFFF4D4D),
    replyIcon = Color(0xFFFF1A1A)
)

val SunsetTheme = ForgeIntColors(
    background = Color(0xFF0F0805),
    surface = Color(0xFF1C100A),
    primary = Color(0xFFFF7E5F),
    onPrimary = Color.Black,
    userBubble = Color(0xFF2D1A10),
    botBubble = Color(0xFF140B07),
    userText = Color(0xFFFFEAD1),
    botText = Color(0xFFE8E8E8),
    settingsIcon = Color(0xFFFFB347),
    replyIcon = Color(0xFFFFCC33)
)

val DarkPurpleTheme = ForgeIntColors(
    background = Color(0xFF0A050F),
    surface = Color(0xFF140A1C),
    primary = Color(0xFF9D46FF),
    onPrimary = Color.White,
    userBubble = Color(0xFF221033),
    botBubble = Color(0xFF100818),
    userText = Color(0xFFEAD1FF),
    botText = Color(0xFFE8E8E8),
    settingsIcon = Color(0xFFD485FF),
    replyIcon = Color(0xFFB388FF)
)

val MidnightBlueTheme = ForgeIntColors(
    background = Color(0xFF02040A),
    surface = Color(0xFF0A101C),
    primary = Color(0xFF4C8BF5),
    onPrimary = Color.White,
    userBubble = Color(0xFF122036),
    botBubble = Color(0xFF06090E),
    userText = Color(0xFFD6E4FF),
    botText = Color(0xFFE0E0E0),
    settingsIcon = Color(0xFF6DA7FC),
    replyIcon = Color(0xFF4C8BF5)
)

val ForestDeepTheme = ForgeIntColors(
    background = Color(0xFF030A03),
    surface = Color(0xFF0A140A),
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    userBubble = Color(0xFF142914),
    botBubble = Color(0xFF050F05),
    userText = Color(0xFFC8E6C9),
    botText = Color(0xFFE0E0E0),
    settingsIcon = Color(0xFF4CAF50),
    replyIcon = Color(0xFF81C784)
)

val SlateGrayTheme = ForgeIntColors(
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    primary = Color(0xFF90A4AE),
    onPrimary = Color.Black,
    userBubble = Color(0xFF2C2C2C),
    botBubble = Color(0xFF181818),
    userText = Color(0xFFECEFF1),
    botText = Color(0xFFE0E0E0),
    settingsIcon = Color(0xFFCFD8DC),
    replyIcon = Color(0xFFB0BEC5)
)

val RoyalGoldTheme = ForgeIntColors(
    background = Color(0xFF0F0F05),
    surface = Color(0xFF1C1C0A),
    primary = Color(0xFFFFD700),
    onPrimary = Color.Black,
    userBubble = Color(0xFF2D2D10),
    botBubble = Color(0xFF141407),
    userText = Color(0xFFFFF9C4),
    botText = Color(0xFFE8E8E8),
    settingsIcon = Color(0xFFFFE082),
    replyIcon = Color(0xFFFFD54F)
)

val NeonVioletTheme = ForgeIntColors(
    background = Color(0xFF05000A),
    surface = Color(0xFF120024),
    primary = Color(0xFFD500F9),
    onPrimary = Color.White,
    userBubble = Color(0xFF2A0052),
    botBubble = Color(0xFF0E001A),
    userText = Color(0xFFFFD6FF),
    botText = Color(0xFFE0E0E0),
    settingsIcon = Color(0xFFEA80FC),
    replyIcon = Color(0xFFE040FB)
)

// 3. Create CompositionLocal
val LocalForgeIntColors = staticCompositionLocalOf { DefaultTheme }

// 4. Helper to map string to theme
fun getThemeByName(name: String): ForgeIntColors {
    return when (name) {
        "Cyberpunk" -> CyberpunkTheme
        "Nature" -> NatureTheme
        "Minimal" -> MinimalTheme
        "OLED" -> OledTheme
        "Matrix" -> MatrixTheme
        "Solarized" -> SolarizedTheme
        "Cotton Candy" -> CottonCandyTheme
        "High Contrast" -> HighContrastTheme
        "Crimson" -> CrimsonTheme
        "Sunset" -> SunsetTheme
        "Dark Purple" -> DarkPurpleTheme
        "Midnight Blue" -> MidnightBlueTheme
        "Forest Deep" -> ForestDeepTheme
        "Slate Gray" -> SlateGrayTheme
        "Royal Gold" -> RoyalGoldTheme
        "Neon Violet" -> NeonVioletTheme
        else -> DefaultTheme
    }
}
// 5. The Theme Composable
@Composable
fun ForgeINTTheme(
    themeName: String = "Default",
    content: @Composable () -> Unit
) {
    val colors = getThemeByName(themeName)

    val wearColors = Colors(
        primary = colors.primary,
        background = colors.background,
        surface = colors.surface,
        onPrimary = colors.onPrimary,
        onSurface = colors.primary, // Text on background usually
        onBackground = colors.primary
    )

    CompositionLocalProvider(
        LocalForgeIntColors provides colors
    ) {
        MaterialTheme(
            colors = wearColors,
            content = content
        )
    }
}
