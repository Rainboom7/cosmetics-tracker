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

// Palette v3 «Облако»: a warm raspberry-pink primary (white text keeps 4.7:1) on cloud-white
// surfaces with a blush tint, after the Podruzhka app's white-and-pink look. Lilac and mint stay as
// quiet accents; text roles keep at least 4.5:1 contrast on their containers.
private val LightColors = lightColorScheme(
    primary = Color(0xFFCC3D72),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDCE7),
    onPrimaryContainer = Color(0xFF3E1424),
    secondary = Color(0xFF7A5A8C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF1E6F8),
    onSecondaryContainer = Color(0xFF2E1F3A),
    tertiary = Color(0xFF3F7A66),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDDF3E8),
    onTertiaryContainer = Color(0xFF0F2A20),
    background = Color(0xFFFFF9FB),
    onBackground = Color(0xFF2E1F25),
    surface = Color(0xFFFFF9FB),
    onSurface = Color(0xFF2E1F25),
    surfaceVariant = Color(0xFFF7E8ED),
    onSurfaceVariant = Color(0xFF7A5D68),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF2F6),
    surfaceContainer = Color(0xFFFFEAF1),
    surfaceContainerHigh = Color(0xFFFFE2EC),
    surfaceContainerHighest = Color(0xFFFBD9E5),
    outline = Color(0xFFA08891),
    outlineVariant = Color(0xFFF6D9E3),
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

// Rounder than stock M3: soft, "airy" cards and pill-shaped chips and buttons.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val AppTypography = Typography().let { base ->
    base.copy(
        // Shop-like hierarchy: bold, tight headings; titles semi-bold; brand line in letter-spaced caps.
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.25).sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Medium),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
    )
}

/** Screen side padding and the gap between cards (design spec v3: 16/12, shop-grid density). */
object Spacing {
    val screen = 16.dp
    val gap = 12.dp
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
    val sky = Color(0xFFDDEEFB)
    val ink = Color(0xFF3E1424)

    /** Usage heat for calendar days: 1, 2, 3+ items. */
    val heat = listOf(Color(0xFFFFE1EC), Color(0xFFFFC3D8), Color(0xFFF7A1C0))

    val header: Brush = Brush.linearGradient(listOf(blush, peach, lilac))
    val streak: Brush = Brush.linearGradient(listOf(blush, Color(0xFFFFC9DC)))
    val days: Brush = Brush.linearGradient(listOf(lilac, Color(0xFFDCD0F5)))
    val empties: Brush = Brush.linearGradient(listOf(mint, Color(0xFFC6EBDA)))
}

/**
 * The "cloud sky" behind screen headers (design spec v3): warm pink at the top fading into peach,
 * with white cloud puffs and a scalloped cloud edge that melts into the page.
 */
object Sky {
    val top = Color(0xFFFFD3E2)
    val middle = Color(0xFFFFE3E6)
    val bottom = Color(0xFFFFEEE8)
    val brush: Brush = Brush.verticalGradient(listOf(top, middle, bottom))

    /** Pink → raspberry for story rings and the primary call to action accents. */
    val ring: Brush = Brush.sweepGradient(listOf(Color(0xFFFF9EC0), Color(0xFFCC3D72), Color(0xFFFFB38A), Color(0xFFFF9EC0)))
    val ringMuted = Color(0xFFF2D7E1)

    /** Banner gradients: blush, peach-butter, lilac-sky. */
    val bannerPink: Brush = Brush.linearGradient(listOf(Color(0xFFFFC7DA), Color(0xFFFFE1D2)))
    val bannerPeach: Brush = Brush.linearGradient(listOf(Color(0xFFFFE0CC), Color(0xFFFFF1C9)))
    val bannerLilac: Brush = Brush.linearGradient(listOf(Color(0xFFE9DEFA), Color(0xFFDDEEFB)))
}
