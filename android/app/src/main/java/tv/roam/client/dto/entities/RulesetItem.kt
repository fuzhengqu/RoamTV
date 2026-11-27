package tv.roam.client.dto.entities

data class RulesetItem(
    var id: String = "",
    var remarks: String? = "",
    var ip: List<String>? = null,
    var domain: List<String>? = null,
    var process: List<String>? = null,
    var outboundTag: String = "",
    var port: String? = null,
    var network: String? = null,
    var protocol: List<String>? = null,
    var enabled: Boolean = true,
    var locked: Boolean? = false,
)
/** Keep the first occurrence while regenerating blank or repeated IDs. */
internal fun List<RulesetItem>.ensureUniqueIds(): Boolean {
    val seen = mutableSetOf<String>()
    var changed = false
    for (rule in this) {
        if (rule.id.isBlank() || !seen.add(rule.id)) {
            rule.id = java.util.UUID.randomUUID().toString()
            seen.add(rule.id)
            changed = true
        }
    }
    return changed
}
