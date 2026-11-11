package tv.roam.client.helper

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Generic Activity-result helper for QR scanning.
 *
 * The target Activity is supplied by the caller so this class has no dependency
 * on any UI package. TV devices typically have no camera; callers should use
 * the config-import channels (clipboard, pairing-code LAN push, document
 * picker) instead of this helper.
 */
class QRCodeScannerHelper(private val activity: ComponentActivity) {
    private var scanCallback: ((String?) -> Unit)? = null

    private val scanLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scanResult = result.data?.getStringExtra("SCAN_RESULT")
                scanCallback?.invoke(scanResult)
            } else {
                scanCallback?.invoke(null)
            }
            scanCallback = null
        }

    fun launch(target: Class<out Activity>, onResult: (String?) -> Unit) {
        scanCallback = onResult
        scanLauncher.launch(Intent(activity, target))
    }
}
