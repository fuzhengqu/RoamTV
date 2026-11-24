package tv.roam.client.dto.entities

/** A profile together with its MMKV GUID, used to load a raw custom outbound. */
data class ProfileEntry(
    val guid: String,
    val profile: ProfileItem,
)
