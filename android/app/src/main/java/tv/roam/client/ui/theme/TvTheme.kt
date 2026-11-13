package tv.roam.client.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Shapes
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme

/**
 * RoamTV's single, fixed television theme.
 *
 * A television is watched from roughly three metres away in a normally dark
 * room, so the app deliberately has no light theme and no dynamic colour. The
 * palette keeps a high contrast ratio for text and makes the focused element
 * obvious without relying on colour alone.
 */
object TvColors {
    val Deep = Color(0xFF0B0F14)
    val DeepElevated = Color(0xFF131A22)
    val Surface = Color(0xFF131A22)
    val SurfaceVariant = Color(0xFF1B2430)
    val SurfaceFocused = Color(0xFF243040)
    val Teal = Color(0xFF4FD1C5)
    val TealDim = Color(0xFF2F7F78)
    val Amber = Color(0xFFF6C177)
    val Text = Color(0xFFF2F5F7)
    val TextMuted = Color(0xFFA9B4C0)
    val Success = Color(0xFF5BE49B)
    val Error = Color(0xFFFF6B6B)
    val Border = Color(0xFF2A3746)
}

private val RoamTvColorScheme = darkColorScheme(
    primary = TvColors.Teal,
    onPrimary = TvColors.Deep,
    secondary = TvColors.Amber,
    onSecondary = TvColors.Deep,
    background = TvColors.Deep,
    onBackground = TvColors.Text,
    surface = TvColors.Surface,
    onSurface = TvColors.Text,
    surfaceVariant = TvColors.SurfaceVariant,
    onSurfaceVariant = TvColors.TextMuted,
    error = TvColors.Error,
    onError = TvColors.Deep,
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
fun RoamTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RoamTvColorScheme,
        typography = RoamTvTypography,
        shapes = RoamTvShapes,
        content = content,
    )
}
