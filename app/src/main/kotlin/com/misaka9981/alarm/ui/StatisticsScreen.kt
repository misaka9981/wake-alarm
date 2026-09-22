package com.misaka9981.alarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.misaka9981.alarm.R
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { WakeAlarmTopBar(stringResource(R.string.statistics_title), onClose) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            val current = statistics
            when {
                current == null -> Text(
                    text = stringResource(R.string.status_loading),
                    modifier = Modifier.padding(24.dp),
                )

                else -> StatisticsBody(current, modifier = Modifier)
            }
        }
    }
}

@Composable
private fun StatisticsBody(statistics: WakeStatistics, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            StatisticRow(
                stringResource(R.string.statistics_current_streak),
                streakLabel(statistics.currentStreak),
            )
        }
        item {
            StatisticRow(
                stringResource(R.string.statistics_average_wake_time),
                statistics.averageWakeTime?.let(clockFormat::format)
                    ?: stringResource(R.string.statistics_no_dismissals),
            )
        }
        item {
            Text(
                text = stringResource(R.string.statistics_recent_dismissals),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        if (statistics.recentDismissals.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.statistics_empty),
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
        Text(text = stringResource(R.string.statistics_dismissed_at, clockFormat.format(dismissal.dismissedAt)))
        Text(text = stringResource(R.string.statistics_rang_for, durationLabel(dismissal.ringingDuration)))
        Text(
            text = when (dismissal.wrongAnswers) {
                0 -> stringResource(R.string.wrong_answers_none)
                1 -> stringResource(R.string.wrong_answers_one)
                else -> stringResource(R.string.wrong_answers_many, dismissal.wrongAnswers)
            },
        )
    }
}

@Composable
private fun streakLabel(count: Int): String = when (count) {
    0 -> stringResource(R.string.statistics_streak_none)
    1 -> stringResource(R.string.statistics_streak_days, 1)
    else -> stringResource(R.string.statistics_streak_days, count)
}

private val clockFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private val dayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")

@Composable
private fun durationLabel(duration: Duration): String {
    val totalSeconds = duration.inWholeSeconds
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) {
        stringResource(R.string.duration_minutes_seconds, minutes, seconds)
    } else {
        stringResource(R.string.duration_seconds, seconds)
    }
}
