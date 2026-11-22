package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

@Immutable
data class ScoutSemanticColors(
    val verified: Color,
    val verifiedContainer: Color,
    val staleWarning: Color,
    val staleWarningContainer: Color,
    val alert: Color,
    val alertContainer: Color,
    val freeCost: Color,
    val freeCostContainer: Color,
    val aiAccent: Color,
    val aiAccentContainer: Color,
    val hairlineBorder: Color
)

val LocalScoutColors = staticCompositionLocalOf {
    ScoutSemanticColors(
        verified = SignalTeal,
        verifiedContainer = SignalTealDeep,
        staleWarning = TelemetryAmber,
        staleWarningContainer = TelemetryAmberDeep,
        alert = CoralAlert,
        alertContainer = CoralAlertDeep,
        freeCost = EmeraldFree,
        freeCostContainer = EmeraldFreeDeep,
        aiAccent = GeminiViolet,
        aiAccentContainer = GeminiVioletDeep,
        hairlineBorder = SlateBorder
    )
}

private val DexScoutDarkScheme = darkColorScheme(
    primary = SignalTeal,
    onPrimary = ObsidianBase,
    primaryContainer = SignalTealDeep,
    onPrimaryContainer = SignalTealBright,
    secondary = TelemetryAmber,
    onSecondary = ObsidianBase,
    secondaryContainer = TelemetryAmberDeep,
    onSecondaryContainer = TelemetryAmberLight,
    tertiary = GeminiViolet,
    onTertiary = ObsidianBase,
    tertiaryContainer = GeminiVioletDeep,
    onTertiaryContainer = Color(0xFFDDD6FE),
    background = ObsidianBase,
    onBackground = InkPrimary,
    surface = SlateSurface,
    onSurface = InkPrimary,
    surfaceVariant = SlateContainer,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLowest = ObsidianBase,
    surfaceContainerLow = SlateSurface,
    surfaceContainer = SlateContainer,
    surfaceContainerHigh = SlateContainerHigh,
    surfaceContainerHighest = Color(0xFF263447),
    outline = SlateBorder,
    outlineVariant = Color(0xFF1D2836),
    error = CoralAlert,
    onError = ObsidianBase,
    errorContainer = CoralAlertDeep,
    onErrorContainer = Color(0xFFFECACA)
)

private val DexScoutLightScheme = lightColorScheme(
    primary = SignalTealDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF134E4A),
    secondary = TelemetryAmberDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = GeminiVioletDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF4C1D95),
    background = PaperBase,
    onBackground = InkDarkPrimary,
    surface = PaperSurface,
    onSurface = InkDarkPrimary,
    surfaceVariant = PaperContainer,
    onSurfaceVariant = InkDarkSecondary,
    outline = PaperBorder,
    outlineVariant = Color(0xFFE2E8F0),
    error = CoralAlertDark,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B)
)

private val DexScoutShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

@Composable
fun DexScoutTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DexScoutDarkScheme else DexScoutLightScheme
    val semanticColors = if (darkTheme) {
        ScoutSemanticColors(
            verified = SignalTeal,
            verifiedContainer = SignalTealDeep,
            staleWarning = TelemetryAmber,
            staleWarningContainer = TelemetryAmberDeep,
            alert = CoralAlert,
            alertContainer = CoralAlertDeep,
            freeCost = EmeraldFree,
            freeCostContainer = EmeraldFreeDeep,
            aiAccent = GeminiViolet,
            aiAccentContainer = GeminiVioletDeep,
            hairlineBorder = SlateBorder
        )
    } else {
        ScoutSemanticColors(
            verified = SignalTealDark,
            verifiedContainer = Color(0xFFCCFBF1),
            staleWarning = TelemetryAmberDark,
            staleWarningContainer = Color(0xFFFEF3C7),
            alert = CoralAlertDark,
            alertContainer = Color(0xFFFEE2E2),
            freeCost = Color(0xFF047857),
            freeCostContainer = Color(0xFFD1FAE5),
            aiAccent = GeminiVioletDark,
            aiAccentContainer = Color(0xFFEDE9FE),
            hairlineBorder = PaperBorder
        )
    }

    CompositionLocalProvider(LocalScoutColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DexScoutTypography,
            shapes = DexScoutShapes,
            content = content
        )
    }
}
