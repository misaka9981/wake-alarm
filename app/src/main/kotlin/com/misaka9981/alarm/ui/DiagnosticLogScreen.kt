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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.misaka9981.alarm.R
import com.misaka9981.alarm.core.DiagnosticEntry
import com.misaka9981.alarm.core.DiagnosticLogCodec
import com.misaka9981.alarm.core.FiringOutcome
import com.misaka9981.alarm.core.ReliabilityRequirement
import com.misaka9981.alarm.data.DataStoreDiagnosticLog
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration

/**
 * The Diagnostic Log: what recent Alarms actually did, readable on the phone.
 *
 * Cloud-built sideloaded builds have no device logging, so this page is how a
 * missed or misbehaving Alarm is explained (ADR-0001, `CONTEXT.md`). It only
 * renders the recorded entries; how much history is kept and how it is persisted
 * are decided by [com.misaka9981.alarm.core.DiagnosticLogCodec] in `core`.
 */
@Composable
fun DiagnosticLogScreen(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val log = remember(context) { DataStoreDiagnosticLog(context) }
    var entries by remember { mutableStateOf<List<DiagnosticEntry>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(log) {
        // Newest first reads best on a phone.
        entries = log.load().asReversed()
        loaded = true
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.diagnostic_log_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClose) { Text(text = stringResource(R.string.action_back)) }
        }
        HorizontalDivider()

        when {
            !loaded -> Text(
                text = stringResource(R.string.status_loading),
                modifier = Modifier.padding(24.dp),
            )

            entries.isEmpty() -> Text(
                text = stringResource(R.string.diagnostic_log_empty),
                modifier = Modifier.padding(24.dp),
            )

            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        text = stringResource(
                            R.string.diagnostic_log_showing,
                            DiagnosticLogCodec.MAX_ENTRIES,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(entries) { entry ->
                    DiagnosticEntryRow(entry)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun DiagnosticEntryRow(entry: DiagnosticEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = outcomeLabel(entry.outcome),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(text = stringResource(R.string.diagnostic_log_scheduled, scheduledLabel(entry.scheduledTime)))
        Text(text = stringResource(R.string.diagnostic_log_fired, timestamp.format(entry.firedAt)))
        Text(
            text = stringResource(
                R.string.diagnostic_log_signalled,
                durationLabel(entry.challengeDuration),
                entry.signallingVolume,
            ),
        )
        Text(
            text = when (entry.wrongAnswers) {
                0 -> stringResource(R.string.wrong_answers_none)
                1 -> stringResource(R.string.wrong_answers_one)
                else -> stringResource(R.string.wrong_answers_many, entry.wrongAnswers)
            },
        )
        Text(text = permissionsLabel(entry.missingRequirements))
    }
}

private val timestamp: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE d MMM HH:mm:ss").withZone(ZoneId.systemDefault())

private val scheduledFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

@Composable
private fun scheduledLabel(scheduledTime: Instant?): String =
    scheduledTime?.let(scheduledFormat::format)
        ?: stringResource(R.string.diagnostic_log_time_not_recorded)

@Composable
private fun outcomeLabel(outcome: FiringOutcome): String = stringResource(
    when (outcome) {
        FiringOutcome.Dismissed -> R.string.diagnostic_log_outcome_dismissed
        FiringOutcome.CapExpired -> R.string.diagnostic_log_outcome_cap_expired
        FiringOutcome.EscapeHatch -> R.string.diagnostic_log_outcome_escape_hatch
    },
)

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

@Composable
private fun permissionsLabel(missing: Set<ReliabilityRequirement>): String {
    if (missing.isEmpty()) return stringResource(R.string.diagnostic_log_permissions_all)
    val labels = mutableListOf<String>()
    for (requirement in missing.sortedBy { it.name }) {
        labels += requirementLabel(requirement)
    }
    return stringResource(
        R.string.diagnostic_log_permissions_missing,
        labels.joinToString(stringResource(R.string.list_separator)),
    )
}

@Composable
private fun requirementLabel(requirement: ReliabilityRequirement): String = stringResource(
    when (requirement) {
        ReliabilityRequirement.ExactAlarm -> R.string.requirement_exact_alarm
        ReliabilityRequirement.FullScreenIntent -> R.string.requirement_full_screen_intent
        ReliabilityRequirement.BatteryOptimisation -> R.string.requirement_battery_optimisation
        ReliabilityRequirement.DoNotDisturbAccess -> R.string.requirement_do_not_disturb
    },
)
