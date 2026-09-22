package com.misaka9981.alarm.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.misaka9981.alarm.core.AppLanguage
import com.misaka9981.alarm.core.LanguageCodec
import com.misaka9981.alarm.core.LanguageRepository
import kotlinx.coroutines.flow.first

private val Context.languageDataStore by preferencesDataStore(name = "language")

/**
 * Thin Android adapter that persists the owner's in-app language choice in
 * DataStore.
 *
 * The encoding and parsing are [LanguageCodec]'s job, so this class only reads
 * and writes the resulting string; an app restart reloads the same choice, and
 * unreadable data falls back to following the system.
 */
class DataStoreLanguageRepository(context: Context) : LanguageRepository {
    private val appContext = context.applicationContext

    override suspend fun load(): AppLanguage =
        LanguageCodec.decode(appContext.languageDataStore.data.first()[LANGUAGE])

    override suspend fun save(language: AppLanguage) {
        appContext.languageDataStore.edit { preferences ->
            preferences[LANGUAGE] = LanguageCodec.encode(language)
        }
    }

    private companion object {
        val LANGUAGE = stringPreferencesKey("language")
    }
}
