package tv.roam.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.roam.client.R
import tv.roam.client.ui.theme.TvColors

/** A television-sized text editor with an optional phone-pairing handoff. */
@Composable
fun TvTextEditDialog(
    title: String,
    initialValue: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    multiline: Boolean = false,
    numberOnly: Boolean = false,
    onFromPhone: (() -> Unit)? = null,
) {
    var text by remember(initialValue) { mutableStateOf(initialValue) }
    val focus = remember { FocusRequester() }
    val firstActionFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    TvDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = TvColors.Text,
            )
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = TextStyle(
                    color = TvColors.Text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                ),
                cursorBrush = SolidColor(TvColors.Teal),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (numberOnly) KeyboardType.Number else KeyboardType.Text,
                    imeAction = if (multiline) ImeAction.Default else ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSave(text) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (multiline) 200.dp else 72.dp)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown &&
                            event.key == Key.DirectionDown
                        ) {
                            firstActionFocus.requestFocus()
                            true
                        } else {
                            false
                        }
                    }
                    .focusRequester(focus)
                    .background(TvColors.SurfaceVariant, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                decorationBox = { inner ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.tv_editor_value_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TvColors.TextMuted,
                            )
                        }
                        inner()
                    }
                },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown &&
                            event.key == Key.DirectionUp
                        ) {
                            focus.requestFocus()
                            true
                        } else {
                            false
                        }
                    },
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                TvSecondaryButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.focusRequester(firstActionFocus),
                )
                if (onFromPhone != null) {
                    TvSecondaryButton(
                        text = stringResource(R.string.tv_editor_from_phone),
                        onClick = onFromPhone,
                    )
                }
                TvPrimaryButton(
                    text = stringResource(R.string.tv_editor_save),
                    onClick = { onSave(text) },
                )
            }
        }
    }
}

/** A focus-trapped single-choice list for transport and policy options. */
@Composable
fun TvOptionsDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    TvDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = TvColors.Text,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                options.forEachIndexed { index, option ->
                    TvSettingsRow(
                        label = option.ifEmpty { stringResource(R.string.tv_editor_none) },
                        value = if (index == selectedIndex) {
                            stringResource(R.string.tv_editor_selected)
                        } else {
                            ""
                        },
                        iconRes = R.drawable.ic_action_done,
                        onClick = { onSelectIndex(index) },
                        modifier = if (index == 0) Modifier.focusRequester(focus) else Modifier,
                    )
                }
            }
            TvSecondaryButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
