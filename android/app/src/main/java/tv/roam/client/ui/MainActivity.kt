package tv.roam.client.ui

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import tv.roam.client.AngApplication
import tv.roam.client.R
import tv.roam.client.core.LauncherManager
import tv.roam.client.extension.toast
import tv.roam.client.feature.main.MainAction
import tv.roam.client.feature.main.MainRepository
import tv.roam.client.feature.main.MainViewModel
import tv.roam.client.feature.pairing.PairingViewModel
import tv.roam.client.feature.settings.SettingsViewModel
import tv.roam.client.handler.AppLocaleManager
import tv.roam.client.handler.SettingsManager
import tv.roam.client.ui.theme.RoamTvTheme

/**
 * The only Activity in RoamTV.
 *
 * All screens are Compose destinations inside this Activity. Keeping a single
 * entry point removes the phone-era Activity-per-screen launch stack, makes
 * remote-control back handling predictable, and lets notification/launcher/
 * deep-link intents resolve to the same place.
 */
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.Factory(application, MainRepository(application as AngApplication))
    }

    private val settingsViewModel: SettingsViewModel by viewModels()
    private val pairingViewModel: PairingViewModel by viewModels()

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) startServiceNow()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Complete the one-time AppCompat locale handoff after the activity
        // context exists, matching the legacy locale migration contract.
        AppLocaleManager.onActivityCreated(this)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        mainViewModel.onAction(MainAction.Initialize)
        handleIntent(intent)

        setContent {
            RoamTvTheme {
                TvApp(
                    mainViewModel = mainViewModel,
                    settingsViewModel = settingsViewModel,
                    pairingViewModel = pairingViewModel,
                    onStartService = ::requestServiceStart,
                    onStopService = ::stopService,
                    onRestartService = ::restartService,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /**
     * One Activity receives text shares and roamtv:// links. The import parser
     * validates the payload; the intent is cleared so a configuration is not
     * imported twice after a configuration change.
     */
    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val text = when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") intent.getStringExtra(Intent.EXTRA_TEXT) else null
            }

            Intent.ACTION_VIEW -> {
                val data = intent.data
                if (data?.scheme == "roamtv") {
                    data.getQueryParameter("url") ?: data.getQueryParameter("config")
                } else {
                    null
                }
            }

            else -> null
        }
        if (text.isNullOrBlank()) return
        mainViewModel.onAction(MainAction.ImportBatchConfig(text))
        intent.removeExtra(Intent.EXTRA_TEXT)
        intent.data = null
    }

    /**
     * Starts the configured service. VPN mode needs the system consent screen;
     * proxy-only and root mode start directly, preserving the existing mode
     * branches from the phone UI.
     */
    private fun requestServiceStart() {
        val selectedGuid = mainViewModel.uiState.value.selectedGuid
        if (selectedGuid.isNullOrEmpty()) {
            toast(R.string.title_file_chooser)
            return
        }
        if (!SettingsManager.isVpnMode()) {
            startServiceNow()
            return
        }
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent == null) {
            startServiceNow()
        } else {
            vpnPermissionLauncher.launch(prepareIntent)
        }
    }

    private fun startServiceNow() {
        LauncherManager.startService(this)
    }

    private fun stopService() {
        LauncherManager.stopService(this)
    }

    private fun restartService() {
        LauncherManager.restartServiceOrStart(this, ::requestServiceStart)
    }
}
