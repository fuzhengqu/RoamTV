package tv.roam.client.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Scrollable column for settings and editor forms.
 *
 * The 6dp top padding is part of the TV focus contract: the first focusable
 * row needs room for its 2dp inset focus ring and rounded top corners. A raw
 * Column + verticalScroll clips them at the viewport edge. Use this component
 * whenever the first child of a scroll list can take focus.
 */
@Composable
fun TvSettingsColumn(
    modifier: Modifier = Modifier,
    state: ScrollState = rememberScrollState(),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(4.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .verticalScroll(state)
            .padding(top = 6.dp),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}
