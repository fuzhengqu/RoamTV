package tv.roam.client.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.roam.client.R
import tv.roam.client.feature.pairing.PairingStatus
import tv.roam.client.feature.pairing.PairingUiState
import tv.roam.client.ui.theme.TvColors

@Composable
fun PairingDialog(
    state: PairingUiState,
    onCancel: () -> Unit,
) {
    val cancelFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { cancelFocus.requestFocus() }

    TvDialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.tv_pairing_title),
                style = MaterialTheme.typography.headlineSmall,
                color = TvColors.Text,
            )
            when (state.status) {
                PairingStatus.Starting -> {
                    Text(
                        text = stringResource(R.string.tv_pairing_waiting),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TvColors.TextMuted,
                    )
                }

                PairingStatus.Error -> {
                    Text(
                        text = state.errorRes?.let { stringResource(it) }
                            ?: stringResource(R.string.tv_pairing_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TvColors.Error,
                    )
                }

                PairingStatus.Ready -> {
                    state.qrCode?.let { bitmap ->
                        Box(
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(180.dp),
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.tv_pairing_scan),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TvColors.TextMuted,
                    )
                    Text(
                        text = stringResource(
                            R.string.tv_pairing_code,
                            state.code.orEmpty(),
                        ),
                        style = MaterialTheme.typography.headlineMedium,
                        color = TvColors.Teal,
                    )
                    Text(
                        text = stringResource(
                            R.string.tv_pairing_address,
                            state.address.orEmpty(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = TvColors.TextMuted,
                    )
                    Text(
                        text = stringResource(
                            R.string.tv_pairing_expires,
                            state.secondsLeft,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = TvColors.Amber,
                    )
                }

                PairingStatus.Idle -> Unit
            }
            TvPrimaryButton(
                text = stringResource(R.string.action_cancel),
                onClick = onCancel,
                modifier = Modifier.focusRequester(cancelFocus),
            )
        }
    }
}
