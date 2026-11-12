package tv.roam.client.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.OutlinedButtonDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import tv.roam.client.ui.theme.TvColors

@Composable
fun TvRailItem(
    label: String,
    @DrawableRes iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(3.dp, TvColors.Teal),
                shape = RoundedCornerShape(12.dp),
            )
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) TvColors.Teal.copy(alpha = 0.14f) else Color.Transparent,
            contentColor = if (selected) TvColors.Teal else TvColors.TextMuted,
            focusedContainerColor = TvColors.SurfaceFocused,
            focusedContentColor = TvColors.Text,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun TvPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        colors = ButtonDefaults.colors(
            containerColor = TvColors.Teal,
            contentColor = TvColors.Deep,
            focusedContainerColor = TvColors.Teal,
            focusedContentColor = TvColors.Deep,
            pressedContainerColor = TvColors.TealDim,
            pressedContentColor = TvColors.Text,
            disabledContainerColor = TvColors.SurfaceVariant,
            disabledContentColor = TvColors.TextMuted,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun TvSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        colors = OutlinedButtonDefaults.colors(
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
        Text(text = text, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun TvIconAction(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
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
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
        )
    }
}

@Composable
fun TvSettingsRow(
    label: String,
    value: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(14.dp)),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(3.dp, TvColors.Teal),
                shape = RoundedCornerShape(14.dp),
            )
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvColors.Surface,
            contentColor = TvColors.Text,
            focusedContainerColor = TvColors.SurfaceFocused,
            focusedContentColor = TvColors.Text,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = TvColors.Teal,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = TvColors.Text,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = TvColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun TvActionRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun TvStatusDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(10.dp)
            .background(color = color, shape = CircleShape)
    )
}

@Composable
fun TvChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(50)),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(3.dp, TvColors.Teal),
                shape = RoundedCornerShape(50),
            )
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) TvColors.Teal else TvColors.SurfaceVariant,
            contentColor = if (selected) TvColors.Deep else TvColors.Text,
            focusedContainerColor = if (selected) TvColors.Teal else TvColors.SurfaceFocused,
            focusedContentColor = if (selected) TvColors.Deep else TvColors.Text,
        ),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
