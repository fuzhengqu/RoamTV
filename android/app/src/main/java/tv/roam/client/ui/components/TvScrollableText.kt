package tv.roam.client.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.roam.client.ui.theme.TvColors

/**
 * A long read-only text block that stays scrollable with a D-pad. Each line is
 * a focus target so focus traversal scrolls the list, and Back closes the
 * containing dialog.
 */
@Composable
fun TvScrollableText(
    lines: List<String>,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    color: Color = TvColors.TextMuted,
    autoFocus: Boolean = true,
) {
    val first = remember { FocusRequester() }
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        itemsIndexed(lines, key = { index, _ -> index }) { index, line ->
            Text(
                text = line.ifEmpty { " " },
                style = style,
                color = color,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (index == 0) Modifier.focusRequester(first) else Modifier)
                    .focusable()
                    .padding(vertical = 2.dp),
            )
        }
    }
    if (autoFocus) {
        LaunchedEffect(Unit) { first.requestFocus() }
    }
}
