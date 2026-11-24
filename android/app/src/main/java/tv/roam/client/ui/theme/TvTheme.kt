package tv.roam.client.ui.theme

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Shapes
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme

/**
 * The television palette. The default is the fixed dark RoamTV palette; when
 * the user enables dynamic colour on Android 12+, the palette is derived from
 * the system wallpaper colours. Layout, focus and typography do not change.
 */
@Immutable
data class TvPalette(
    val deep: Color,
    val deepElevated: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceFocused: Color,
    val teal: Color,
    val tealDim: Color,
    val amber: Color,
    val text: Color,
    val textMuted: Color,
    val success: Color,
    val error: Color,
    val border: Color,
)

val DefaultTvPalette = TvPalette(
    deep = Color(0xFF0B0F14),
    deepElevated = Color(0xFF131A22),
    surface = Color(0xFF131A22),
    surfaceVariant = Color(0xFF1B2430),
    surfaceFocused = Color(0xFF243040),
    teal = Color(0xFF4FD1C5),
    tealDim = Color(0xFF2F7F78),
    amber = Color(0xFFF6C177),
    text = Color(0xFFF2F5F7),
    textMuted = Color(0xFFA9B4C0),
    success = Color(0xFF5BE49B),
    error = Color(0xFFFF6B6B),
    border = Color(0xFF2A3746),
)

private val LocalTvPalette = staticCompositionLocalOf { DefaultTvPalette }

/**
 * Colour tokens used by the TV UI. The getters read the active [TvPalette], so
 * all existing screens follow the dynamic-colour setting without a layout
 * change.
 */
object TvColors {
    val Deep: Color
        @Composable @ReadOnlyComposable get() = LocalTvPalette.current.deep
    val DeepElevated: Color
        @Composable @ReadOnlyComposable get() = LocalTvPalette.current.deepElevated
    val Surface: Color
        @Composable @ReadOnlyComposable get() = LocalTvPalette.current.surface
    val SurfaceVariant: Color
        @Composable @ReadOnlyComposable get() = LocalTvPalette.current.surfaceVariant
    val SurfaceFocused: Color
        @Composable @ReadOnlyComposable get() = LocalTvPalette.current.surfaceFocused
    val Teal: Color
        @Composable @ReadOnlyComposable get() = LocalTvPalette.current.teal
    val TealDim: Color
        @Composable @ReadOnlyComposable get() = LocalTvPalette.current.tealDim
    val Amber: Color
        @Composable @ReadOnlyComposable get() = LocalTvPalette.current.amber
    val Text: Color
        @Composable @ReadOnlyComposable get() = LocalTvPalette.current.text
    val TextMuted: Color
        @Composable @ReadOnlyComposable get() = LocalTvPalette.current.textMuted
    val Success: Color
        @Composable @ReadOnlyComposable get() = LocalTvPalette.current.success
    val Error: Color
        @Composable @ReadOnlyComposable get() = LocalTvPalette.current.error
    val Border: Color
        @Composable @ReadOnlyComposable get() = LocalTvPalette.current.border
}

private fun TvPalette.toColorScheme() = darkColorScheme(
    primary = teal,
    onPrimary = deep,
    secondary = amber,
    onSecondary = deep,
    background = deep,
    onBackground = text,
    surface = surface,
    onSurface = text,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = textMuted,
    error = error,
    onError = deep,
)

private val RoamTvTypography = Typography().copy(
    displayLarge = TextStyle(fontSize = 48.sp, lineHeight = 56.sp, fontWeight = FontWeight.SemiBold),
    displayMedium = TextStyle(fontSize = 40.sp, lineHeight = 48.sp, fontWeight = FontWeight.SemiBold),
    displaySmall = TextStyle(fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.SemiBold),
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

private val RoamTvShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun RoamTvTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val palette = remember(dynamicColor, context) {
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicTvPalette(context)
        } else {
            DefaultTvPalette
        }
    }
    CompositionLocalProvider(LocalTvPalette provides palette) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(),
            typography = RoamTvTypography,
            shapes = RoamTvShapes,
            content = content,
        )
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun dynamicTvPalette(context: Context): TvPalette = TvPalette(
    deep = Color(ContextCompat.getColor(context, android.R.color.system_neutral1_900)),
    deepElevated = Color(ContextCompat.getColor(context, android.R.color.system_neutral1_800)),
    surface = Color(ContextCompat.getColor(context, android.R.color.system_neutral1_800)),
    surfaceVariant = Color(ContextCompat.getColor(context, android.R.color.system_neutral2_800)),
    surfaceFocused = Color(ContextCompat.getColor(context, android.R.color.system_neutral2_700)),
    teal = Color(ContextCompat.getColor(context, android.R.color.system_accent1_200)),
    tealDim = Color(ContextCompat.getColor(context, android.R.color.system_accent1_400)),
    amber = Color(ContextCompat.getColor(context, android.R.color.system_accent3_200)),
    text = Color(ContextCompat.getColor(context, android.R.color.system_neutral1_50)),
    textMuted = Color(ContextCompat.getColor(context, android.R.color.system_neutral1_300)),
    success = Color(ContextCompat.getColor(context, android.R.color.system_accent2_200)),
    error = Color(0xFFFF6B6B),
    border = Color(ContextCompat.getColor(context, android.R.color.system_neutral2_600)),
)
