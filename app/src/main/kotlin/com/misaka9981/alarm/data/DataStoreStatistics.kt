package com.misaka9981.alarm.data

import android.content.Context
import com.misaka9981.alarm.core.StatisticsRepository
import com.misaka9981.alarm.core.WakeStatistics
import java.time.ZoneId

/**
 * Thin Android adapter that reads the morning statistics.
 *
 * All the decision-making — which figures exist and how they are derived — lives
 * in `core` ([WakeStatistics.of]); this class only reads the recorded Diagnostic
 * Log and supplies the device's time zone. Statistics are computed from recorded
 * history rather than transient state, so they survive an app restart.
 */
class DataStoreStatistics(context: Context) : StatisticsRepository {
    private val log = DataStoreDiagnosticLog(context.applicationContext)

    override suspend fun load(): WakeStatistics =
        WakeStatistics.of(log.load(), ZoneId.systemDefault())
}
