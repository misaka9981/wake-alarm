package com.misaka9981.alarm.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.misaka9981.alarm.core.EscapeHatchPassword
import com.misaka9981.alarm.core.EscapeHatchRepository
import kotlinx.coroutines.flow.first

private val Context.escapeHatchDataStore by preferencesDataStore(name = "escape_hatch")

/**
 * Thin Android adapter that persists the owner's private Escape Hatch password in
 * DataStore.
 *
 * The password is a single secret the app only ever compares, so there is no
 * structure to encode; the adapter just reads and writes the string and lets
 * `core` validate it through [EscapeHatchPassword]. An empty or blank stored
 * value is treated as "no password set", which fails closed.
 */
class DataStoreEscapeHatchRepository(context: Context) : EscapeHatchRepository {
    private val appContext = context.applicationContext

    override suspend fun load(): EscapeHatchPassword? {
        val stored = appContext.escapeHatchDataStore.data.first()[PASSWORD] ?: return null
        return try {
            EscapeHatchPassword(stored)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    override suspend fun save(password: EscapeHatchPassword) {
        appContext.escapeHatchDataStore.edit { preferences ->
            preferences[PASSWORD] = password.value
        }
    }

    private companion object {
        val PASSWORD = stringPreferencesKey("password")
    }
}
