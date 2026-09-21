package com.misaka9981.alarm.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.misaka9981.alarm.core.Alarm
import com.misaka9981.alarm.core.AlarmCodec
import com.misaka9981.alarm.core.AlarmFormatException
import com.misaka9981.alarm.core.AlarmRepository
import kotlinx.coroutines.flow.first

private val Context.alarmDataStore by preferencesDataStore(name = "alarms")

/**
 * Thin Android adapter that persists the Alarm configuration in DataStore.
 *
 * All the decision-making — what the configuration looks like on disk, and what
 * counts as valid — lives in `core` ([AlarmCodec]); this class only reads and
 * writes the resulting string, so an app restart reloads the same Alarms.
 */
class DataStoreAlarmRepository(context: Context) : AlarmRepository {
    private val appContext = context.applicationContext

    override suspend fun load(): List<Alarm> {
        val stored = appContext.alarmDataStore.data.first()[ALARMS] ?: return emptyList()
        return try {
            AlarmCodec.decode(stored)
        } catch (_: AlarmFormatException) {
            // Unreadable data must not crash the screen; start from clean and let
            // the next save overwrite it.
            emptyList()
        }
    }

    override suspend fun save(alarms: List<Alarm>) {
        appContext.alarmDataStore.edit { preferences ->
            preferences[ALARMS] = AlarmCodec.encode(alarms)
        }
    }

    private companion object {
        val ALARMS = stringPreferencesKey("alarms")
    }
}
