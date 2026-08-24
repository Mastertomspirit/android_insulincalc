package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppTheme(val displayName: String, val previewColor: Color) {
    MEDICAL_TEAL("Medizinisch Türkis", Color(0xFF006874)),
    OCEAN_BLUE("Ozean Blau", Color(0xFF0061A4)),
    EMERALD_GREEN("Smaragd Grün", Color(0xFF1B6C43)),
    SUNSET_AMBER("Sonnenuntergang", Color(0xFFB34A00)),
    BERRY_VIOLET("Beere & Violett", Color(0xFF834C77)),
    MIDNIGHT_DARK("Mitternacht AMOLED", Color(0xFF9965F4))
}

// 1. Teal Theme
private val MedicalTealDarkColorScheme = darkColorScheme(
    primary = MedicalTealPrimaryDark,
    onPrimary = MedicalTealOnPrimaryDark,
    primaryContainer = MedicalTealPrimaryContainerDark,
    onPrimaryContainer = MedicalTealOnPrimaryContainerDark,
    secondary = MedicalSecondaryDark,
    onSecondary = MedicalOnSecondaryDark,
    secondaryContainer = MedicalSecondaryContainerDark,
    onSecondaryContainer = MedicalOnSecondaryContainerDark,
    tertiary = MedicalTertiaryDark,
    onTertiary = MedicalOnTertiaryDark,
    tertiaryContainer = MedicalTertiaryContainerDark,
    onTertiaryContainer = MedicalOnTertiaryContainerDark,
    background = MedicalBackgroundDark,
    onBackground = MedicalOnBackgroundDark,
    surface = MedicalSurfaceDark,
    onSurface = MedicalOnSurfaceDark,
    surfaceVariant = MedicalSurfaceVariantDark,
    onSurfaceVariant = MedicalOnSurfaceVariantDark,
)

private val MedicalTealLightColorScheme = lightColorScheme(
    primary = MedicalTealPrimary,
    onPrimary = MedicalTealOnPrimary,
    primaryContainer = MedicalTealPrimaryContainer,
    onPrimaryContainer = MedicalTealOnPrimaryContainer,
    secondary = MedicalSecondary,
    onSecondary = MedicalOnSecondary,
    secondaryContainer = MedicalSecondaryContainer,
    onSecondaryContainer = MedicalOnSecondaryContainer,
    tertiary = MedicalTertiary,
    onTertiary = MedicalOnTertiary,
    tertiaryContainer = MedicalTertiaryContainer,
    onTertiaryContainer = MedicalOnTertiaryContainer,
    background = MedicalBackgroundLight,
    onBackground = MedicalOnBackgroundLight,
    surface = MedicalSurfaceLight,
    onSurface = MedicalOnSurfaceLight,
    surfaceVariant = MedicalSurfaceVariantLight,
    onSurfaceVariant = MedicalOnSurfaceVariantLight,
)

// 2. Ocean Blue
private val OceanBlueLightColorScheme = lightColorScheme(
    primary = OceanPrimary,
    onPrimary = Color.White,
    primaryContainer = OceanPrimaryContainer,
    onPrimaryContainer = OceanOnPrimaryContainer,
    secondary = Color(0xFF535F70),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7E3F8),
    onSecondaryContainer = Color(0xFF101C2B),
    background = Color(0xFFFDFCFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFDFCFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E)
)

private val OceanBlueDarkColorScheme = darkColorScheme(
    primary = OceanPrimaryDark,
    onPrimary = Color(0xFF00325A),
    primaryContainer = OceanPrimaryContainerDark,
    onPrimaryContainer = OceanPrimaryContainer,
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD7E3F8),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7D0)
)

// 3. Emerald Green
private val EmeraldLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = Color(0xFF4F6353),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2E8D4),
    onSecondaryContainer = Color(0xFF0D1F13),
    background = Color(0xFFFCFDF6),
    onBackground = Color(0xFF1A1C19),
    surface = Color(0xFFFCFDF6),
    onSurface = Color(0xFF1A1C19),
    surfaceVariant = Color(0xFFDDE5DB),
    onSurfaceVariant = Color(0xFF414942)
)

private val EmeraldDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = Color(0xFF00391D),
    primaryContainer = EmeraldPrimaryContainerDark,
    onPrimaryContainer = EmeraldPrimaryContainer,
    secondary = Color(0xFFB6CCB8),
    onSecondary = Color(0xFF223527),
    secondaryContainer = Color(0xFF384B3C),
    onSecondaryContainer = Color(0xFFD2E8D4),
    background = Color(0xFF1A1C19),
    onBackground = Color(0xFFE2E3DD),
    surface = Color(0xFF1A1C19),
    onSurface = Color(0xFFE2E3DD),
    surfaceVariant = Color(0xFF414942),
    onSurfaceVariant = Color(0xFFC1C9BF)
)

// 4. Sunset Amber
private val SunsetLightColorScheme = lightColorScheme(
    primary = SunsetPrimary,
    onPrimary = Color.White,
    primaryContainer = SunsetPrimaryContainer,
    onPrimaryContainer = SunsetOnPrimaryContainer,
    secondary = Color(0xFF77574B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBCE),
    onSecondaryContainer = Color(0xFF2C160D),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF231A16),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF231A16),
    surfaceVariant = Color(0xFFF5DED6),
    onSurfaceVariant = Color(0xFF53433E)
)

private val SunsetDarkColorScheme = darkColorScheme(
    primary = SunsetPrimaryDark,
    onPrimary = Color(0xFF602100),
    primaryContainer = SunsetPrimaryContainerDark,
    onPrimaryContainer = SunsetPrimaryContainer,
    secondary = Color(0xFFE7BEAF),
    onSecondary = Color(0xFF442A20),
    secondaryContainer = Color(0xFF5D4035),
    onSecondaryContainer = Color(0xFFFFDBCE),
    background = Color(0xFF201A18),
    onBackground = Color(0xFFEDE0DC),
    surface = Color(0xFF201A18),
    onSurface = Color(0xFFEDE0DC),
    surfaceVariant = Color(0xFF53433E),
    onSurfaceVariant = Color(0xFFD8C2BB)
)

// 5. Berry Violet
private val BerryLightColorScheme = lightColorScheme(
    primary = BerryPrimary,
    onPrimary = Color.White,
    primaryContainer = BerryPrimaryContainer,
    onPrimaryContainer = BerryOnPrimaryContainer,
    secondary = Color(0xFF705767),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFADAEB),
    onSecondaryContainer = Color(0xFF281623),
    background = Color(0xFFFFF7F9),
    onBackground = Color(0xFF201A1E),
    surface = Color(0xFFFFF7F9),
    onSurface = Color(0xFF201A1E),
    surfaceVariant = Color(0xFFEEDEE6),
    onSurfaceVariant = Color(0xFF4E444B)
)

private val BerryDarkColorScheme = darkColorScheme(
    primary = BerryPrimaryDark,
    onPrimary = Color(0xFF4E1F47),
    primaryContainer = BerryPrimaryContainerDark,
    onPrimaryContainer = BerryPrimaryContainer,
    secondary = Color(0xFFDCBED0),
    onSecondary = Color(0xFF3E2A38),
    secondaryContainer = Color(0xFF564050),
    onSecondaryContainer = Color(0xFFFADAEB),
    background = Color(0xFF1E1A1D),
    onBackground = Color(0xFFEAE0E4),
    surface = Color(0xFF1E1A1D),
    onSurface = Color(0xFFEAE0E4),
    surfaceVariant = Color(0xFF4E444B),
    onSurfaceVariant = Color(0xFFD2C3CC)
)

// 6. Midnight AMOLED
private val MidnightColorScheme = darkColorScheme(
    primary = MidnightPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = MidnightPrimaryContainerDark,
    onPrimaryContainer = Color(0xFFE9DDFF),
    secondary = Color(0xFFCBC2DB),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    background = MidnightBackground,
    onBackground = Color(0xFFF5EFF7),
    surface = MidnightSurface,
    onSurface = Color(0xFFF5EFF7),
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = Color(0xFFCAC4D0)
)

@Composable
fun MyApplicationTheme(
    selectedTheme: AppTheme = AppTheme.MEDICAL_TEAL,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme: ColorScheme = when {
        selectedTheme == AppTheme.MIDNIGHT_DARK -> MidnightColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> when (selectedTheme) {
            AppTheme.MEDICAL_TEAL -> if (darkTheme) MedicalTealDarkColorScheme else MedicalTealLightColorScheme
            AppTheme.OCEAN_BLUE -> if (darkTheme) OceanBlueDarkColorScheme else OceanBlueLightColorScheme
            AppTheme.EMERALD_GREEN -> if (darkTheme) EmeraldDarkColorScheme else EmeraldLightColorScheme
            AppTheme.SUNSET_AMBER -> if (darkTheme) SunsetDarkColorScheme else SunsetLightColorScheme
            AppTheme.BERRY_VIOLET -> if (darkTheme) BerryDarkColorScheme else BerryLightColorScheme
            AppTheme.MIDNIGHT_DARK -> MidnightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

