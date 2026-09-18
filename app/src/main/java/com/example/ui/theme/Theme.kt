package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.example.model.AppThemePreset
import com.example.model.DynamicArtworkPalette

@Composable
fun MyApplicationTheme(
    themePreset: AppThemePreset = AppThemePreset.SYSTEM_DEFAULT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    isOledPureBlack: Boolean = false,
    isDynamicArtworkColorEnabled: Boolean = false,
    dynamicArtworkPalette: DynamicArtworkPalette? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isSystemPreset = themePreset == AppThemePreset.SYSTEM_DEFAULT

    val colorScheme = if (isSystemPreset && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) {
            if (isOledPureBlack) {
                dynamicDarkColorScheme(context).copy(
                    background = Color(0xFF000000),
                    surface = Color(0xFF050505),
                    surfaceVariant = Color(0xFF0F0F0F),
                    outline = Color(0xFF222222)
                )
            } else {
                dynamicDarkColorScheme(context)
            }
        } else {
            dynamicLightColorScheme(context)
        }
    } else {
        val isEffectiveLight = if (isSystemPreset) !darkTheme else themePreset.isLight

        val activePrimary = if (isDynamicArtworkColorEnabled && dynamicArtworkPalette?.isExtracted == true) {
            val extracted = dynamicArtworkPalette.getPrimaryAccent(themePreset.primaryColor)
            if (isEffectiveLight && extracted.luminance() > 0.65f) {
                if (themePreset.primaryColor.luminance() <= 0.65f) themePreset.primaryColor else Color(0xFF0F172A)
            } else {
                extracted
            }
        } else {
            themePreset.primaryColor
        }

        val activeSecondary = if (isDynamicArtworkColorEnabled && dynamicArtworkPalette?.isExtracted == true) {
            val extracted = dynamicArtworkPalette.getSecondaryAccent(themePreset.secondaryColor)
            if (isEffectiveLight && extracted.luminance() > 0.65f) {
                if (themePreset.secondaryColor.luminance() <= 0.65f) themePreset.secondaryColor else Color(0xFF334155)
            } else {
                extracted
            }
        } else {
            themePreset.secondaryColor
        }

        val onPrimaryComputed = if (activePrimary.luminance() > 0.55f) Color(0xFF000000) else Color(0xFFFFFFFF)
        val onSecondaryComputed = if (activeSecondary.luminance() > 0.55f) Color(0xFF000000) else Color(0xFFFFFFFF)

        if (isEffectiveLight) {
            lightColorScheme(
                primary = activePrimary,
                onPrimary = onPrimaryComputed,
                primaryContainer = themePreset.containerColor,
                onPrimaryContainer = activePrimary,
                secondary = activeSecondary,
                onSecondary = onSecondaryComputed,
                secondaryContainer = themePreset.borderColor,
                onSecondaryContainer = activePrimary,
                tertiary = activeSecondary,
                background = themePreset.backgroundColor,
                onBackground = themePreset.onBackgroundColor,
                surface = themePreset.surfaceColor,
                onSurface = themePreset.onSurfaceColor,
                surfaceVariant = themePreset.containerColor,
                onSurfaceVariant = themePreset.onSurfaceVariantColor,
                outline = themePreset.borderColor,
                outlineVariant = themePreset.borderColor.copy(alpha = 0.5f)
            )
        } else {
            val oledBackground = Color(0xFF000000)
            val oledSurface = Color(0xFF050505)
            val oledContainer = Color(0xFF0F0F0F)
            val oledBorder = Color(0xFF222222)

            val bg = if (isOledPureBlack) oledBackground else themePreset.backgroundColor
            val surf = if (isOledPureBlack) oledSurface else themePreset.surfaceColor
            val container = if (isOledPureBlack) oledContainer else themePreset.containerColor
            val border = if (isOledPureBlack) oledBorder else themePreset.borderColor

            darkColorScheme(
                primary = activePrimary,
                onPrimary = onPrimaryComputed,
                primaryContainer = container,
                onPrimaryContainer = activeSecondary,
                secondary = activeSecondary,
                onSecondary = onSecondaryComputed,
                secondaryContainer = border,
                onSecondaryContainer = activePrimary,
                tertiary = activeSecondary,
                background = bg,
                onBackground = if (isOledPureBlack) Color(0xFFFFFFFF) else themePreset.onBackgroundColor,
                surface = surf,
                onSurface = if (isOledPureBlack) Color(0xFFF0F0F0) else themePreset.onSurfaceColor,
                surfaceVariant = container,
                onSurfaceVariant = if (isOledPureBlack) Color(0xFFCCCCCC) else themePreset.onSurfaceVariantColor,
                outline = border,
                outlineVariant = border.copy(alpha = 0.5f)
            )
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
