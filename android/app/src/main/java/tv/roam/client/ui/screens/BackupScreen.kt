package tv.roam.client.ui.screens

import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.roam.client.AppConfig
import tv.roam.client.BuildConfig
import tv.roam.client.R
import tv.roam.client.dto.entities.WebDavConfig
import tv.roam.client.extension.toast
import tv.roam.client.feature.backup.BackupAction
import tv.roam.client.feature.backup.BackupUiState
import tv.roam.client.feature.backup.BackupViewModel
import tv.roam.client.ui.components.TvConfirmDialog
import tv.roam.client.ui.components.TvDialog
import tv.roam.client.ui.components.TvKeyHintBar
import tv.roam.client.ui.components.TvOptionsDialog
import tv.roam.client.ui.components.TvPrimaryButton
import tv.roam.client.ui.components.TvSecondaryButton
import tv.roam.client.ui.components.TvSettingsRow
import tv.roam.client.ui.components.TvTextEditDialog
import tv.roam.client.ui.components.TvTopBar
import tv.roam.client.ui.theme.TvColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class BackupLocation(@StringRes val labelRes: Int) {
    Local(R.string.backup_location_local),
    WebDav(R.string.backup_location_webdav),
}

private enum class WebDavField(@StringRes val labelRes: Int, val password: Boolean = false) {
    Url(R.string.title_webdav_url),
    User(R.string.title_webdav_user),
    Password(R.string.title_webdav_pass, password = true),
    RemotePath(R.string.title_webdav_remote_path),
}

@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val appName = stringResource(R.string.app_name)
    val shareTitle = stringResource(R.string.title_configuration_share)
    val firstFocus = remember { FocusRequester() }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showCleanupDialog by remember { mutableStateOf(false) }
    var showWebDavDialog by remember { mutableStateOf(false) }

    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) viewModel.onAction(BackupAction.BackupLocal(uri))
    }
    val openBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.onAction(BackupAction.RestoreLocal(uri))
    }

    LaunchedEffect(Unit) { firstFocus.requestFocus() }
    LaunchedEffect(state.messageRes) {
        val message = state.messageRes ?: return@LaunchedEffect
        context.toast(message)
        viewModel.onAction(BackupAction.ConsumeMessage)
    }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        context.toast(message)
        viewModel.onAction(BackupAction.ConsumeMessage)
    }
    LaunchedEffect(state.shareFilePath) {
        val path = state.shareFilePath ?: return@LaunchedEffect
        viewModel.onAction(BackupAction.ConsumeShare)
        runCatching {
            val file = File(path)
            val uri = FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + ".cache",
                file,
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newUri(context.contentResolver, file.name, uri)
            }
            context.startActivity(
                Intent.createChooser(intent, shareTitle)
            )
        }.onFailure {
            context.toast(R.string.toast_failure)
        }
    }
    LaunchedEffect(state.restored) {
        if (state.restored) viewModel.onAction(BackupAction.ConsumeRestore)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
        ) {
            TvTopBar(
                title = stringResource(R.string.title_configuration_backup_restore),
                onBack = onBack,
            )
            if (state.isBusy) {
                Text(
                    text = stringResource(R.string.tv_editor_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.Amber,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TvSettingsRow(
                    label = stringResource(R.string.title_configuration_backup),
                    value = "",
                    iconRes = R.drawable.ic_backup_24dp,
                    onClick = { showBackupDialog = true },
                    modifier = Modifier.focusRequester(firstFocus),
                )
                TvSettingsRow(
                    label = stringResource(R.string.title_configuration_share),
                    value = "",
                    iconRes = R.drawable.ic_share_24dp,
                    onClick = { viewModel.onAction(BackupAction.Share) },
                )
                TvSettingsRow(
                    label = stringResource(R.string.title_configuration_restore),
                    value = "",
                    iconRes = R.drawable.ic_restore_24dp,
                    onClick = { showRestoreDialog = true },
                )
                TvSettingsRow(
                    label = stringResource(R.string.title_profile_storage_cleanup),
                    value = stringResource(R.string.summary_profile_storage_cleanup),
                    iconRes = R.drawable.ic_delete_24dp,
                    onClick = { showCleanupDialog = true },
                )
                Spacer(Modifier.height(12.dp))
                TvSettingsRow(
                    label = stringResource(R.string.title_webdav_config_setting),
                    value = state.webDavConfig?.baseUrl.orEmpty().ifEmpty {
                        stringResource(R.string.tv_editor_not_set)
                    },
                    iconRes = R.drawable.ic_settings_24dp,
                    onClick = { showWebDavDialog = true },
                )
                Spacer(Modifier.height(24.dp))
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

    if (showBackupDialog) {
        TvOptionsDialog(
            title = stringResource(R.string.title_configuration_backup),
            options = BackupLocation.entries.map { stringResource(it.labelRes) },
            selectedIndex = -1,
            onSelectIndex = { index ->
                showBackupDialog = false
                when (BackupLocation.entries.getOrNull(index)) {
                    BackupLocation.Local -> {
                        val stamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
                            .format(Date())
                        createBackup.launch("${appName}_$stamp.zip")
                    }

                    BackupLocation.WebDav -> viewModel.onAction(BackupAction.BackupWebDav)
                    null -> Unit
                }
            },
            onDismiss = { showBackupDialog = false },
        )
    }

    if (showRestoreDialog) {
        TvOptionsDialog(
            title = stringResource(R.string.title_configuration_restore),
            options = BackupLocation.entries.map { stringResource(it.labelRes) },
            selectedIndex = -1,
            onSelectIndex = { index ->
                showRestoreDialog = false
                when (BackupLocation.entries.getOrNull(index)) {
                    BackupLocation.Local -> openBackup.launch(arrayOf("application/zip"))
                    BackupLocation.WebDav -> viewModel.onAction(BackupAction.RestoreWebDav)
                    null -> Unit
                }
            },
            onDismiss = { showRestoreDialog = false },
        )
    }

    if (showCleanupDialog) {
        TvConfirmDialog(
            title = stringResource(R.string.title_profile_storage_cleanup),
            message = stringResource(R.string.message_profile_storage_cleanup),
            confirmText = stringResource(R.string.action_delete),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = {
                showCleanupDialog = false
                viewModel.onAction(BackupAction.CleanupProfiles)
            },
            onDismiss = { showCleanupDialog = false },
            danger = true,
        )
    }

    if (showWebDavDialog) {
        WebDavDialog(
            initial = state.webDavConfig,
            onSave = { config ->
                showWebDavDialog = false
                viewModel.onAction(BackupAction.SaveWebDav(config))
            },
            onDismiss = { showWebDavDialog = false },
        )
    }
}

@Composable
private fun WebDavDialog(
    initial: WebDavConfig?,
    onSave: (WebDavConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    var url by remember(initial) { mutableStateOf(initial?.baseUrl.orEmpty()) }
    var username by remember(initial) { mutableStateOf(initial?.username.orEmpty()) }
    var password by remember(initial) { mutableStateOf(initial?.password.orEmpty()) }
    var remotePath by remember(initial) {
        mutableStateOf(initial?.remoteBasePath ?: AppConfig.WEBDAV_BACKUP_DIR)
    }
    var editing by remember { mutableStateOf<WebDavField?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    fun valueOf(field: WebDavField): String = when (field) {
        WebDavField.Url -> url
        WebDavField.User -> username
        WebDavField.Password -> password
        WebDavField.RemotePath -> remotePath
    }

    fun setValue(field: WebDavField, value: String) {
        when (field) {
            WebDavField.Url -> url = value
            WebDavField.User -> username = value
            WebDavField.Password -> password = value
            WebDavField.RemotePath -> remotePath = value
        }
    }

    TvDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.title_webdav_config_setting),
                style = MaterialTheme.typography.headlineSmall,
                color = TvColors.Text,
            )
            WebDavField.entries.forEachIndexed { index, field ->
                val value = valueOf(field)
                TvSettingsRow(
                    label = stringResource(field.labelRes),
                    value = when {
                        field.password && value.isNotEmpty() -> stringResource(R.string.tv_password_masked)
                        value.isEmpty() -> stringResource(R.string.tv_editor_not_set)
                        else -> value
                    },
                    iconRes = if (field.password) R.drawable.ic_lock_24dp else R.drawable.ic_edit_24dp,
                    onClick = { editing = field },
                    modifier = if (index == 0) Modifier.focusRequester(focus) else Modifier,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                TvSecondaryButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onDismiss,
                )
                TvPrimaryButton(
                    text = stringResource(R.string.menu_item_save_config),
                    onClick = {
                        onSave(
                            WebDavConfig(
                                baseUrl = url.trim(),
                                username = username.trim().ifEmpty { null },
                                password = password,
                                remoteBasePath = remotePath.trim().ifEmpty {
                                    AppConfig.WEBDAV_BACKUP_DIR
                                },
                            )
                        )
                    },
                    enabled = url.isNotBlank(),
                )
            }
        }
    }

    editing?.let { field ->
        TvTextEditDialog(
            title = stringResource(field.labelRes),
            initialValue = valueOf(field),
            password = field.password,
            onSave = { value ->
                setValue(field, value)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}
