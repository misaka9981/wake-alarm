package com.misaka9981.alarm.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.misaka9981.alarm.core.AnchorCatalog
import com.misaka9981.alarm.core.AnchorCodec
import com.misaka9981.alarm.core.AnchorFormatException
import com.misaka9981.alarm.core.AnchorRepository
import kotlinx.coroutines.flow.first

private val Context.anchorDataStore by preferencesDataStore(name = "anchors")

/**
 * Thin Android adapter that persists the Physical Anchor configuration in
 * DataStore.
 *
 * All the decision-making — what the configuration looks like on disk, and what
 * counts as valid — lives in `core` ([AnchorCodec]); this class only reads and
 * writes the resulting string, so the owner's anchors and bindings survive an
 * app restart.
 */
class DataStoreAnchorRepository(context: Context) : AnchorRepository {
    private val appContext = context.applicationContext

    override suspend fun load(): AnchorCatalog {
        val stored = appContext.anchorDataStore.data.first()[ANCHORS] ?: return AnchorCatalog.empty
        return try {
            AnchorCodec.decode(stored)
        } catch (_: AnchorFormatException) {
            // Unreadable data must not crash the screen; start from clean and let
            // the next save overwrite it.
            AnchorCatalog.empty
        }
    }

    override suspend fun save(catalog: AnchorCatalog) {
        appContext.anchorDataStore.edit { preferences ->
            preferences[ANCHORS] = AnchorCodec.encode(catalog)
        }
    }

    private companion object {
        val ANCHORS = stringPreferencesKey("anchors")
    }
}
