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
import io.github.rainboom7.cosmetics.core.model.Category

// Fallback palette from the design spec (seed #8C4A60), used below Android 12 or without dynamic color.
private val LightColors = lightColorScheme(
    primary = Color(0xFF8C4A60),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF3A071D),
    tertiaryContainer = Color(0xFFFFDCC1),
    onTertiaryContainer = Color(0xFF2E1500),
    background = Color(0xFFFFF8F8),
    surface = Color(0xFFFFF8F8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFAEAED),
    surfaceContainerHigh = Color(0xFFF4E4E7),
    surfaceContainerHighest = Color(0xFFEEDEE1),
    onSurface = Color(0xFF22191C),
    onSurfaceVariant = Color(0xFF514347),
    outline = Color(0xFF837377),
    outlineVariant = Color(0xFFD5C2C6),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB1C8),
    onPrimary = Color(0xFF541D32),
    primaryContainer = Color(0xFF703348),
    onPrimaryContainer = Color(0xFFFFD9E2),
    tertiaryContainer = Color(0xFF623F20),
    onTertiaryContainer = Color(0xFFFFDCC1),
    background = Color(0xFF191113),
    surface = Color(0xFF191113),
    surfaceContainerLowest = Color(0xFF140C0E),
    surfaceContainer = Color(0xFF261D20),
    surfaceContainerHigh = Color(0xFF31282A),
    surfaceContainerHighest = Color(0xFF3C3235),
    onSurface = Color(0xFFEFDFE2),
    onSurfaceVariant = Color(0xFFD5C2C6),
    outline = Color(0xFF9E8C90),
    outlineVariant = Color(0xFF514347),
    error = Color(0xFFFFB4AB),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val AppTypography = Typography().let { base ->
    base.copy(
        // Brand line on cards: caps, letter-spaced.
        labelMedium = base.labelMedium.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
    )
}

@Composable
fun CosmeticsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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

/** Tinted background + ink color for photo placeholders, per category (design spec). */
data class CategoryTone(val tone: Color, val ink: Color)

fun Category.tone(): CategoryTone = when (this) {
    Category.SKINCARE, Category.BODY -> CategoryTone(Color(0xFFE3F0F6), Color(0xFF3B6E86))
    Category.MAKEUP, Category.NAILS, Category.FRAGRANCE -> CategoryTone(Color(0xFFF6E3EA), Color(0xFF8C4A60))
    Category.HAIR -> CategoryTone(Color(0xFFE9F2E1), Color(0xFF4F6B35))
    Category.OTHER -> CategoryTone(Color(0xFFF1ECE4), Color(0xFF6B5B45))
}
