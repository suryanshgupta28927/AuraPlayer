package com.aura.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

private val ColorOnAccent = Color.White

private val AuraDarkScheme = darkColorScheme(
    primary = AuraAccent,
    secondary = AuraAccent2,
    tertiary = AuraAccent3,
    background = AuraBg,
    surface = AuraSurface,
    surfaceVariant = AuraCard,
    onBackground = AuraText,
    onSurface = AuraText,
    onPrimary = ColorOnAccent
)

val AuraTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, color = AuraText),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = AuraText),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = AuraText),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, color = AuraText),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, color = AuraMuted),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, color = AuraMuted)
)

@Composable
fun AuraPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AuraDarkScheme,
        typography = AuraTypography,
        content = content
    )
}
