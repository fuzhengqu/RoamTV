package tv.roam.client.enums

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LanguageTest {

    @Test
    fun `fromCode maps every supported code and falls back to auto`() {
        assertEquals(Language.AUTO, Language.fromCode("auto"))
        assertEquals(Language.ENGLISH, Language.fromCode("en"))
        assertEquals(Language.CHINA, Language.fromCode("zh-rCN"))
        assertEquals(Language.TRADITIONAL_CHINESE, Language.fromCode("zh-rTW"))
        assertEquals(Language.VIETNAMESE, Language.fromCode("vi"))
        assertEquals(Language.RUSSIAN, Language.fromCode("ru"))
        assertEquals(Language.PERSIAN, Language.fromCode("fa"))
        assertEquals(Language.ARABIC, Language.fromCode("ar"))
        assertEquals(Language.AZERBAIJANI, Language.fromCode("az"))
        assertEquals(Language.BANGLA, Language.fromCode("bn"))
        assertEquals(Language.BAKHTIARI, Language.fromCode("bqi-rIR"))
        assertEquals(Language.AUTO, Language.fromCode("unknown"))
    }

    @Test
    fun `fromLanguageTag distinguishes simplified and traditional Chinese`() {
        assertEquals(Language.CHINA, Language.fromLanguageTag("zh-CN"))
        assertEquals(Language.TRADITIONAL_CHINESE, Language.fromLanguageTag("zh-TW"))
        assertEquals(Language.TRADITIONAL_CHINESE, Language.fromLanguageTag("zh-HK"))
        assertEquals(Language.TRADITIONAL_CHINESE, Language.fromLanguageTag("zh-Hant"))
        assertEquals(Language.AUTO, Language.fromLanguageTag(null))
        assertEquals(Language.AUTO, Language.fromLanguageTag("de-DE"))
    }

    @Test
    fun `auto clears the app locale while a language sets a tag`() {
        assertTrue(Language.AUTO.toLocaleList().isEmpty)
        assertEquals("en", Language.ENGLISH.toLocaleList().get(0)?.language)
        assertEquals("zh", Language.CHINA.toLocaleList().get(0)?.language)
    }
}
