package com.misaka9981.alarm.core

import kotlin.test.Test
import kotlin.test.assertEquals

class LanguageCodecTest {
    @Test
    fun everyChoiceRoundTripsThroughItsEncoding() {
        AppLanguage.entries.forEach { language ->
            assertEquals(language, LanguageCodec.decode(LanguageCodec.encode(language)))
        }
    }

    @Test
    fun decodeTreatsMissingStorageAsFollowingTheSystem() {
        assertEquals(AppLanguage.System, LanguageCodec.decode(null))
        assertEquals(AppLanguage.System, LanguageCodec.decode(""))
        assertEquals(AppLanguage.System, LanguageCodec.decode("   "))
    }

    @Test
    fun decodeIsCaseInsensitiveAndTrimsSurroundingWhitespace() {
        assertEquals(AppLanguage.Chinese, LanguageCodec.decode(" ZH "))
        assertEquals(AppLanguage.English, LanguageCodec.decode("En"))
    }

    @Test
    fun decodeFallsBackToTheSystemForUnrecognisedData() {
        assertEquals(AppLanguage.System, LanguageCodec.decode("klingon"))
        assertEquals(AppLanguage.System, LanguageCodec.decode("system "))
    }

    @Test
    fun aPinnedChoiceCarriesItsLanguageTagAndSystemCarriesNone() {
        assertEquals("zh", AppLanguage.Chinese.explicitTag)
        assertEquals("en", AppLanguage.English.explicitTag)
        assertEquals(null, AppLanguage.System.explicitTag)
    }
}
