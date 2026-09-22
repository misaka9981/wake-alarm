package com.misaka9981.alarm.core

/**
 * Encodes the owner's [AppLanguage] choice to a single string and back, with no
 * dependency on any storage or Android API.
 *
 * This is the part of persistence that is decidable, so it is the part kept in
 * `core` and tested; the Android adapter only stores the string it produces.
 * Anything unrecognised decodes to [AppLanguage.System], which fails safe: the
 * app follows the phone rather than pinning a language the owner did not choose.
 */
object LanguageCodec {
    private const val CHINESE = "zh"
    private const val ENGLISH = "en"

    fun encode(language: AppLanguage): String = when (language) {
        AppLanguage.System -> "system"
        AppLanguage.Chinese -> CHINESE
        AppLanguage.English -> ENGLISH
    }

    fun decode(text: String?): AppLanguage = when (text?.trim()?.lowercase()) {
        CHINESE -> AppLanguage.Chinese
        ENGLISH -> AppLanguage.English
        else -> AppLanguage.System
    }
}
