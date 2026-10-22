package io.github.rainboom7.cosmetics.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import io.github.rainboom7.cosmetics.core.model.Category
import io.github.rainboom7.cosmetics.core.model.Condition
import io.github.rainboom7.cosmetics.core.model.ShelfStatus

// Pastel palette from the design spec, v2 ("Пудра"): dusty rose primary, lavender secondary,
// mint tertiary on a warm off-white. Text roles keep at least 4.5:1 contrast on their containers.
private val LightColors = lightColorScheme(
    primary = Color(0xFF9C5B73),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDDE8),
    onPrimaryContainer = Color(0xFF3E1424),
    secondary = Color(0xFF6E5F80),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFECE5F6),
    onSecondaryContainer = Color(0xFF2C2238),
    tertiary = Color(0xFF4A7565),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDCF1E6),
    onTertiaryContainer = Color(0xFF0F2A20),
    background = Color(0xFFFFF8FA),
    onBackground = Color(0xFF2A2125),
    surface = Color(0xFFFFF8FA),
    onSurface = Color(0xFF2A2125),
    surfaceVariant = Color(0xFFF3E7EA),
    onSurfaceVariant = Color(0xFF6A5A60),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF1F5),
    surfaceContainer = Color(0xFFFDE9F0),
    surfaceContainerHigh = Color(0xFFF9E2EA),
    surfaceContainerHighest = Color(0xFFF4DBE4),
    outline = Color(0xFF9C8A90),
    outlineVariant = Color(0xFFF2D7E1),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFE1DE),
    onErrorContainer = Color(0xFF5C1410),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF4B6CB),
    onPrimary = Color(0xFF4E2133),
    primaryContainer = Color(0xFF5E3346),
    onPrimaryContainer = Color(0xFFFFD9E4),
    secondary = Color(0xFFD4C4E6),
    onSecondary = Color(0xFF3A2F48),
    secondaryContainer = Color(0xFF463B52),
    onSecondaryContainer = Color(0xFFECE5F6),
    tertiary = Color(0xFFA9D4C2),
    onTertiary = Color(0xFF173A2E),
    tertiaryContainer = Color(0xFF2F4D41),
    onTertiaryContainer = Color(0xFFCFEBDD),
    background = Color(0xFF1C1618),
    onBackground = Color(0xFFF0E3E6),
    surface = Color(0xFF1C1618),
    onSurface = Color(0xFFF0E3E6),
    surfaceVariant = Color(0xFF3A3034),
    onSurfaceVariant = Color(0xFFD3C3C8),
    surfaceContainerLowest = Color(0xFF161113),
    surfaceContainerLow = Color(0xFF211A1D),
    surfaceContainer = Color(0xFF261F22),
    surfaceContainerHigh = Color(0xFF30282B),
    surfaceContainerHighest = Color(0xFF3B3235),
    outline = Color(0xFF9E8C92),
    outlineVariant = Color(0xFF4E4246),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

// Rounder than stock M3: soft, "airy" cards and pill-shaped chips.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val AppTypography = Typography().let { base ->
    base.copy(
        // Tab titles: lighter and a bit tighter than stock, so headers feel calm.
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Normal, letterSpacing = (-0.25).sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Normal),
        // Brand line on cards: caps, letter-spaced.
        labelMedium = base.labelMedium.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp),
    )
}

/** Screen side padding and the gap between cards (design spec v2: 20/16, up from 16/12). */
object Spacing {
    val screen = 20.dp
    val gap = 16.dp
}

/**
 * The app always uses its own pastel palette. Dynamic color stays available ([dynamicColor]) but is
 * off by default: wallpaper colors are often saturated and would undo the soft look.
 */
@Composable
fun CosmeticsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, shapes = AppShapes, typography = AppTypography, content = content)
}

/** A pastel background + readable ink pair, used for placeholders and status chips. */
data class CategoryTone(val tone: Color, val ink: Color)

/** Photo placeholder tones per category (design spec). */
fun Category.tone(): CategoryTone = when (this) {
    Category.SKINCARE, Category.BODY -> CategoryTone(Color(0xFFE4F0F7), Color(0xFF3B6E86))
    Category.MAKEUP, Category.NAILS, Category.FRAGRANCE -> CategoryTone(Color(0xFFF9E6EE), Color(0xFF8E4F67))
    Category.HAIR -> CategoryTone(Color(0xFFE6F3E9), Color(0xFF41694B))
    Category.OTHER -> CategoryTone(Color(0xFFF3EEE7), Color(0xFF6B5B45))
}

/**
 * Status chip colors: mint "есть запас", peach "заканчивается", lavender "пробую",
 * stone "не использую", sky "отдам". Light pastels with dark ink work in both themes.
 */
fun ShelfStatus.tone(): CategoryTone = when (this) {
    ShelfStatus.STOCKED -> CategoryTone(Color(0xFFDCF1E6), Color(0xFF2F5E4B))
    ShelfStatus.RUNNING_LOW -> CategoryTone(Color(0xFFFFE4D2), Color(0xFF8A4B1F))
    ShelfStatus.TRYING -> CategoryTone(Color(0xFFECE5F6), Color(0xFF5B4A75))
    ShelfStatus.NOT_USING -> CategoryTone(Color(0xFFEEEAE6), Color(0xFF5E5650))
    ShelfStatus.GIVE_AWAY -> CategoryTone(Color(0xFFDFEEF7), Color(0xFF2F5F7A))
}

/** Dot color for usage marks in the calendar, matching the item's category. */
val Category.dot: Color get() = tone().ink

/** Chip colors for the single "Состояние" choice: lifecycle states plus the [ShelfStatus] tones. */
fun Condition.tone(): CategoryTone = when (this) {
    Condition.NEW -> CategoryTone(Color(0xFFFFE9F1), Color(0xFF8E3D5E))
    Condition.OPENED -> CategoryTone(Color(0xFFFFE0EA), Color(0xFF7E3552))
    Condition.TRYING -> ShelfStatus.TRYING.tone()
    Condition.STOCKED -> ShelfStatus.STOCKED.tone()
    Condition.RUNNING_LOW -> ShelfStatus.RUNNING_LOW.tone()
    Condition.NOT_USING -> ShelfStatus.NOT_USING.tone()
    Condition.GIVE_AWAY -> ShelfStatus.GIVE_AWAY.tone()
    Condition.FINISHED -> CategoryTone(Color(0xFFF1ECE4), Color(0xFF6B5B45))
}

/**
 * Candy pastels for the "tasty" accents (calendar header, stat tiles, usage heat): blush, peach,
 * lilac and mint. Light tones with dark ink, so they read in both themes.
 */
object Candy {
    val blush = Color(0xFFFFD6E5)
    val peach = Color(0xFFFFE3D3)
    val lilac = Color(0xFFE9DEFA)
    val mint = Color(0xFFD7F2E5)
    val butter = Color(0xFFFFF1C9)
    val ink = Color(0xFF3E1424)

    /** Usage heat for calendar days: 1, 2, 3+ items. */
    val heat = listOf(Color(0xFFFFE1EC), Color(0xFFFFC3D8), Color(0xFFF7A1C0))

    val header: Brush = Brush.linearGradient(listOf(blush, peach, lilac))
    val streak: Brush = Brush.linearGradient(listOf(blush, Color(0xFFFFC9DC)))
    val days: Brush = Brush.linearGradient(listOf(lilac, Color(0xFFDCD0F5)))
    val empties: Brush = Brush.linearGradient(listOf(mint, Color(0xFFC6EBDA)))
}
