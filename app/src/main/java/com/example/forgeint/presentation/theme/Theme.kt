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
    background = Color.Black,
    surface = Color(0xFF111329),
    primary = Color(0xFF00E5FF),
    onPrimary = Color.Black,
    userBubble = Color(0xFF2A0B4A),
    botBubble = Color(0xFF00313A),
    userText = Color(0xFFFF8AEF),
    botText = Color(0xFF9CFBFF),
    settingsIcon = Color(0xFFFFD740),
    replyIcon = Color(0xFF76FF03)
)

val NatureTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF122017),
    primary = Color(0xFF6EEB83),
    onPrimary = Color.Black,
    userBubble = Color(0xFF2C4A2F),
    botBubble = Color(0xFF163224),
    userText = Color(0xFFF1FFE7),
    botText = Color(0xFFC8FFD5),
    settingsIcon = Color(0xFFFFD166),
    replyIcon = Color(0xFF72D6FF)
)

val MinimalTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF121212),
    primary = Color(0xFFFFFFFF),
    onPrimary = Color.Black,
    userBubble = Color(0xFF242424),
    botBubble = Color(0xFF101010),
    userText = Color.White,
    botText = Color(0xFFCFD8DC),
    settingsIcon = Color(0xFF64B5F6),
    replyIcon = Color(0xFFFFCC80)
)

val OledTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color.Black,
    primary = Color(0xFFFFC400),
    onPrimary = Color.Black,
    userBubble = Color(0xFF171717),
    botBubble = Color(0xFF050505),
    userText = Color(0xFFFFFFFF),
    botText = Color(0xFFB3E5FC),
    settingsIcon = Color(0xFFFF5252),
    replyIcon = Color(0xFF69F0AE)
)

val MatrixTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF060A06),
    primary = Color(0xFF00FF41),
    onPrimary = Color.Black,
    userBubble = Color(0xFF0A2A0A),
    botBubble = Color(0xFF031403),
    userText = Color(0xFFA5FFB3),
    botText = Color(0xFF7CFF8D),
    settingsIcon = Color(0xFFFFEA00),
    replyIcon = Color(0xFF40C4FF)
)

val SolarizedTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF00242E),
    primary = Color(0xFF4FC3F7),
    onPrimary = Color.White,
    userBubble = Color(0xFF0D3F4A),
    botBubble = Color(0xFF07303B),
    userText = Color(0xFFFDF6E3),
    botText = Color(0xFFE6F7FF),
    settingsIcon = Color(0xFFFF6FAE),
    replyIcon = Color(0xFF7CFFCB)
)

val CottonCandyTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF1F1830),
    primary = Color(0xFFFF9ECF),
    onPrimary = Color.Black,
    userBubble = Color(0xFF3A2950),
    botBubble = Color(0xFF2A1E3D),
    userText = Color(0xFFFFF0FA),
    botText = Color(0xFFE0FFFB),
    settingsIcon = Color(0xFF80D8FF),
    replyIcon = Color(0xFFFFF176)
)

val HighContrastTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF101010),
    primary = Color.White,
    onPrimary = Color.Black,
    userBubble = Color(0xFF1B1B1B),
    botBubble = Color.Black,
    userText = Color(0xFFFFFF00),
    botText = Color(0xFF00E5FF),
    settingsIcon = Color(0xFFFF5252),
    replyIcon = Color(0xFF69F0AE)
)

val CrimsonTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF261017),
    primary = Color(0xFFFF4D6D),
    onPrimary = Color.White,
    userBubble = Color(0xFF4A1821),
    botBubble = Color(0xFF32101A),
    userText = Color(0xFFFFE8EE),
    botText = Color(0xFFFFCAD6),
    settingsIcon = Color(0xFFFFC857),
    replyIcon = Color(0xFF7EE8FA)
)

val SunsetTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF2A1610),
    primary = Color(0xFFFF8A5B),
    onPrimary = Color.Black,
    userBubble = Color(0xFF4A271A),
    botBubble = Color(0xFF3A1E15),
    userText = Color(0xFFFFEEDB),
    botText = Color(0xFFFFD9C2),
    settingsIcon = Color(0xFFFFEE58),
    replyIcon = Color(0xFF80D8FF)
)

val DarkPurpleTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF1C1030),
    primary = Color(0xFFB388FF),
    onPrimary = Color.White,
    userBubble = Color(0xFF362052),
    botBubble = Color(0xFF27183D),
    userText = Color(0xFFF3E5FF),
    botText = Color(0xFFE0D4FF),
    settingsIcon = Color(0xFFFFD54F),
    replyIcon = Color(0xFF84FFFF)
)

val MidnightBlueTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF0B1830),
    primary = Color(0xFF5AA9FF),
    onPrimary = Color.White,
    userBubble = Color(0xFF143152),
    botBubble = Color(0xFF0D223A),
    userText = Color(0xFFE3EEFF),
    botText = Color(0xFFCFE9FF),
    settingsIcon = Color(0xFFFFC857),
    replyIcon = Color(0xFF7CFFCB)
)

val ForestDeepTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF0D1F0F),
    primary = Color(0xFF4CD964),
    onPrimary = Color.Black,
    userBubble = Color(0xFF1C4020),
    botBubble = Color(0xFF123016),
    userText = Color(0xFFE4FFE7),
    botText = Color(0xFFC6F7CD),
    settingsIcon = Color(0xFFFFD166),
    replyIcon = Color(0xFF80DEEA)
)

val SlateGrayTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF1A2530),
    primary = Color(0xFF9FB3C8),
    onPrimary = Color.Black,
    userBubble = Color(0xFF2D3E4E),
    botBubble = Color(0xFF223241),
    userText = Color(0xFFEAF2FA),
    botText = Color(0xFFD3E3F4),
    settingsIcon = Color(0xFFFFB74D),
    replyIcon = Color(0xFF80CBC4)
)

val RoyalGoldTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF231D0E),
    primary = Color(0xFFFFD54F),
    onPrimary = Color.Black,
    userBubble = Color(0xFF3B3013),
    botBubble = Color(0xFF2E250F),
    userText = Color(0xFFFFF8D6),
    botText = Color(0xFFFFE9A8),
    settingsIcon = Color(0xFF90CAF9),
    replyIcon = Color(0xFFFF8A80)
)

val NeonVioletTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF180734),
    primary = Color(0xFFE040FB),
    onPrimary = Color.White,
    userBubble = Color(0xFF3A1360),
    botBubble = Color(0xFF2A0E4A),
    userText = Color(0xFFFFE6FF),
    botText = Color(0xFFE7D7FF),
    settingsIcon = Color(0xFFFFEA00),
    replyIcon = Color(0xFF40C4FF)
)

val OceanBreezeTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF0A192F),
    primary = Color(0xFF64FFDA),
    onPrimary = Color.Black,
    userBubble = Color(0xFF112240),
    botBubble = Color(0xFF0A192F),
    userText = Color(0xFFE6F1FF),
    botText = Color(0xFFCCD6F6),
    settingsIcon = Color(0xFF48B0F7),
    replyIcon = Color(0xFF8892B0)
)

val VolcanicAshTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF1A1A1A),
    primary = Color(0xFFFF3D00),
    onPrimary = Color.White,
    userBubble = Color(0xFF2D2D2D),
    botBubble = Color(0xFF1F1F1F),
    userText = Color(0xFFF5F5F5),
    botText = Color(0xFFE0E0E0),
    settingsIcon = Color(0xFFFF8A65),
    replyIcon = Color(0xFFFFB74D)
)

val MintChocolateTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF2C1E16),
    primary = Color(0xFF98FF98),
    onPrimary = Color.Black,
    userBubble = Color(0xFF4A3525),
    botBubble = Color(0xFF3B281D),
    userText = Color(0xFFE8F5E9),
    botText = Color(0xFFC8E6C9),
    settingsIcon = Color(0xFFA5D6A7),
    replyIcon = Color(0xFF81C784)
)

val ElectricIndigoTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF0F0033),
    primary = Color(0xFF6600FF),
    onPrimary = Color.White,
    userBubble = Color(0xFF260080),
    botBubble = Color(0xFF1A0059),
    userText = Color(0xFFE6E6FF),
    botText = Color(0xFFCCCCFF),
    settingsIcon = Color(0xFF00E5FF),
    replyIcon = Color(0xFFFF00FF)
)

val DesertSandTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF2B2013),
    primary = Color(0xFFE4A010),
    onPrimary = Color.Black,
    userBubble = Color(0xFF4A3B2C),
    botBubble = Color(0xFF382A1D),
    userText = Color(0xFFFFF3E0),
    botText = Color(0xFFFFE0B2),
    settingsIcon = Color(0xFFFFCC80),
    replyIcon = Color(0xFFFFAB40)
)

val CosmicNebulaTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF1A0B2E),
    primary = Color(0xFFFF007F),
    onPrimary = Color.White,
    userBubble = Color(0xFF31145A),
    botBubble = Color(0xFF240E42),
    userText = Color(0xFFF3E8FF),
    botText = Color(0xFFE9D5FF),
    settingsIcon = Color(0xFF00FFFF),
    replyIcon = Color(0xFFB026FF)
)

val AutumnLeavesTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF260E04),
    primary = Color(0xFFFF5722),
    onPrimary = Color.White,
    userBubble = Color(0xFF4A1A06),
    botBubble = Color(0xFF381304),
    userText = Color(0xFFFBE9E7),
    botText = Color(0xFFFFCCBC),
    settingsIcon = Color(0xFFFFCA28),
    replyIcon = Color(0xFF8D6E63)
)

val GlacierWhiteTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF0D1B2A),
    primary = Color(0xFFE0F7FA),
    onPrimary = Color.Black,
    userBubble = Color(0xFF1B263B),
    botBubble = Color(0xFF131C2D),
    userText = Color(0xFFFFFFFF),
    botText = Color(0xFFB0C4DE),
    settingsIcon = Color(0xFF4169E1),
    replyIcon = Color(0xFF87CEEB)
)

val AbyssalDepthsTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF000B18),
    primary = Color(0xFF00FFCC),
    onPrimary = Color.Black,
    userBubble = Color(0xFF001F3F),
    botBubble = Color(0xFF00152B),
    userText = Color(0xFFE0FFFF),
    botText = Color(0xFFB0E0E6),
    settingsIcon = Color(0xFF1E90FF),
    replyIcon = Color(0xFF00CED1)
)

val Retro80sTheme = ForgeIntColors(
    background = Color.Black,
    surface = Color(0xFF1F0024),
    primary = Color(0xFFFF00FF),
    onPrimary = Color.Black,
    userBubble = Color(0xFF3D004A),
    botBubble = Color(0xFF2B0033),
    userText = Color(0xFFFFFF00),
    botText = Color(0xFF00FFFF),
    settingsIcon = Color(0xFFFF0055),
    replyIcon = Color(0xFF39FF14)
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
        "Ocean Breeze" -> OceanBreezeTheme
        "Volcanic Ash" -> VolcanicAshTheme
        "Mint Chocolate" -> MintChocolateTheme
        "Electric Indigo" -> ElectricIndigoTheme
        "Desert Sand" -> DesertSandTheme
        "Cosmic Nebula" -> CosmicNebulaTheme
        "Autumn Leaves" -> AutumnLeavesTheme
        "Glacier White" -> GlacierWhiteTheme
        "Abyssal Depths" -> AbyssalDepthsTheme
        "Retro 80s" -> Retro80sTheme
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
