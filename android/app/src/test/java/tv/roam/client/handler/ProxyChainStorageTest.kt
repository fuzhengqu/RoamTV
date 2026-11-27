package tv.roam.client.handler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import tv.roam.client.dto.entities.ProfileItem
import tv.roam.client.enums.EConfigType

class ProxyChainStorageTest {

    @Test
    fun `new list storage preserves a remark containing a comma`() {
        val profile = ProfileItem.create(EConfigType.PROXYCHAIN)
        val members = listOf("Tokyo, JP", "Frankfurt")

        ProxyChainStorage.encode(profile, members)

        assertEquals(members, profile.proxyChainProfileList)
        assertNull(profile.proxyChainProfiles)
        assertEquals(members, ProxyChainStorage.decode(profile))
    }

    @Test
    fun `legacy comma separated value is migrated best effort`() {
        val profile = ProfileItem.create(EConfigType.PROXYCHAIN).copy(
            proxyChainProfiles = "Tokyo, JP, Frankfurt",
        )

        assertEquals(
            listOf("Tokyo", "JP", "Frankfurt"),
            ProxyChainStorage.decode(profile),
        )
    }

    @Test
    fun `empty values decode to an empty list`() {
        assertEquals(emptyList<String>(), ProxyChainStorage.decode(ProfileItem.create(EConfigType.PROXYCHAIN)))
        assertEquals(emptyList<String>(), ProxyChainStorage.splitLegacy(null))
    }
}
