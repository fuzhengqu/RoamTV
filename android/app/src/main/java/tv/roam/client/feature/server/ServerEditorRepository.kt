package tv.roam.client.feature.server

import tv.roam.client.dto.entities.ProfileItem
import tv.roam.client.fmt.CustomFmt
import tv.roam.client.handler.AngConfigManager
import tv.roam.client.handler.MmkvManager

/** Storage boundary for [ServerEditorViewModel]; fakeable in JVM tests. */
interface ServerEditorDataSource {
    fun load(guid: String): ProfileItem?
    fun loadRaw(guid: String): String?
    fun save(guid: String, item: ProfileItem): String
    fun saveRaw(guid: String, content: String)
    fun remove(guid: String)
    fun selectedGuid(): String?
    fun description(item: ProfileItem): String
    fun parseCustom(content: String): ProfileItem
}

class ServerEditorRepository : ServerEditorDataSource {

    override fun load(guid: String): ProfileItem? = MmkvManager.decodeServerConfig(guid)

    override fun loadRaw(guid: String): String? = MmkvManager.decodeServerRaw(guid)

    override fun save(guid: String, item: ProfileItem): String =
        MmkvManager.encodeServerConfig(guid, item)

    override fun saveRaw(guid: String, content: String) =
        MmkvManager.encodeServerRaw(guid, content)

    override fun remove(guid: String) = MmkvManager.removeServer(guid)

    override fun selectedGuid(): String? = MmkvManager.getSelectServer()

    override fun description(item: ProfileItem): String =
        AngConfigManager.generateDescription(item)

    override fun parseCustom(content: String): ProfileItem = CustomFmt.parse(content)
}
