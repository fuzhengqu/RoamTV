package tv.roam.client.feature.pairing

import android.app.Application
import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.roam.client.R
import tv.roam.client.BuildConfig
import tv.roam.client.mvvm.BaseViewModel
import java.net.Inet4Address

enum class PairingStatus {
    Idle,
    Starting,
    Ready,
    Error,
}

data class PairingUiState(
    val status: PairingStatus = PairingStatus.Idle,
    val qrCode: Bitmap? = null,
    val code: String? = null,
    val address: String? = null,
    val secondsLeft: Int = 0,
    @StringRes val errorRes: Int? = null,
)

/**
 * Owns the temporary LAN pairing server. The server is stopped when the user
 * leaves the import screen, when a payload is received, or when the session
 * expires. Nothing is persisted.
 */
class PairingViewModel(application: Application) : BaseViewModel(application) {

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    private val _received = Channel<String>(Channel.BUFFERED)
    val received = _received.receiveAsFlow()

    private var server: PairingHttpServer? = null
    private var countdownJob: Job? = null
    private var startJob: Job? = null

    fun start() {
        if (_uiState.value.status == PairingStatus.Starting ||
            _uiState.value.status == PairingStatus.Ready
        ) {
            return
        }
        _uiState.value = PairingUiState(status = PairingStatus.Starting)
        startJob = viewModelScope.launch(Dispatchers.IO) {
            val appContext = getApplication<Application>()
            val address = LanAddressResolver.find(appContext)
            if (address == null) {
                publishError(R.string.tv_pairing_no_network)
                return@launch
            }
            val pageHtml = runCatching {
                appContext.assets.open("pairing/index.html")
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
            }.getOrNull()
            val sjclJs = runCatching {
                appContext.assets.open("pairing/sjcl.js")
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
            }.getOrNull()
            if (pageHtml == null || sjclJs == null) {
                publishError(R.string.tv_pairing_error)
                return@launch
            }

            val session = PairingSessionFactory.create()
            // The Android emulator exposes the guest as 10.0.2.15, which the
            // host cannot route directly. Bind all interfaces in debug builds
            // so `adb forward` can drive the page from a desktop browser; release
            // builds keep the resolved LAN bind. Remove this fallback when the
            // pairing check runs on physical hardware.
            val bindAddress =
                if (BuildConfig.DEBUG && address.hostAddress == EMULATOR_NAT_ADDRESS) {
                    java.net.InetAddress.getByName("0.0.0.0")
                } else {
                    address
                }
            val httpServer = PairingHttpServer(
                session = session,
                bindAddress = bindAddress,
                pageHtml = pageHtml,
                sjclJs = sjclJs,
                onText = { text ->
                    viewModelScope.launch(Dispatchers.Main) {
                        _received.send(text)
                        stopInternal()
                    }
                },
            )
            try {
                val port = httpServer.start()
                if (!isActive) {
                    httpServer.close()
                    return@launch
                }
                server = httpServer
                val url = buildString {
                    append("http://")
                    append(address.hostAddress)
                    append(':')
                    append(port)
                    append("/s/")
                    append(session.id)
                    append("?code=")
                    append(session.code)
                    append('#')
                    append(PairingCrypto.base64Url(session.key))
                }
                val qr = PairingQrCode.create(url)
                if (!isActive || server !== httpServer) {
                    httpServer.close()
                    return@launch
                }
                _uiState.value = PairingUiState(
                    status = PairingStatus.Ready,
                    qrCode = qr,
                    code = session.code,
                    address = "${address.hostAddress}:$port",
                    secondsLeft = (PairingSessionFactory.DEFAULT_TTL_MILLIS / 1000).toInt(),
                )
                startCountdown(session, httpServer)
            } catch (_: SecurityException) {
                httpServer.close()
                publishError(R.string.tv_pairing_permission_denied)
            } catch (_: Exception) {
                httpServer.close()
                publishError(R.string.tv_pairing_error)
            }
        }
    }

    fun stop() {
        stopInternal()
        _uiState.value = PairingUiState()
    }

    override fun onCleared() {
        stopInternal()
        super.onCleared()
    }

    private fun startCountdown(session: PairingSession, httpServer: PairingHttpServer) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (isActive) {
                val left = ((session.expiresAtMillis - System.currentTimeMillis()) / 1000)
                    .toInt()
                    .coerceAtLeast(0)
                _uiState.value = _uiState.value.copy(secondsLeft = left)
                if (left == 0) {
                    httpServer.close()
                    publishError(R.string.tv_pairing_expired)
                    break
                }
                delay(1_000)
            }
        }
    }

    private suspend fun publishError(@StringRes resId: Int) {
        withContext(Dispatchers.Main) {
            _uiState.value = PairingUiState(
                status = PairingStatus.Error,
                errorRes = resId,
            )
        }
    }

    private fun stopInternal() {
        startJob?.cancel()
        startJob = null
        countdownJob?.cancel()
        countdownJob = null
        server?.close()
        server = null
    }

    private companion object {
        const val EMULATOR_NAT_ADDRESS = "10.0.2.15"
    }
}
