package com.misaka9981.alarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.misaka9981.alarm.core.DismissalRecord
import com.misaka9981.alarm.core.WakeStatistics
import com.misaka9981.alarm.data.DataStoreStatistics
import java.time.format.DateTimeFormatter
import kotlin.time.Duration

/**
 * How the owner's mornings are going: the current Streak, the recent dismissal
 * times and wrong-answer counts, and the average wake time.
 *
 * It renders only what [WakeStatistics] reports; every figure is derived in
 * `core` from the recorded Diagnostic Log, so the page is a thin adapter over
 * recorded history rather than transient state. There is no Snooze anywhere.
 */
@Composable
fun StatisticsScreen(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val statisticsRepository = remember(context) { DataStoreStatistics(context) }
    var statistics by remember { mutableStateOf<WakeStatistics?>(null) }

    LaunchedEffect(statisticsRepository) {
        statistics = statisticsRepository.load()
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClose) { Text(text = "Back") }
        }
        HorizontalDivider()

        val current = statistics
        when {
            current == null -> Text(text = "Loading…", modifier = Modifier.padding(24.dp))

            else -> StatisticsBody(current, modifier = modifier)
        }
    }
}

@Composable
private fun StatisticsBody(statistics: WakeStatistics, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item { StatisticRow("Current Streak", streakLabel(statistics.currentStreak)) }
        item {
            StatisticRow(
                "Average wake time",
                statistics.averageWakeTime?.let(clockFormat::format) ?: "No dismissals yet",
            )
        }
        item {
            Text(
                text = "Recent dismissals",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        if (statistics.recentDismissals.isEmpty()) {
            item {
                Text(
                    text = "No Alarms have been dismissed yet. Each dismissal is summarised here.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        } else {
            items(statistics.recentDismissals) { dismissal ->
                DismissalRow(dismissal)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun StatisticRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = value, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun DismissalRow(dismissal: DismissalRecord) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = dayFormat.format(dismissal.day), style = MaterialTheme.typography.titleMedium)
        Text(text = "Dismissed at ${clockFormat.format(dismissal.dismissedAt)}")
        Text(text = "Rang for ${durationLabel(dismissal.ringingDuration)}")
        Text(
            text = when (dismissal.wrongAnswers) {
                0 -> "No wrong answers"
                1 -> "1 wrong answer"
                else -> "${dismissal.wrongAnswers} wrong answers"
            },
        )
    }
}

private fun streakLabel(count: Int): String = when (count) {
    0 -> "No Streak yet"
    1 -> "1 day"
    else -> "$count days"
}

private val clockFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private val dayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")

private fun durationLabel(duration: Duration): String {
    val totalSeconds = duration.inWholeSeconds
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}
