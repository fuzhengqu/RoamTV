package tv.roam.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.roam.client.R
import tv.roam.client.extension.toast
import tv.roam.client.feature.subscription.SubscriptionListViewModel
import tv.roam.client.feature.subscription.SubscriptionRow
import tv.roam.client.feature.subscription.SubscriptionsAction
import tv.roam.client.ui.components.TvIconAction
import tv.roam.client.ui.components.TvKeyHintBar
import tv.roam.client.ui.components.TvPrimaryButton
import tv.roam.client.ui.components.TvSettingsRow
import tv.roam.client.ui.components.TvTopBar
import tv.roam.client.ui.theme.TvColors

@Composable
fun SubscriptionsScreen(
    viewModel: SubscriptionListViewModel,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val firstFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.onAction(SubscriptionsAction.Load)
    }
    LaunchedEffect(state.updateResult) {
        val result = state.updateResult ?: return@LaunchedEffect
        if (result.configCount > 0) {
            val quantity = context.resources.getQuantityString(
                R.plurals.title_update_config_count,
                result.configCount,
                result.configCount,
            )
            context.toast(quantity)
        } else {
            context.toast(R.string.title_update_subscription_no_subscription)
        }
        viewModel.onAction(SubscriptionsAction.ConsumeResult)
    }
    LaunchedEffect(state.messageRes) {
        val message = state.messageRes ?: return@LaunchedEffect
        context.toast(message)
        viewModel.onAction(SubscriptionsAction.ConsumeMessage)
    }
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading && state.rows.isNotEmpty()) firstFocus.requestFocus()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
        ) {
            TvTopBar(
                title = stringResource(R.string.title_sub_setting),
                actions = {
                    TvIconAction(
                        iconRes = R.drawable.ic_add_24dp,
                        contentDescription = stringResource(R.string.acc_add_subscription),
                        onClick = onAdd,
                    )
                    TvIconAction(
                        iconRes = R.drawable.ic_restore_24dp,
                        contentDescription = stringResource(R.string.acc_update_subscriptions),
                        onClick = { viewModel.onAction(SubscriptionsAction.UpdateAll) },
                    )
                },
            )
            if (state.isUpdating) {
                Text(
                    text = stringResource(R.string.tv_subscriptions_updating),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.Amber,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            if (!state.isLoading && state.rows.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.title_update_subscription_no_subscription),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TvColors.TextMuted,
                    )
                    TvPrimaryButton(
                        text = stringResource(R.string.acc_add_subscription),
                        onClick = onAdd,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.rows, key = { it.guid }) { row ->
                        SubscriptionRowView(
                            row = row,
                            modifier = if (row.guid == state.rows.firstOrNull()?.guid) {
                                Modifier.focusRequester(firstFocus)
                            } else {
                                Modifier
                            },
                            onClick = { onEdit(row.guid) },
                        )
                    }
                }
            }
        }
        TvKeyHintBar(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(),
            hints = listOf(
                stringResource(R.string.tv_key_ok) to stringResource(R.string.tv_key_ok_action),
                stringResource(R.string.tv_key_back) to stringResource(R.string.tv_key_back_action),
            ),
        )
    }
}

@Composable
private fun SubscriptionRowView(
    row: SubscriptionRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subtitle = if (row.url.isBlank()) {
        stringResource(R.string.tv_editor_not_set)
    } else {
        row.url
    }
    TvSettingsRow(
        label = row.remarks,
        value = subtitle,
        iconRes = R.drawable.ic_subscriptions_24dp,
        onClick = onClick,
        modifier = modifier,
    )
}
