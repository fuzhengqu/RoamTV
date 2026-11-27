package tv.roam.client.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import tv.roam.client.AppConfig
import tv.roam.client.dto.entities.ProfileItem
import tv.roam.client.enums.EConfigType

class CoreOutboundBuilderXdriveTest {

    @Test
    fun `xdrive settings merge extra defaults and dedicated fields`() {
        val profile = ProfileItem.create(EConfigType.VLESS).copy(
            network = "xdrive",
            xdriveService = "",
            xdriveSecrets = "access, secret",
            xdriveExtra = """{"service":"s3","remoteFolder":"old","segmentBytes":1024,"s3":{"bucket":"demo"}}""",
            path = "/new-folder",
        )

        val settings = CoreOutboundBuilder.buildXdriveSettings(profile)

        assertNotNull(settings)
        assertEquals("s3", settings?.service)
        assertEquals("/new-folder", settings?.remoteFolder)
        assertEquals(listOf("access", "secret"), settings?.secrets)
        assertEquals(1024, settings?.segmentBytes)
        assertEquals("demo", settings?.s3?.bucket)
    }

    @Test
    fun `xdrive defaults to google drive and rejects non object extra`() {
        val profile = ProfileItem.create(EConfigType.VLESS).copy(network = "xdrive")
        assertEquals(
            AppConfig.XDRIVE_SERVICE_GOOGLE_DRIVE,
            CoreOutboundBuilder.buildXdriveSettings(profile)?.service,
        )

        val invalid = profile.copy(xdriveExtra = "[1,2,3]")
        assertNull(CoreOutboundBuilder.buildXdriveSettings(invalid))
    }

    @Test
    fun `transport settings install xdrive settings`() {
        val profile = ProfileItem.create(EConfigType.VLESS).copy(
            network = "xdrive",
            server = "storage.example.com",
            serverPort = "443",
            xdriveService = "local",
            path = "/folder",
        )
        val stream = tv.roam.client.dto.V2rayConfig.OutboundBean.StreamSettingsBean()

        CoreOutboundBuilder.populateTransportSettings(stream, profile)

        assertEquals("xdrive", stream.network)
        assertEquals("local", stream.xdriveSettings?.service)
        assertEquals("/folder", stream.xdriveSettings?.remoteFolder)
    }

    @Test
    fun `xdrive mux is always enabled and coerced into accepted ranges`() {
        val mux = CoreOutboundBuilder.buildXdriveMux(
            concurrency = "0",
            xudpConcurrency = "-1",
            xudpProxyUDP443 = "skip",
        )

        assertEquals(true, mux.enabled)
        assertEquals(8, mux.concurrency)
        assertEquals(
            AppConfig.DEFAULT_MUX_XUDP_CONCURRENCY.toInt(),
            mux.xudpConcurrency,
        )
        assertEquals("reject", mux.xudpProxyUDP443)

        val valid = CoreOutboundBuilder.buildXdriveMux("16", "4", "allow")
        assertEquals(16, valid.concurrency)
        assertEquals(4, valid.xudpConcurrency)
        assertEquals("allow", valid.xudpProxyUDP443)
    }
}
