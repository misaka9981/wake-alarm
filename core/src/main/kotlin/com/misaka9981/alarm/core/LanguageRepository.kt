package com.misaka9981.alarm.core

/**
 * Port for storing the owner's in-app language choice.
 *
 * `core` declares the contract; the Android adapter persists it. Reading and
 * writing the encoding is [LanguageCodec]'s job, so this port only moves the
 * choice in and out of storage.
 */
interface LanguageRepository {
    /** The owner's chosen language, or [AppLanguage.System] when nothing is stored. */
    suspend fun load(): AppLanguage

    /** Stores [language], replacing any earlier choice. */
    suspend fun save(language: AppLanguage)
}
