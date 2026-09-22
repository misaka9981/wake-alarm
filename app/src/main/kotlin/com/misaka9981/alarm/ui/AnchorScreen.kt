package com.misaka9981.alarm.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.misaka9981.alarm.R
import com.misaka9981.alarm.core.Alarm
import com.misaka9981.alarm.core.AlarmId
import com.misaka9981.alarm.core.AlarmRepository
import com.misaka9981.alarm.core.AnchorCatalog
import com.misaka9981.alarm.core.AnchorCode
import com.misaka9981.alarm.core.AnchorRepository
import com.misaka9981.alarm.core.AnchorScanResult
import com.misaka9981.alarm.core.AnchorScanner
import com.misaka9981.alarm.core.PhysicalAnchor
import kotlinx.coroutines.launch

/**
 * Development entry point for the Physical Anchor flow: pick an Alarm, scan a
 * real-world object to set and bind an anchor, then scan again to prove that a
 * wrong object fails and the bound one is reached.
 *
 * This screen only renders and owns the platform concerns — loading the
 * configuration and driving the camera port. Every rule about what an anchor is,
 * how it binds to an Alarm, and whether a scan reached it lives in `core`.
 */
@Composable
fun AnchorScreen(
    alarmRepository: AlarmRepository,
    anchorRepository: AnchorRepository,
    scanner: AnchorScanner,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var alarms by remember { mutableStateOf<List<Alarm>>(emptyList()) }
    var catalog by remember { mutableStateOf(AnchorCatalog.empty) }
    var loaded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<AlarmId?>(null) }
    var status by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(alarmRepository, anchorRepository) {
        alarms = alarmRepository.load()
        catalog = anchorRepository.load()
        selected = alarms.firstOrNull()?.id
        loaded = true
    }

    suspend fun commit(next: AnchorCatalog) {
        catalog = next
        anchorRepository.save(next)
    }

    val anchorPrefix = stringResource(R.string.anchor_default_label)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { WakeAlarmTopBar(stringResource(R.string.anchor_title), onClose) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.anchor_dev_hint),
                style = MaterialTheme.typography.bodySmall,
            )

            if (!loaded) {
                CircularProgressIndicator()
                return@Column
            }

            if (alarms.isEmpty()) {
                Text(text = stringResource(R.string.anchor_create_alarm_first))
                return@Column
            }

            Text(text = stringResource(R.string.anchor_alarm_label), style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                alarms.forEach { alarm ->
                    FilterChip(
                        selected = alarm.id == selected,
                        onClick = {
                            selected = alarm.id
                            status = null
                        },
                        label = { Text(text = formatAnchorAlarm(alarm)) },
                    )
                }
            }

            val alarmId = selected
            Button(
                onClick = {
                    if (alarmId == null) return@Button
                    scope.launch {
                        status = context.getString(R.string.status_scanning)
                        val payload = scanner.scan()
                        if (payload == null) {
                            status = context.getString(R.string.status_scan_cancelled)
                            return@launch
                        }
                        val code = AnchorCode(payload)
                        val label = catalog.findByCode(code)?.label
                            ?: anchorLabelFor(payload, anchorPrefix)
                        val next = catalog
                            .set(PhysicalAnchor(code = code, label = label))
                            .bind(alarmId, code)
                        commit(next)
                        status = context.getString(
                            R.string.anchor_set_status,
                            label,
                            alarmName(alarms, alarmId),
                        )
                    }
                },
                enabled = alarmId != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(text = stringResource(R.string.anchor_set_from_scan)) }

            Button(
                onClick = {
                    if (alarmId == null) return@Button
                    scope.launch {
                        status = context.getString(R.string.status_scanning)
                        status = when (val result = catalog.gateFor(alarmId, scanner).scan()) {
                            is AnchorScanResult.Reached ->
                                context.getString(R.string.anchor_reached_status)
                            is AnchorScanResult.NotReached ->
                                if (result.scanned == null) {
                                    context.getString(R.string.status_scan_cancelled)
                                } else {
                                    context.getString(R.string.anchor_not_bound)
                                }
                        }
                    }
                },
                enabled = alarmId != null && catalog.anchorFor(alarmId) != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(text = stringResource(R.string.anchor_reach_from_scan)) }

            status?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }

            HorizontalDivider()
            Text(text = stringResource(R.string.anchor_list_title), style = MaterialTheme.typography.titleSmall)
            if (catalog.anchors.isEmpty()) {
                Text(text = stringResource(R.string.anchor_list_empty))
            } else {
                catalog.anchors.forEach { anchor ->
                    AnchorRow(
                        anchor = anchor,
                        boundAlarms = catalog.bindings
                            .filterValues { it == anchor.code }
                            .keys
                            .mapNotNull { id -> alarms.firstOrNull { it.id == id } },
                        onDelete = { scope.launch { commit(catalog.delete(anchor.code)) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun AnchorRow(
    anchor: PhysicalAnchor,
    boundAlarms: List<Alarm>,
    onDelete: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = anchor.label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(
                        R.string.anchor_code,
                        anchor.code.value.take(32) +
                            if (anchor.code.value.length > 32) "…" else "",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = if (boundAlarms.isEmpty()) {
                        stringResource(R.string.anchor_not_bound_to_alarm)
                    } else {
                        stringResource(
                            R.string.anchor_bound_to,
                            boundAlarms.joinToString(
                                stringResource(R.string.list_separator),
                            ) { formatAnchorAlarm(it) },
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onDelete) { Text(text = stringResource(R.string.action_delete)) }
        }
        HorizontalDivider()
    }
}

internal fun anchorLabelFor(payload: String, defaultLabel: String): String {
    val cleaned = payload.filterNot { it.isWhitespace() || it == '|' }.take(8)
    return if (cleaned.isEmpty()) defaultLabel else "$defaultLabel $cleaned"
}

private fun formatAnchorAlarm(alarm: Alarm): String = "%02d:%02d".format(alarm.time.hour, alarm.time.minute)

private fun alarmName(alarms: List<Alarm>, id: AlarmId): String =
    alarms.firstOrNull { it.id == id }?.let(::formatAnchorAlarm) ?: id.value
