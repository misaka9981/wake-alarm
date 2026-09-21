package com.misaka9981.alarm.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.misaka9981.alarm.core.EscapeHatchFormatException
import com.misaka9981.alarm.core.EscapeHatchLog
import com.misaka9981.alarm.core.EscapeHatchUse
import com.misaka9981.alarm.core.EscapeHatchUseCodec
import kotlinx.coroutines.flow.first

private val Context.escapeHatchLogDataStore by preferencesDataStore(name = "escape_hatch_log")

/**
 * Thin Android adapter that persists every Escape Hatch use in DataStore.
 *
 * All the decision-making — what a recorded use looks like on disk — lives in
 * `core` ([EscapeHatchUseCodec]); this class only reads and appends, so the owner
 * can see how often they resort to the Escape Hatch (ticket 08) and the day's
 * Streak can be broken from recorded history (ticket 09).
 */
class DataStoreEscapeHatchLog(context: Context) : EscapeHatchLog {
    private val appContext = context.applicationContext

    override suspend fun record(use: EscapeHatchUse) {
        appContext.escapeHatchLogDataStore.edit { preferences ->
            val existing = decode(preferences[USES])
            preferences[USES] = EscapeHatchUseCodec.encode(existing + use)
        }
    }

    override suspend fun load(): List<EscapeHatchUse> =
        decode(appContext.escapeHatchLogDataStore.data.first()[USES])

    private fun decode(stored: String?): List<EscapeHatchUse> {
        if (stored == null) return emptyList()
        return try {
            EscapeHatchUseCodec.decode(stored)
        } catch (_: EscapeHatchFormatException) {
            // Unreadable data must not crash the firing screen; start from clean
            // and let the next record overwrite it.
            emptyList()
        }
    }

    private companion object {
        val USES = stringPreferencesKey("uses")
    }
}
