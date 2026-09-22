package com.misaka9981.alarm.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.misaka9981.alarm.core.DiagnosticEntry
import com.misaka9981.alarm.core.DiagnosticFormatException
import com.misaka9981.alarm.core.DiagnosticLog
import com.misaka9981.alarm.core.DiagnosticLogCodec
import kotlinx.coroutines.flow.first

private val Context.diagnosticLogDataStore by preferencesDataStore(name = "diagnostic_log")

/**
 * Thin Android adapter that persists the Diagnostic Log in DataStore.
 *
 * All the decision-making — what a recorded firing looks like on disk, and how
 * much history is kept — lives in `core` ([DiagnosticLogCodec]); this class only
 * reads and appends. The log survives an app restart, which is the whole point:
 * cloud-built sideloaded builds have no device logging (ADR-0001).
 */
class DataStoreDiagnosticLog(context: Context) : DiagnosticLog {
    private val appContext = context.applicationContext

    override suspend fun record(entry: DiagnosticEntry) {
        appContext.diagnosticLogDataStore.edit { preferences ->
            val existing = decode(preferences[ENTRIES])
            preferences[ENTRIES] = DiagnosticLogCodec.encode(existing + entry)
        }
    }

    override suspend fun load(): List<DiagnosticEntry> =
        decode(appContext.diagnosticLogDataStore.data.first()[ENTRIES])

    private fun decode(stored: String?): List<DiagnosticEntry> {
        if (stored == null) return emptyList()
        return try {
            DiagnosticLogCodec.decode(stored)
        } catch (_: DiagnosticFormatException) {
            // Unreadable data must not crash the log page; start from clean and
            // let the next record overwrite it.
            emptyList()
        }
    }

    private companion object {
        val ENTRIES = stringPreferencesKey("entries")
    }
}
