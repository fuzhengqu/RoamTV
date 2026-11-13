package tv.roam.client.feature.server

import androidx.annotation.StringRes
import tv.roam.client.R
import tv.roam.client.enums.EConfigType
import tv.roam.client.enums.NetworkType

/** One editable value in the server form. */
enum class ServerField(
    @StringRes val defaultLabelRes: Int,
    val kind: ServerFieldKind = ServerFieldKind.Text,
    val secret: Boolean = false,
) {
    Remarks(R.string.server_lab_remarks),
    Address(R.string.server_lab_address),
    Port(R.string.server_lab_port, ServerFieldKind.Number),
    Password(R.string.server_lab_id3, secret = true),
    Method(R.string.server_lab_security),
    Encryption(R.string.server_lab_encryption),
    Flow(R.string.server_lab_flow),
    Username(R.string.server_lab_security4),
    SecretKey(R.string.server_lab_secret_key, secret = true),
    PublicKey(R.string.server_lab_public_key),
    PreSharedKey(R.string.server_lab_preshared_key, secret = true),
    Reserved(R.string.server_lab_reserved),
    LocalAddress(R.string.server_lab_local_address),
    Mtu(R.string.server_lab_local_mtu, ServerFieldKind.Number),
    RemoteDns(R.string.server_lab_remote_dns),
    ObfsPassword(R.string.server_obfs_password, secret = true),
    PortHopping(R.string.server_lab_port_hop),
    PortHoppingInterval(R.string.server_lab_port_hop_interval, ServerFieldKind.Number),
    BandwidthDown(R.string.server_lab_bandwidth_down),
    BandwidthUp(R.string.server_lab_bandwidth_up),
    Network(R.string.server_lab_network, ServerFieldKind.Dropdown),
    HeaderType(R.string.server_lab_head_type, ServerFieldKind.Dropdown),
    Mode(R.string.server_lab_mode_type, ServerFieldKind.Dropdown),
    XhttpMode(R.string.server_lab_xhttp_mode, ServerFieldKind.Dropdown),
    ServiceName(R.string.server_lab_path_grpc),
    Authority(R.string.server_lab_request_host_grpc),
    Host(R.string.server_lab_request_host),
    Path(R.string.server_lab_path),
    XhttpExtra(R.string.server_lab_xhttp_extra, ServerFieldKind.Multiline),
    FinalMask(R.string.server_lab_final_mask, ServerFieldKind.Multiline),
    Seed(R.string.server_lab_path_kcp),
    KcpMtu(R.string.server_lab_kcp_mtu, ServerFieldKind.Number),
    KcpTti(R.string.server_lab_kcp_tti, ServerFieldKind.Number),
    BrowserDialerMode(R.string.server_lab_browser_dialer, ServerFieldKind.Dropdown),
    StreamSecurity(R.string.server_lab_stream_security, ServerFieldKind.Dropdown),
    Sni(R.string.server_lab_sni),
    Fingerprint(R.string.server_lab_stream_fingerprint, ServerFieldKind.Dropdown),
    Alpn(R.string.server_lab_stream_alpn, ServerFieldKind.Dropdown),
    AllowInsecure(R.string.server_lab_allow_insecure, ServerFieldKind.Switch),
    PublicKeyReality(R.string.server_lab_public_key),
    ShortId(R.string.server_lab_short_id),
    SpiderX(R.string.server_lab_spider_x),
    Mldsa65Verify(R.string.server_lab_mldsa65_verify),
    EchConfigList(R.string.server_lab_ech_config_list),
    VerifyPeerCertByName(R.string.server_lab_verify_peer_cert_by_name),
    PinnedCa256(R.string.server_lab_pinned_ca256),
    Content(R.string.server_lab_content, ServerFieldKind.Multiline),
}

enum class ServerFieldKind { Text, Number, Multiline, Dropdown, Switch }

enum class ServerSection(@StringRes val titleRes: Int) {
    Basic(R.string.tv_editor_section_basic),
    Protocol(R.string.tv_editor_section_protocol),
    Transport(R.string.tv_editor_section_transport),
    Security(R.string.tv_editor_section_security),
    Advanced(R.string.tv_editor_section_advanced),
}

/** Immutable editor state owned by [ServerEditorViewModel]. */
data class ServerEditorState(
    val guid: String = "",
    val configType: EConfigType = EConfigType.VMESS,
    val values: Map<ServerField, String> = emptyMap(),
    val switches: Map<ServerField, Boolean> = emptyMap(),
    val errors: Map<ServerField, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val validationAttempt: Int = 0,
    val savedGuid: String? = null,
    val deleted: Boolean = false,
    val restartRequired: Boolean = false,
    @StringRes val generalError: Int? = null,
) {
    fun value(field: ServerField): String = values[field].orEmpty()

    fun isOn(field: ServerField): Boolean = switches[field] == true

    val isNew: Boolean
        get() = guid.isBlank()
}

sealed interface ServerEditorAction {
    data class Load(
        val guid: String,
        val configType: EConfigType,
        val restartOnSave: Boolean,
    ) : ServerEditorAction

    data class SetValue(val field: ServerField, val value: String) : ServerEditorAction
    data class SetSwitch(val field: ServerField, val value: Boolean) : ServerEditorAction
    data object Save : ServerEditorAction
    data object Delete : ServerEditorAction
    data object ConsumeSaved : ServerEditorAction
    data object ConsumeDeleted : ServerEditorAction
    data object ConsumeGeneralError : ServerEditorAction
}

/**
 * Field order and visibility for the TV editor. The rules mirror the phone
 * editor so a configuration written here has the same shape as one imported
 * or edited on a phone.
 */
object ServerEditorDefinition {

    private val passwordRequired = setOf(
        EConfigType.VMESS,
        EConfigType.VLESS,
        EConfigType.TROJAN,
        EConfigType.SHADOWSOCKS,
        EConfigType.HYSTERIA2,
    )

    private val networkTypes = setOf(
        EConfigType.VMESS,
        EConfigType.VLESS,
        EConfigType.SHADOWSOCKS,
        EConfigType.TROJAN,
    )

    private val streamSecurityTypes = setOf(
        EConfigType.VMESS,
        EConfigType.VLESS,
        EConfigType.SHADOWSOCKS,
        EConfigType.TROJAN,
    )

    fun passwordRequired(type: EConfigType): Boolean = type in passwordRequired

    fun visibleFields(state: ServerEditorState): List<ServerField> {
        val type = state.configType
        val fields = mutableListOf<ServerField>()
        fields += ServerField.Remarks
        if (type == EConfigType.CUSTOM) {
            fields += ServerField.Content
            return fields
        }
        fields += ServerField.Address
        fields += ServerField.Port
        when (type) {
            EConfigType.VMESS -> {
                fields += ServerField.Password
                fields += ServerField.Method
            }

            EConfigType.VLESS -> {
                fields += ServerField.Password
                fields += ServerField.Encryption
                fields += ServerField.Flow
            }

            EConfigType.SHADOWSOCKS -> {
                fields += ServerField.Password
                fields += ServerField.Method
            }

            EConfigType.SOCKS, EConfigType.HTTP -> {
                fields += ServerField.Username
                fields += ServerField.Password
            }

            EConfigType.TROJAN -> fields += ServerField.Password

            EConfigType.WIREGUARD -> {
                fields += ServerField.SecretKey
                fields += ServerField.PublicKey
                fields += ServerField.PreSharedKey
                fields += ServerField.Reserved
                fields += ServerField.LocalAddress
                fields += ServerField.Mtu
                fields += ServerField.RemoteDns
                fields += ServerField.FinalMask
            }

            EConfigType.HYSTERIA2 -> {
                fields += ServerField.Password
                fields += ServerField.ObfsPassword
                fields += ServerField.PortHopping
                fields += ServerField.PortHoppingInterval
                fields += ServerField.BandwidthDown
                fields += ServerField.BandwidthUp
                fields += ServerField.StreamSecurity
                fields += ServerField.Sni
                fields += ServerField.EchConfigList
                fields += ServerField.PinnedCa256
                fields += ServerField.AllowInsecure
                fields += ServerField.FinalMask
            }

            else -> Unit
        }
        if (type in networkTypes) {
            fields += ServerField.Network
            when (state.value(ServerField.Network)) {
                NetworkType.TCP.type -> fields += ServerField.HeaderType
                NetworkType.KCP.type -> fields += ServerField.HeaderType
                NetworkType.GRPC.type -> fields += ServerField.Mode
                NetworkType.XHTTP.type -> fields += ServerField.XhttpMode
            }
            fields += ServerField.Host
            if (state.value(ServerField.Network) != NetworkType.KCP.type) {
                fields += ServerField.Path
            }
            if (state.value(ServerField.Network) == NetworkType.XHTTP.type) {
                fields += ServerField.XhttpExtra
            }
            if (state.value(ServerField.Network) == NetworkType.KCP.type) {
                fields += ServerField.Seed
                fields += ServerField.KcpMtu
                fields += ServerField.KcpTti
            }
            fields += ServerField.FinalMask
            if (state.value(ServerField.Network) in
                listOf(NetworkType.WS.type, NetworkType.XHTTP.type)
            ) {
                fields += ServerField.BrowserDialerMode
            }
        }
        if (type in streamSecurityTypes) {
            fields += ServerField.StreamSecurity
            val security = state.value(ServerField.StreamSecurity)
            if (security.isNotEmpty()) {
                fields += ServerField.Sni
                fields += ServerField.Fingerprint
            }
            when (security) {
                tv.roam.client.AppConfig.TLS -> {
                    fields += ServerField.AllowInsecure
                    fields += ServerField.Alpn
                    fields += ServerField.EchConfigList
                    fields += ServerField.VerifyPeerCertByName
                    fields += ServerField.PinnedCa256
                }

                tv.roam.client.AppConfig.REALITY -> {
                    fields += ServerField.PublicKeyReality
                    fields += ServerField.ShortId
                    fields += ServerField.SpiderX
                    fields += ServerField.Mldsa65Verify
                }
            }
        }
        return fields.distinct()
    }

    fun sectionOf(field: ServerField): ServerSection = when (field) {
        ServerField.Remarks,
        ServerField.Address,
        ServerField.Port,
        ServerField.Content
        -> ServerSection.Basic

        ServerField.Network,
        ServerField.HeaderType,
        ServerField.Mode,
        ServerField.XhttpMode,
        ServerField.ServiceName,
        ServerField.Authority,
        ServerField.Host,
        ServerField.Path,
        ServerField.Seed,
        ServerField.KcpMtu,
        ServerField.KcpTti,
        ServerField.BrowserDialerMode
        -> ServerSection.Transport

        ServerField.StreamSecurity,
        ServerField.Sni,
        ServerField.Fingerprint,
        ServerField.Alpn,
        ServerField.AllowInsecure,
        ServerField.PublicKeyReality,
        ServerField.ShortId,
        ServerField.SpiderX,
        ServerField.Mldsa65Verify,
        ServerField.EchConfigList,
        ServerField.VerifyPeerCertByName,
        ServerField.PinnedCa256
        -> ServerSection.Security

        ServerField.XhttpExtra,
        ServerField.FinalMask,
        -> ServerSection.Advanced

        else -> ServerSection.Protocol
    }
}
