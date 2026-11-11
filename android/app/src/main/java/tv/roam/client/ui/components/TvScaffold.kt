package tv.roam.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.roam.client.R
import tv.roam.client.ui.theme.TvColors

/**
 * Applies the 5% television overscan safe area on top of a deep, vertically
 * graded background. The fraction is used instead of fixed dp so 720p, 1080p
 * and 4K panels all keep the same visual margin.
 */
@Composable
fun TvOverscan(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(TvColors.DeepElevated, TvColors.Deep, TvColors.Deep)
                )
            )
    ) {
        val horizontal = maxWidth * 0.05f
        val vertical = maxHeight * 0.05f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontal, vertical = vertical),
            content = content,
        )
    }
}

/** Standard top bar for a secondary screen. */
@Composable
fun TvTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backFocusRequester: FocusRequester? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = backFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier,
                colors = IconButtonDefaults.colors(
                    containerColor = Color.Transparent,
                    contentColor = TvColors.Text,
                    focusedContainerColor = TvColors.Teal,
                    focusedContentColor = TvColors.Deep,
                    pressedContainerColor = TvColors.SurfaceFocused,
                    pressedContentColor = TvColors.Text,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = TvColors.TextMuted,
                ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back_24dp),
                    contentDescription = stringResource(R.string.acc_back),
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = TvColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = actions,
        )
    }
}

/** A short, always-visible reminder of the remote-control actions on this screen. */
@Composable
fun TvKeyHintBar(
    hints: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        hints.forEach { (key, action) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(TvColors.SurfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.labelSmall,
                        color = TvColors.Teal,
                    )
                }
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelMedium,
                    color = TvColors.TextMuted,
                )
            }
        }
    }
}

/**
 * Root two-column layout used by the primary destinations: navigation rail on
 * the leading edge and a content column that owns its own top bar and hints.
 */
@Composable
fun TvRailLayout(
    rail: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    TvOverscan {
        Row(Modifier.fillMaxSize()) {
            rail()
            Spacer(Modifier.width(24.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                content()
            }
        }
    }
}
