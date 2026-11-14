package tv.roam.client.feature.userasset

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import tv.roam.client.R

class UserAssetEditorValidationTest {

    @Test
    fun `blank remarks and url are required`() {
        val errors = validateAssetEditor(remarks = "", url = "", duplicateRemarks = false)

        assertEquals(
            mapOf(
                AssetEditorField.Remarks to R.string.tv_editor_error_required,
                AssetEditorField.Url to R.string.tv_editor_error_required,
            ),
            errors,
        )
    }

    @Test
    fun `duplicate remarks are rejected`() {
        val errors = validateAssetEditor(
            remarks = "geoip.dat",
            url = "https://example.invalid/geoip.dat",
            duplicateRemarks = true,
        )

        assertEquals(mapOf(AssetEditorField.Remarks to R.string.msg_remark_is_duplicate), errors)
    }

    @Test
    fun `valid values produce no errors`() {
        val errors = validateAssetEditor(
            remarks = "custom.dat",
            url = "https://example.invalid/custom.dat",
            duplicateRemarks = false,
        )

        assertEquals(emptyMap<AssetEditorField, Int>(), errors)
    }

    @Test
    fun `picked file names are reduced to a safe leaf name`() {
        assertEquals("geoip.dat", sanitizeAssetFileName("primary/downloads/geoip.dat"))
        assertEquals("geoip.dat", sanitizeAssetFileName("downloads\\geoip.dat"))
        assertEquals("geoip.dat", sanitizeAssetFileName("  geoip.dat  "))
    }

    @Test
    fun `blank and relative names are rejected`() {
        assertNull(sanitizeAssetFileName(null))
        assertNull(sanitizeAssetFileName(""))
        assertNull(sanitizeAssetFileName("."))
        assertNull(sanitizeAssetFileName(".."))
        assertNull(sanitizeAssetFileName("folder/"))
    }
}
