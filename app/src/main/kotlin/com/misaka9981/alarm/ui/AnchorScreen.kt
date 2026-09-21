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
import androidx.compose.ui.unit.dp
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Physical Anchor", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Scan a real-world object away from the bed to set it, then scan " +
                "again to prove reaching it.",
            style = MaterialTheme.typography.bodySmall,
        )

        if (!loaded) {
            CircularProgressIndicator()
            return@Column
        }

        if (alarms.isEmpty()) {
            Text(text = "Create an Alarm first.")
            TextButton(onClick = onClose) { Text(text = "Close") }
            return@Column
        }

        Text(text = "Alarm", style = MaterialTheme.typography.titleSmall)
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
                    status = "Scanning…"
                    val payload = scanner.scan()
                    if (payload == null) {
                        status = "Scan cancelled."
                        return@launch
                    }
                    val code = AnchorCode(payload)
                    val label = catalog.findByCode(code)?.label ?: anchorLabelFor(payload)
                    val next = catalog
                        .set(PhysicalAnchor(code = code, label = label))
                        .bind(alarmId, code)
                    commit(next)
                    status = "Set \"$label\" and bound it to ${alarmName(alarms, alarmId)}."
                }
            },
            enabled = alarmId != null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(text = "Set Physical Anchor from scan") }

        Button(
            onClick = {
                if (alarmId == null) return@Button
                scope.launch {
                    status = "Scanning…"
                    status = when (val result = catalog.gateFor(alarmId, scanner).scan()) {
                        is AnchorScanResult.Reached -> "Anchor reached."
                        is AnchorScanResult.NotReached ->
                            if (result.scanned == null) {
                                "Scan cancelled."
                            } else {
                                "Not the bound Physical Anchor."
                            }
                    }
                }
            },
            enabled = alarmId != null && catalog.anchorFor(alarmId) != null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(text = "Reach Anchor from scan") }

        status?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }

        HorizontalDivider()
        Text(text = "Anchors", style = MaterialTheme.typography.titleSmall)
        if (catalog.anchors.isEmpty()) {
            Text(text = "None yet.")
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

        TextButton(onClick = onClose) { Text(text = "Close (development entry point)") }
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
                    text = "Code: ${anchor.code.value.take(32)}" +
                        if (anchor.code.value.length > 32) "…" else "",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = if (boundAlarms.isEmpty()) {
                        "Not bound to an Alarm"
                    } else {
                        "Bound to " + boundAlarms.joinToString(", ") { formatAnchorAlarm(it) }
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onDelete) { Text(text = "Delete") }
        }
        HorizontalDivider()
    }
}

private fun anchorLabelFor(payload: String): String {
    val cleaned = payload.filterNot { it.isWhitespace() || it == '|' }.take(8)
    return if (cleaned.isEmpty()) "Anchor" else "Anchor $cleaned"
}

private fun formatAnchorAlarm(alarm: Alarm): String = "%02d:%02d".format(alarm.time.hour, alarm.time.minute)

private fun alarmName(alarms: List<Alarm>, id: AlarmId): String =
    alarms.firstOrNull { it.id == id }?.let(::formatAnchorAlarm) ?: id.value
