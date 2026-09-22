package com.misaka9981.alarm.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.misaka9981.alarm.R
import com.misaka9981.alarm.core.Alarm
import com.misaka9981.alarm.core.AlarmCatalog
import com.misaka9981.alarm.core.AlarmId
import com.misaka9981.alarm.core.AlarmRepository
import com.misaka9981.alarm.core.AlarmTime
import com.misaka9981.alarm.core.ReliabilityRequirement
import com.misaka9981.alarm.schedule.AlarmScheduler
import java.time.DayOfWeek
import java.util.UUID
import kotlinx.coroutines.launch

/**
 * The Alarm list: every Alarm's time, repeat days, and enabled state, with the
 * create/edit/enable/disable/delete actions. Rendering only; the rules live in
 * `core` ([AlarmCatalog]).
 *
 * Saving the list re-arms every Alarm through [scheduler], and loading it does
 * the same so the app is armed after it regains control. [missingRequirements]
 * drives the reliability warning; [onOpenReliability] opens the guide.
 *
 * The navigation links and the debug entry points live in the [Scaffold]'s
 * `topBar`, a dedicated region of their own, so they can never cover an Alarm
 * row, its text, or its enable `Switch`.
 *
 * When [onOpenDevChallenge] is provided (debug builds), a development entry
 * point opens the Dismiss Challenge without an Alarm ringing. [onOpenDevAnchor]
 * and [onOpenDevFire] are the equivalents for the Physical Anchor flow and the
 * end-to-end firing flow.
 */
@Composable
fun AlarmListScreen(
    repository: AlarmRepository,
    scheduler: AlarmScheduler,
    modifier: Modifier = Modifier,
    missingRequirements: Set<ReliabilityRequirement> = emptySet(),
    onOpenReliability: (() -> Unit)? = null,
    onOpenDiagnosticLog: (() -> Unit)? = null,
    onOpenStatistics: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onOpenDevChallenge: (() -> Unit)? = null,
    onOpenDevAnchor: (() -> Unit)? = null,
    onOpenDevFire: ((AlarmId) -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    var catalog by remember { mutableStateOf(AlarmCatalog.empty) }
    var loaded by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Alarm?>(null) }

    LaunchedEffect(repository) {
        catalog = AlarmCatalog.of(repository.load())
        scheduler.reschedule(catalog.alarms)
        loaded = true
    }

    fun commit(next: AlarmCatalog) {
        catalog = next
        scheduler.reschedule(next.alarms)
        scope.launch { repository.save(next.alarms) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AlarmListNavigation(
                missingRequirements = missingRequirements,
                devFireAlarmId = catalog.alarms.firstOrNull()?.id,
                onOpenReliability = onOpenReliability,
                onOpenDiagnosticLog = onOpenDiagnosticLog,
                onOpenStatistics = onOpenStatistics,
                onOpenSettings = onOpenSettings,
                onOpenDevChallenge = onOpenDevChallenge,
                onOpenDevAnchor = onOpenDevAnchor,
                onOpenDevFire = onOpenDevFire,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) {
                Text(text = "+", style = MaterialTheme.typography.headlineMedium)
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when {
                !loaded -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                catalog.alarms.isEmpty() -> Text(
                    text = stringResource(R.string.alarms_empty),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(catalog.alarms, key = { it.id.value }) { alarm ->
                        AlarmRow(
                            alarm = alarm,
                            onEdit = { editing = alarm },
                            onEnabledChange = { commit(catalog.setEnabled(alarm.id, it)) },
                        )
                    }
                }
            }
        }
    }

    if (creating) {
        AlarmEditorDialog(
            initial = null,
            onDismiss = { creating = false },
            onSave = { alarm ->
                creating = false
                commit(catalog.add(alarm))
            },
            onDelete = null,
        )
    }

    editing?.let { alarm ->
        AlarmEditorDialog(
            initial = alarm,
            onDismiss = { editing = null },
            onSave = { updated ->
                editing = null
                commit(catalog.update(updated))
            },
            onDelete = {
                editing = null
                commit(catalog.delete(alarm.id))
            },
        )
    }
}

/**
 * The Alarm list's navigation links and debug entry points, held in the
 * `topBar` so they occupy their own region. They never overlap the list because
 * the Scaffold reserves this height for them.
 */
@Composable
private fun AlarmListNavigation(
    missingRequirements: Set<ReliabilityRequirement>,
    devFireAlarmId: AlarmId?,
    onOpenReliability: (() -> Unit)?,
    onOpenDiagnosticLog: (() -> Unit)?,
    onOpenStatistics: (() -> Unit)?,
    onOpenSettings: (() -> Unit)?,
    onOpenDevChallenge: (() -> Unit)?,
    onOpenDevAnchor: (() -> Unit)?,
    onOpenDevFire: ((AlarmId) -> Unit)?,
) {
    val hasNavigation = onOpenReliability != null ||
        onOpenDiagnosticLog != null ||
        onOpenStatistics != null ||
        onOpenSettings != null
    val hasDevelopment = onOpenDevChallenge != null ||
        onOpenDevAnchor != null ||
        (onOpenDevFire != null && devFireAlarmId != null)
    if (!hasNavigation && !hasDevelopment) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start,
        ) {
            if (missingRequirements.isNotEmpty() && onOpenReliability != null) {
                TextButton(onClick = onOpenReliability) {
                    Text(
                        text = stringResource(
                            R.string.reliability_warning,
                            missingRequirements.size,
                        ),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (onOpenDiagnosticLog != null) {
                TextButton(onClick = onOpenDiagnosticLog) {
                    Text(text = stringResource(R.string.nav_diagnostic_log))
                }
            }
            if (onOpenStatistics != null) {
                TextButton(onClick = onOpenStatistics) {
                    Text(text = stringResource(R.string.nav_statistics))
                }
            }
            if (onOpenSettings != null) {
                TextButton(onClick = onOpenSettings) {
                    Text(text = stringResource(R.string.nav_settings))
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            if (onOpenDevChallenge != null) {
                TextButton(onClick = onOpenDevChallenge) {
                    Text(text = stringResource(R.string.dev_dismiss_challenge))
                }
            }
            if (onOpenDevAnchor != null) {
                TextButton(onClick = onOpenDevAnchor) {
                    Text(text = stringResource(R.string.dev_physical_anchor))
                }
            }
            if (onOpenDevFire != null && devFireAlarmId != null) {
                TextButton(onClick = { onOpenDevFire(devFireAlarmId) }) {
                    Text(text = stringResource(R.string.dev_fire_alarm))
                }
            }
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: Alarm,
    onEdit: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = formatTime(alarm.time), style = MaterialTheme.typography.headlineSmall)
            Text(text = formatRepeatDays(alarm.repeatDays), style = MaterialTheme.typography.bodyMedium)
            if (alarm.silentMode) {
                Text(
                    text = stringResource(R.string.alarm_silent_mode),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (alarm.defaultDifficulty > Alarm.DEFAULT_DIFFICULTY) {
                Text(
                    text = stringResource(
                        R.string.alarm_starts_at_difficulty,
                        alarm.defaultDifficulty,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Switch(checked = alarm.enabled, onCheckedChange = onEnabledChange)
    }
    HorizontalDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlarmEditorDialog(
    initial: Alarm?,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val timePicker = rememberTimePickerState(
        initialHour = initial?.time?.hour ?: 7,
        initialMinute = initial?.time?.minute ?: 0,
        is24Hour = true,
    )
    var repeatDays by remember { mutableStateOf(initial?.repeatDays ?: emptySet()) }
    var enabled by remember { mutableStateOf(initial?.enabled ?: true) }
    // Silent Mode is off by default: a normal Alarm still rings.
    var silentMode by remember { mutableStateOf(initial?.silentMode ?: false) }
    // The difficulty Escalation starts from, chosen in advance; the gentlest by
    // default. It is set here, never during an Alarm.
    var defaultDifficulty by remember {
        mutableStateOf(initial?.defaultDifficulty ?: Alarm.DEFAULT_DIFFICULTY)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (initial == null) R.string.alarm_editor_title_new
                    else R.string.alarm_editor_title_edit,
                ),
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TimePicker(state = timePicker)
                Text(
                    text = stringResource(R.string.alarm_editor_repeat),
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = day in repeatDays,
                            onClick = {
                                repeatDays = if (day in repeatDays) repeatDays - day else repeatDays + day
                            },
                            label = { Text(text = shortDayName(day)) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.alarm_editor_enabled),
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.alarm_editor_silent_mode),
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = silentMode, onCheckedChange = { silentMode = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.alarm_editor_default_difficulty),
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        enabled = defaultDifficulty > Alarm.DEFAULT_DIFFICULTY_RANGE.first,
                        onClick = { defaultDifficulty-- },
                    ) { Text(text = "−") }
                    Text(text = "$defaultDifficulty")
                    TextButton(
                        enabled = defaultDifficulty < Alarm.DEFAULT_DIFFICULTY_RANGE.last,
                        onClick = { defaultDifficulty++ },
                    ) { Text(text = "+") }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = repeatDays.isNotEmpty(),
                onClick = {
                    onSave(
                        Alarm(
                            id = initial?.id ?: AlarmId(UUID.randomUUID().toString()),
                            time = AlarmTime(timePicker.hour, timePicker.minute),
                            repeatDays = repeatDays,
                            enabled = enabled,
                            silentMode = silentMode,
                            defaultDifficulty = defaultDifficulty,
                        ),
                    )
                },
            ) { Text(text = stringResource(R.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(text = stringResource(R.string.action_delete))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        },
    )
}

private fun formatTime(time: AlarmTime): String = "%02d:%02d".format(time.hour, time.minute)

@Composable
private fun formatRepeatDays(days: Set<DayOfWeek>): String {
    if (days.size == DayOfWeek.entries.size) return stringResource(R.string.alarm_repeat_every_day)
    val separator = stringResource(R.string.list_separator)
    val names = mutableListOf<String>()
    for (day in days.sortedBy { it.value }) {
        names += shortDayName(day)
    }
    return names.joinToString(separator)
}

@Composable
private fun shortDayName(day: DayOfWeek): String = stringResource(
    when (day) {
        DayOfWeek.MONDAY -> R.string.day_mon
        DayOfWeek.TUESDAY -> R.string.day_tue
        DayOfWeek.WEDNESDAY -> R.string.day_wed
        DayOfWeek.THURSDAY -> R.string.day_thu
        DayOfWeek.FRIDAY -> R.string.day_fri
        DayOfWeek.SATURDAY -> R.string.day_sat
        DayOfWeek.SUNDAY -> R.string.day_sun
    },
)
