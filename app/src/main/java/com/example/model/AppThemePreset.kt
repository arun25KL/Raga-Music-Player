package com.example.model

import androidx.compose.ui.graphics.Color

enum class ThemeCategory(val title: String) {
    LUXURY_AND_STUDIO("Luxury & Studio"),
    DARK_AND_OLED("Dark & OLED"),
    MONOCHROME_AND_LIGHT("Monochrome & Light"),
    SINGLE_COLOR("Single Colors")
}

enum class AppThemePreset(
    val title: String,
    val category: ThemeCategory,
    val primaryColor: Color,
    val secondaryColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val containerColor: Color,
    val borderColor: Color,
    val isLight: Boolean = false,
    val onBackgroundColor: Color = if (isLight) Color(0xFF111827) else Color(0xFFFFFFFF),
    val onSurfaceColor: Color = if (isLight) Color(0xFF111827) else Color(0xFFFFFFFF),
    val onSurfaceVariantColor: Color = if (isLight) Color(0xFF4B5563) else Color(0xFFD4E0EB)
) {
    // --- Luxury & Studio ---
    SYSTEM_DEFAULT(
        title = "System Default",
        category = ThemeCategory.LUXURY_AND_STUDIO,
        primaryColor = Color(0xFF60A5FA),
        secondaryColor = Color(0xFF38BDF8),
        backgroundColor = Color(0xFF0F172A),
        surfaceColor = Color(0xFF1E293B),
        containerColor = Color(0xFF334155),
        borderColor = Color(0xFF475569),
        isLight = false
    ),
    CHAMPAGNE_LUXE(
        title = "Champagne Luxe",
        category = ThemeCategory.LUXURY_AND_STUDIO,
        primaryColor = Color(0xFFF3E5AB),
        secondaryColor = Color(0xFFE0A96D),
        backgroundColor = Color(0xFF090A0C),
        surfaceColor = Color(0xFF141519),
        containerColor = Color(0xFF1E2026),
        borderColor = Color(0xFF3F3B32),
        isLight = false
    ),
    CYBERPUNK_SYNTHWAVE(
        title = "Cyberpunk Synth",
        category = ThemeCategory.LUXURY_AND_STUDIO,
        primaryColor = Color(0xFFD946EF),
        secondaryColor = Color(0xFF06B6D4),
        backgroundColor = Color(0xFF090611),
        surfaceColor = Color(0xFF130D23),
        containerColor = Color(0xFF1F1538),
        borderColor = Color(0xFF4C1D95),
        isLight = false
    ),
    NORDIC_AURORA(
        title = "Nordic Aurora",
        category = ThemeCategory.LUXURY_AND_STUDIO,
        primaryColor = Color(0xFF34D399),
        secondaryColor = Color(0xFF38BDF8),
        backgroundColor = Color(0xFF050F14),
        surfaceColor = Color(0xFF0C1B24),
        containerColor = Color(0xFF142B39),
        borderColor = Color(0xFF1E455B),
        isLight = false
    ),
    MOCHA_ESPRESSO(
        title = "Mocha Espresso",
        category = ThemeCategory.LUXURY_AND_STUDIO,
        primaryColor = Color(0xFFF59E0B),
        secondaryColor = Color(0xFFFB923C),
        backgroundColor = Color(0xFF120B08),
        surfaceColor = Color(0xFF1C130E),
        containerColor = Color(0xFF2B1D16),
        borderColor = Color(0xFF4D3428),
        isLight = false
    ),
    PHANTOM_QUARTZ(
        title = "Phantom Quartz",
        category = ThemeCategory.LUXURY_AND_STUDIO,
        primaryColor = Color(0xFFF8FAFC),
        secondaryColor = Color(0xFF94A3B8),
        backgroundColor = Color(0xFF08090C),
        surfaceColor = Color(0xFF101217),
        containerColor = Color(0xFF191D24),
        borderColor = Color(0xFF2E3542),
        isLight = false
    ),
    TOKYO_MIDNIGHT(
        title = "Tokyo Midnight",
        category = ThemeCategory.LUXURY_AND_STUDIO,
        primaryColor = Color(0xFFF43F5E),
        secondaryColor = Color(0xFFA855F7),
        backgroundColor = Color(0xFF0A0710),
        surfaceColor = Color(0xFF140F20),
        containerColor = Color(0xFF201833),
        borderColor = Color(0xFF3B2B5C),
        isLight = false
    ),

    // --- Monochrome & Light ---
    SIMPLE_WHITE_AND_BLACK(
        title = "White & Black",
        category = ThemeCategory.MONOCHROME_AND_LIGHT,
        primaryColor = Color(0xFF000000),
        secondaryColor = Color(0xFF262626),
        backgroundColor = Color(0xFFFFFFFF),
        surfaceColor = Color(0xFFFAFAFA),
        containerColor = Color(0xFFF0F0F0),
        borderColor = Color(0xFFE0E0E0),
        isLight = true
    ),
    WHITE_AND_BLUE(
        title = "White & Blue",
        category = ThemeCategory.MONOCHROME_AND_LIGHT,
        primaryColor = Color(0xFF1D4ED8),
        secondaryColor = Color(0xFF3B82F6),
        backgroundColor = Color(0xFFFFFFFF),
        surfaceColor = Color(0xFFF8FAFC),
        containerColor = Color(0xFFEFF6FF),
        borderColor = Color(0xFFDBEAFE),
        isLight = true
    ),
    OCEAN_WHITE_AND_BLUE(
        title = "White & Ocean Blue",
        category = ThemeCategory.MONOCHROME_AND_LIGHT,
        primaryColor = Color(0xFF0284C7),
        secondaryColor = Color(0xFF0EA5E9),
        backgroundColor = Color(0xFFFAFDFF),
        surfaceColor = Color(0xFFFFFFFF),
        containerColor = Color(0xFFE0F2FE),
        borderColor = Color(0xFFBAE6FD),
        isLight = true
    ),
    BLACK_AND_WHITE(
        title = "Black & White (Dark)",
        category = ThemeCategory.MONOCHROME_AND_LIGHT,
        primaryColor = Color(0xFFFFFFFF),
        secondaryColor = Color(0xFFD0D0D0),
        backgroundColor = Color(0xFF000000),
        surfaceColor = Color(0xFF141414),
        containerColor = Color(0xFF222222),
        borderColor = Color(0xFF383838),
        isLight = false
    ),
    MONOCHROME_WHITE(
        title = "Minimal Monochrome",
        category = ThemeCategory.MONOCHROME_AND_LIGHT,
        primaryColor = Color(0xFF000000),
        secondaryColor = Color(0xFF424242),
        backgroundColor = Color(0xFFFFFFFF),
        surfaceColor = Color(0xFFF5F5F7),
        containerColor = Color(0xFFE8E8ED),
        borderColor = Color(0xFFD0D0D8),
        isLight = true
    ),
    STUDIO_WHITE(
        title = "Studio White",
        category = ThemeCategory.MONOCHROME_AND_LIGHT,
        primaryColor = Color(0xFF0F172A),
        secondaryColor = Color(0xFF0284C7),
        backgroundColor = Color(0xFFF8FAFC),
        surfaceColor = Color(0xFFFFFFFF),
        containerColor = Color(0xFFF1F5F9),
        borderColor = Color(0xFFE2E8F0),
        isLight = true
    ),
    WARM_IVORY_WHITE(
        title = "Warm Ivory",
        category = ThemeCategory.MONOCHROME_AND_LIGHT,
        primaryColor = Color(0xFF78350F),
        secondaryColor = Color(0xFFD97706),
        backgroundColor = Color(0xFFFAF8F5),
        surfaceColor = Color(0xFFFFFFFF),
        containerColor = Color(0xFFF4EEE6),
        borderColor = Color(0xFFE5DDD2),
        isLight = true
    ),
    PEARL_MINIMAL_WHITE(
        title = "Pearl White",
        category = ThemeCategory.MONOCHROME_AND_LIGHT,
        primaryColor = Color(0xFF1E293B),
        secondaryColor = Color(0xFF6366F1),
        backgroundColor = Color(0xFFFFFFFF),
        surfaceColor = Color(0xFFF8FAFC),
        containerColor = Color(0xFFEEF2F6),
        borderColor = Color(0xFFE2E8F0),
        isLight = true
    ),
    NORDIC_FROST_WHITE(
        title = "Nordic Frost",
        category = ThemeCategory.MONOCHROME_AND_LIGHT,
        primaryColor = Color(0xFF0284C7),
        secondaryColor = Color(0xFF06B6D4),
        backgroundColor = Color(0xFFF0F9FF),
        surfaceColor = Color(0xFFFFFFFF),
        containerColor = Color(0xFFE0F2FE),
        borderColor = Color(0xFFBAE6FD),
        isLight = true
    ),
    MATCHA_CREAM_WHITE(
        title = "Matcha Cream",
        category = ThemeCategory.MONOCHROME_AND_LIGHT,
        primaryColor = Color(0xFF2E7D32),
        secondaryColor = Color(0xFF558B2F),
        backgroundColor = Color(0xFFF7FAF7),
        surfaceColor = Color(0xFFFFFFFF),
        containerColor = Color(0xFFE8F5E9),
        borderColor = Color(0xFFC8E6C9),
        isLight = true
    ),
    SAKURA_BLOSSOM_WHITE(
        title = "Sakura Rose",
        category = ThemeCategory.MONOCHROME_AND_LIGHT,
        primaryColor = Color(0xFFBE185D),
        secondaryColor = Color(0xFFDB2777),
        backgroundColor = Color(0xFFFDF8F9),
        surfaceColor = Color(0xFFFFFFFF),
        containerColor = Color(0xFFFCE7F3),
        borderColor = Color(0xFFFBCFE8),
        isLight = true
    ),

    // --- Dark & OLED ---
    OLED_BLACK(
        title = "Pure OLED Black",
        category = ThemeCategory.DARK_AND_OLED,
        primaryColor = Color(0xFFFF9800),
        secondaryColor = Color(0xFFFFD54F),
        backgroundColor = Color(0xFF000000),
        surfaceColor = Color(0xFF0E0E0E),
        containerColor = Color(0xFF181818),
        borderColor = Color(0xFF2B2B2B),
        isLight = false
    ),
    MIDNIGHT_DARK(
        title = "Midnight Dark",
        category = ThemeCategory.DARK_AND_OLED,
        primaryColor = Color(0xFF38BDF8),
        secondaryColor = Color(0xFF93C5FD),
        backgroundColor = Color(0xFF070A10),
        surfaceColor = Color(0xFF0F1420),
        containerColor = Color(0xFF182030),
        borderColor = Color(0xFF27344E),
        isLight = false
    ),
    TITANIUM_SLATE(
        title = "Titanium Slate",
        category = ThemeCategory.DARK_AND_OLED,
        primaryColor = Color(0xFFE2E8F0),
        secondaryColor = Color(0xFF94A3B8),
        backgroundColor = Color(0xFF0F1216),
        surfaceColor = Color(0xFF171B21),
        containerColor = Color(0xFF222730),
        borderColor = Color(0xFF363E4D),
        isLight = false
    ),

    // --- Single Colors ---
    AMBER_GOLD(
        title = "Amber Gold",
        category = ThemeCategory.SINGLE_COLOR,
        primaryColor = Color(0xFFFF9800),
        secondaryColor = Color(0xFFFFD54F),
        backgroundColor = Color(0xFF121110),
        surfaceColor = Color(0xFF1C1816),
        containerColor = Color(0xFF28221D),
        borderColor = Color(0xFF45362B),
        isLight = false
    ),
    ELECTRIC_YELLOW(
        title = "Neon Yellow",
        category = ThemeCategory.SINGLE_COLOR,
        primaryColor = Color(0xFFFFEE55),
        secondaryColor = Color(0xFFFFF59D),
        backgroundColor = Color(0xFF141408),
        surfaceColor = Color(0xFF20200D),
        containerColor = Color(0xFF2C2C13),
        borderColor = Color(0xFF484822),
        isLight = false
    ),
    LIME_GREEN(
        title = "Lime Green",
        category = ThemeCategory.SINGLE_COLOR,
        primaryColor = Color(0xFFAEEA00),
        secondaryColor = Color(0xFFC6FF00),
        backgroundColor = Color(0xFF0E1606),
        surfaceColor = Color(0xFF16230B),
        containerColor = Color(0xFF203112),
        borderColor = Color(0xFF365220),
        isLight = false
    ),
    EMERALD_GREEN(
        title = "Emerald Green",
        category = ThemeCategory.SINGLE_COLOR,
        primaryColor = Color(0xFF00FF88),
        secondaryColor = Color(0xFF69F0AE),
        backgroundColor = Color(0xFF0C1813),
        surfaceColor = Color(0xFF12241C),
        containerColor = Color(0xFF1A3328),
        borderColor = Color(0xFF2A5240),
        isLight = false
    ),
    OCEAN_CYAN(
        title = "Electric Cyan",
        category = ThemeCategory.SINGLE_COLOR,
        primaryColor = Color(0xFF00F5FF),
        secondaryColor = Color(0xFF80D8FF),
        backgroundColor = Color(0xFF08161C),
        surfaceColor = Color(0xFF0E2029),
        containerColor = Color(0xFF152F3C),
        borderColor = Color(0xFF21495E),
        isLight = false
    ),
    COBALT_BLUE(
        title = "Cobalt Blue",
        category = ThemeCategory.SINGLE_COLOR,
        primaryColor = Color(0xFF2979FF),
        secondaryColor = Color(0xFF82B1FF),
        backgroundColor = Color(0xFF080E1C),
        surfaceColor = Color(0xFF0E172B),
        containerColor = Color(0xFF16223D),
        borderColor = Color(0xFF253761),
        isLight = false
    ),
    ROYAL_AMETHYST(
        title = "Royal Purple",
        category = ThemeCategory.SINGLE_COLOR,
        primaryColor = Color(0xFFD8B4FE),
        secondaryColor = Color(0xFFC084FC),
        backgroundColor = Color(0xFF130E1A),
        surfaceColor = Color(0xFF1E1728),
        containerColor = Color(0xFF2A1F38),
        borderColor = Color(0xFF44305B),
        isLight = false
    ),
    NEON_MAGENTA(
        title = "Neon Pink",
        category = ThemeCategory.SINGLE_COLOR,
        primaryColor = Color(0xFFFF4081),
        secondaryColor = Color(0xFFFF80AB),
        backgroundColor = Color(0xFF1A0813),
        surfaceColor = Color(0xFF280E1E),
        containerColor = Color(0xFF3A142C),
        borderColor = Color(0xFF5A2045),
        isLight = false
    ),
    ROSE_CORAL(
        title = "Rose Coral",
        category = ThemeCategory.SINGLE_COLOR,
        primaryColor = Color(0xFFFF6E6E),
        secondaryColor = Color(0xFFFFAB91),
        backgroundColor = Color(0xFF180C10),
        surfaceColor = Color(0xFF241318),
        containerColor = Color(0xFF331C23),
        borderColor = Color(0xFF512E39),
        isLight = false
    )
}
