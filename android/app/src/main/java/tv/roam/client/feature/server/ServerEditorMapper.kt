package tv.roam.client.feature.server

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.google.gson.JsonParser
import tv.roam.client.AppConfig
import tv.roam.client.R
import tv.roam.client.dto.entities.ProfileItem
import tv.roam.client.enums.EConfigType
import tv.roam.client.enums.NetworkType
import tv.roam.client.extension.nullIfBlank

/**
 * Pure mapping and validation for the TV server editor.
 *
 * Keeping this free of Android storage makes the save rules testable on the
 * JVM. [ServerEditorViewModel] owns persistence and threading.
 */
object ServerEditorMapper {

    fun initialState(type: EConfigType): ServerEditorState {
        val values = linkedMapOf<ServerField, String>()
        values[ServerField.Remarks] = ""
        if (type == EConfigType.CUSTOM) {
            values[ServerField.Content] = ""
        } else {
            values[ServerField.Address] = ""
            values[ServerField.Port] = AppConfig.DEFAULT_PORT.toString()
            values[ServerField.Network] = NetworkType.TCP.type
            values[ServerField.HeaderType] = "none"
            values[ServerField.StreamSecurity] = when (type) {
                EConfigType.TROJAN -> AppConfig.TLS
                EConfigType.HYSTERIA2 -> AppConfig.TLS
                else -> ""
            }
            if (type == EConfigType.WIREGUARD) {
                values[ServerField.LocalAddress] = AppConfig.WIREGUARD_LOCAL_ADDRESS_V4
                values[ServerField.Mtu] = AppConfig.WIREGUARD_LOCAL_MTU
                values[ServerField.RemoteDns] = AppConfig.WIREGUARD_LOCAL_REMOTE_DNS
                values[ServerField.Reserved] = "0,0,0"
            }
            if (type == EConfigType.HYSTERIA2) {
                values[ServerField.AllowInsecure] = ""
            }
        }
        return ServerEditorState(
            configType = type,
            values = values,
            switches = mapOf(ServerField.AllowInsecure to false),
            isLoading = false,
        )
    }

    fun fromProfile(item: ProfileItem): ServerEditorState {
        val type = item.configType
        val values = linkedMapOf<ServerField, String>()
        values[ServerField.Remarks] = item.remarks
        values[ServerField.Address] = item.server.orEmpty()
        values[ServerField.Port] = item.serverPort ?: AppConfig.DEFAULT_PORT.toString()
        values[ServerField.Password] = item.password.orEmpty()
        values[ServerField.Method] = item.method.orEmpty()
        values[ServerField.Encryption] = item.method.orEmpty()
        values[ServerField.Flow] = item.flow.orEmpty()
        values[ServerField.Username] = item.username.orEmpty()
        values[ServerField.SecretKey] = item.secretKey.orEmpty()
        values[ServerField.PublicKey] = item.publicKey.orEmpty()
        values[ServerField.PreSharedKey] = item.preSharedKey.orEmpty()
        values[ServerField.Reserved] = item.reserved ?: "0,0,0"
        values[ServerField.LocalAddress] =
            item.localAddress ?: AppConfig.WIREGUARD_LOCAL_ADDRESS_V4
        values[ServerField.Mtu] = item.mtu?.toString() ?: AppConfig.WIREGUARD_LOCAL_MTU
        values[ServerField.RemoteDns] =
            item.remoteDNS ?: AppConfig.WIREGUARD_LOCAL_REMOTE_DNS
        values[ServerField.ObfsPassword] = item.obfsPassword.orEmpty()
        values[ServerField.PortHopping] = item.portHopping.orEmpty()
        values[ServerField.PortHoppingInterval] = item.portHoppingInterval.orEmpty()
        values[ServerField.BandwidthDown] = item.bandwidthDown.orEmpty()
        values[ServerField.BandwidthUp] = item.bandwidthUp.orEmpty()
        values[ServerField.Network] = item.network ?: NetworkType.TCP.type
        values[ServerField.HeaderType] = item.headerType ?: "none"
        values[ServerField.Mode] = item.mode.orEmpty()
        values[ServerField.XhttpMode] = item.xhttpMode.orEmpty()
        values[ServerField.ServiceName] = item.serviceName.orEmpty()
        values[ServerField.Authority] = item.authority.orEmpty()
        values[ServerField.Host] = item.host.orEmpty()
        values[ServerField.Path] = item.path.orEmpty()
        values[ServerField.XhttpExtra] = item.xhttpExtra.orEmpty()
        values[ServerField.FinalMask] = item.finalMask.orEmpty()
        values[ServerField.Seed] = item.seed.orEmpty()
        values[ServerField.KcpMtu] = item.kcpMtu?.toString().orEmpty()
        values[ServerField.KcpTti] = item.kcpTti?.toString().orEmpty()
        values[ServerField.BrowserDialerMode] = item.browserDialerMode.orEmpty()
        values[ServerField.StreamSecurity] = item.security.orEmpty()
        values[ServerField.Sni] = item.sni.orEmpty()
        values[ServerField.Fingerprint] = item.fingerPrint.orEmpty()
        values[ServerField.Alpn] = item.alpn.orEmpty()
        values[ServerField.PublicKeyReality] =
            if (item.security == AppConfig.REALITY) item.publicKey.orEmpty() else ""
        values[ServerField.ShortId] = item.shortId.orEmpty()
        values[ServerField.SpiderX] = item.spiderX.orEmpty()
        values[ServerField.Mldsa65Verify] = item.mldsa65Verify.orEmpty()
        values[ServerField.EchConfigList] = item.echConfigList.orEmpty()
        values[ServerField.VerifyPeerCertByName] = item.verifyPeerCertByName.orEmpty()
        values[ServerField.PinnedCa256] = item.pinnedCA256.orEmpty()
        return ServerEditorState(
            guid = "",
            configType = type,
            values = values,
            switches = mapOf(ServerField.AllowInsecure to (item.insecure == true)),
            isLoading = false,
        )
    }

    fun toProfile(state: ServerEditorState, initial: ProfileItem): ProfileItem {
        val type = state.configType
        val isVmess = type == EConfigType.VMESS
        val isVless = type == EConfigType.VLESS
        val isShadowsocks = type == EConfigType.SHADOWSOCKS
        val isSocksOrHttp = type == EConfigType.SOCKS || type == EConfigType.HTTP
        val isWireguard = type == EConfigType.WIREGUARD
        val isHysteria2 = type == EConfigType.HYSTERIA2

        if (type == EConfigType.CUSTOM) {
            return initial.copy(
                configType = type,
                remarks = state.value(ServerField.Remarks).trim(),
            )
        }

        return initial.copy(
            configType = type,
            remarks = state.value(ServerField.Remarks).trim(),
            server = state.value(ServerField.Address),
            serverPort = state.value(ServerField.Port),
            password = state.value(ServerField.Password),
            method = when {
                isVmess || isShadowsocks -> state.value(ServerField.Method)
                isVless -> state.value(ServerField.Encryption)
                else -> null
            },
            flow = if (isVless) state.value(ServerField.Flow) else null,
            username = if (isSocksOrHttp) state.value(ServerField.Username) else null,
            secretKey = if (isWireguard) state.value(ServerField.SecretKey) else null,
            publicKey = when {
                isWireguard -> state.value(ServerField.PublicKey)
                state.value(ServerField.StreamSecurity) == AppConfig.REALITY ->
                    state.value(ServerField.PublicKeyReality)

                else -> null
            },
            preSharedKey = if (isWireguard) state.value(ServerField.PreSharedKey) else null,
            reserved = if (isWireguard) state.value(ServerField.Reserved) else null,
            localAddress = if (isWireguard) state.value(ServerField.LocalAddress) else null,
            mtu = if (isWireguard) state.value(ServerField.Mtu).toIntOrNull() else null,
            remoteDNS = if (isWireguard) state.value(ServerField.RemoteDns) else null,
            obfsPassword = if (isHysteria2) state.value(ServerField.ObfsPassword) else null,
            portHopping = if (isHysteria2) state.value(ServerField.PortHopping) else null,
            portHoppingInterval =
                if (isHysteria2) state.value(ServerField.PortHoppingInterval) else null,
            bandwidthDown = if (isHysteria2) state.value(ServerField.BandwidthDown) else null,
            bandwidthUp = if (isHysteria2) state.value(ServerField.BandwidthUp) else null,
            network = state.value(ServerField.Network),
            headerType = state.value(ServerField.HeaderType),
            mode = state.value(ServerField.Mode).nullIfBlank(),
            xhttpMode = state.value(ServerField.XhttpMode).nullIfBlank(),
            serviceName = state.value(ServerField.ServiceName).nullIfBlank(),
            authority = state.value(ServerField.Authority).nullIfBlank(),
            host = state.value(ServerField.Host),
            path = state.value(ServerField.Path),
            xhttpExtra = state.value(ServerField.XhttpExtra).nullIfBlank(),
            finalMask = state.value(ServerField.FinalMask).nullIfBlank(),
            seed = state.value(ServerField.Seed).nullIfBlank(),
            kcpMtu = state.value(ServerField.KcpMtu).toIntOrNull(),
            kcpTti = state.value(ServerField.KcpTti).toIntOrNull(),
            browserDialerMode = if (state.value(ServerField.Network) in
                listOf(NetworkType.WS.type, NetworkType.XHTTP.type)
            ) {
                state.value(ServerField.BrowserDialerMode).nullIfBlank()
            } else {
                null
            },
            security = when {
                isHysteria2 && state.value(ServerField.StreamSecurity).isBlank() -> AppConfig.TLS
                else -> state.value(ServerField.StreamSecurity)
            },
            sni = state.value(ServerField.Sni),
            insecure = state.isOn(ServerField.AllowInsecure),
            fingerPrint = state.value(ServerField.Fingerprint),
            alpn = state.value(ServerField.Alpn),
            shortId = state.value(ServerField.ShortId),
            spiderX = state.value(ServerField.SpiderX),
            mldsa65Verify = state.value(ServerField.Mldsa65Verify),
            echConfigList = state.value(ServerField.EchConfigList),
            verifyPeerCertByName = state.value(ServerField.VerifyPeerCertByName),
            pinnedCA256 = state.value(ServerField.PinnedCa256),
        )
    }

    /** A running/selected profile is deleted through the Home flow, never here. */
    fun canDelete(guid: String, selectedGuid: String?): Boolean =
        guid.isNotBlank() && guid != selectedGuid

    fun validate(state: ServerEditorState): Map<ServerField, Int> {        val errors = linkedMapOf<ServerField, Int>()
        val type = state.configType
        if (state.value(ServerField.Remarks).isBlank()) {
            errors[ServerField.Remarks] = R.string.tv_editor_error_required
        }
        if (type == EConfigType.CUSTOM) {
            if (state.value(ServerField.Content).isBlank()) {
                errors[ServerField.Content] = R.string.tv_editor_error_required
            }
            return errors
        }
        if (state.value(ServerField.Address).isBlank()) {
            errors[ServerField.Address] = R.string.tv_editor_error_required
        }
        if (type != EConfigType.HYSTERIA2 &&
            (state.value(ServerField.Port).toIntOrNull() ?: 0) <= 0
        ) {
            errors[ServerField.Port] = R.string.tv_editor_error_port
        }
        if (ServerEditorDefinition.passwordRequired(type) &&
            state.value(ServerField.Password).isBlank()
        ) {
            errors[ServerField.Password] = R.string.tv_editor_error_required
        }
        if (type == EConfigType.TROJAN &&
            state.value(ServerField.StreamSecurity).isBlank()
        ) {
            errors[ServerField.StreamSecurity] = R.string.tv_editor_error_required
        }
        if (state.value(ServerField.XhttpExtra).isNotBlank() &&
            !isJsonObject(state.value(ServerField.XhttpExtra))
        ) {
            errors[ServerField.XhttpExtra] = R.string.tv_editor_error_json
        }
        if (state.value(ServerField.FinalMask).isNotBlank() &&
            !isJsonObject(state.value(ServerField.FinalMask))
        ) {
            errors[ServerField.FinalMask] = R.string.tv_editor_error_json
        }
        return errors
    }

    private fun isJsonObject(value: String): Boolean = try {
        JsonParser.parseString(value).isJsonObject
    } catch (_: Exception) {
        false
    }

    @StringRes
    fun labelRes(
        field: ServerField,
        type: EConfigType,
        values: Map<ServerField, String>,
    ): Int = when (field) {
        ServerField.Password -> when (type) {
            EConfigType.VMESS, EConfigType.VLESS -> R.string.server_lab_id
            EConfigType.SOCKS, EConfigType.HTTP -> R.string.server_lab_id4
            else -> R.string.server_lab_id3
        }

        ServerField.Host -> when (values[ServerField.Network]) {
            NetworkType.WS.type -> R.string.server_lab_request_host_ws
            NetworkType.HTTP_UPGRADE.type -> R.string.server_lab_request_host_httpupgrade
            NetworkType.XHTTP.type -> R.string.server_lab_request_host_xhttp
            NetworkType.H2.type -> R.string.server_lab_request_host_h2
            NetworkType.GRPC.type -> R.string.server_lab_request_host_grpc
            NetworkType.TCP.type -> R.string.server_lab_request_host_http
            else -> R.string.server_lab_request_host6
        }

        ServerField.Path -> when (values[ServerField.Network]) {
            NetworkType.WS.type -> R.string.server_lab_path_ws
            NetworkType.HTTP_UPGRADE.type -> R.string.server_lab_path_httpupgrade
            NetworkType.XHTTP.type -> R.string.server_lab_path_xhttp
            NetworkType.H2.type -> R.string.server_lab_path_h2
            NetworkType.GRPC.type -> R.string.server_lab_path_grpc
            else -> R.string.server_lab_path
        }

        ServerField.HeaderType -> R.string.server_lab_head_type

        else -> field.defaultLabelRes
    }

    @ArrayRes
    fun optionsRes(
        field: ServerField,
        type: EConfigType,
        values: Map<ServerField, String>,
    ): Int? = when (field) {
        ServerField.Method -> when (type) {
            EConfigType.SHADOWSOCKS -> R.array.ss_securitys
            else -> R.array.securitys
        }

        ServerField.Network -> R.array.networks
        ServerField.HeaderType ->
            if (values[ServerField.Network] == NetworkType.KCP.type) {
                R.array.header_type_kcp_and_quic
            } else {
                R.array.header_type_tcp
            }

        ServerField.Mode -> R.array.mode_type_grpc
        ServerField.XhttpMode -> R.array.xhttp_mode
        ServerField.BrowserDialerMode -> R.array.browser_dialer_mode_value
        ServerField.StreamSecurity -> R.array.streamsecurityxs
        ServerField.Fingerprint -> R.array.streamsecurity_utls
        ServerField.Alpn -> R.array.streamsecurity_alpn
        ServerField.Flow -> R.array.flows
        else -> null
    }
}
