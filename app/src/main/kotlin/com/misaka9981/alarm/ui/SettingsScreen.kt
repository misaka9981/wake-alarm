package com.misaka9981.alarm.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.misaka9981.alarm.BuildConfig
import com.misaka9981.alarm.R
import com.misaka9981.alarm.core.Alarm
import com.misaka9981.alarm.core.AlarmCatalog
import com.misaka9981.alarm.core.AlarmRepository
import com.misaka9981.alarm.core.AnchorCatalog
import com.misaka9981.alarm.core.AnchorCode
import com.misaka9981.alarm.core.AnchorRepository
import com.misaka9981.alarm.core.AnchorScanner
import com.misaka9981.alarm.core.AppLanguage
import com.misaka9981.alarm.core.EscapeHatchPassword
import com.misaka9981.alarm.core.EscapeHatchRepository
import com.misaka9981.alarm.core.PhysicalAnchor
import kotlinx.coroutines.launch

/**
 * Settings for the values first-run setup established: the wake time, the
 * Physical Anchor, and the Escape Hatch password. The owner can change any of
 * them later.
 *
 * Rendering and storage only. Which Alarm is the wake time, what counts as a
 * bound anchor, and whether a password is acceptable are decided in `core`.
 */
@Composable
fun SettingsScreen(
    alarmRepository: AlarmRepository,
    anchorRepository: AnchorRepository,
    escapeHatchRepository: EscapeHatchRepository,
    scanner: AnchorScanner,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var alarms by remember { mutableStateOf<List<Alarm>>(emptyList()) }
    var anchors by remember { mutableStateOf(AnchorCatalog.empty) }
    var passwordSet by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }
    var changingPassword by remember { mutableStateOf(false) }
    var anchorStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshKey) {
        alarms = alarmRepository.load()
        anchors = anchorRepository.load()
        passwordSet = escapeHatchRepository.load() != null
        loaded = true
    }

    // The earliest Alarm is the wake time setup created and the one settings
    // changes; `AlarmCatalog` orders the same way.
    val primaryAlarm = AlarmCatalog.of(alarms).alarms.firstOrNull()
    val boundAnchor = primaryAlarm?.let { anchors.anchorFor(it.id) }
    val anchorPrefix = stringResource(R.string.anchor_default_label)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { WakeAlarmTopBar(stringResource(R.string.settings_title), onClose) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!loaded) {
                CircularProgressIndicator()
                return@Column
            }

            Section(title = stringResource(R.string.settings_wake_time))
            Text(
                text = primaryAlarm?.let { stringResource(R.string.settings_alarm_at, formatSettingsTime(it)) }
                    ?: stringResource(R.string.settings_no_alarm),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = { primaryAlarm?.let { editingAlarm = it } },
                enabled = primaryAlarm != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(text = stringResource(R.string.settings_change_wake_time)) }

            HorizontalDivider()
            Section(title = stringResource(R.string.settings_physical_anchor))
            Text(
                text = boundAnchor?.let { stringResource(R.string.settings_anchor_bound, it.label) }
                    ?: stringResource(R.string.settings_anchor_none),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = {
                    val alarmId = primaryAlarm?.id ?: return@Button
                    scope.launch {
                        anchorStatus = context.getString(R.string.status_scanning)
                        val payload = scanner.scan()
                        if (payload == null) {
                            anchorStatus = context.getString(R.string.status_scan_cancelled)
                            return@launch
                        }
                        val code = AnchorCode(payload)
                        val label = anchors.findByCode(code)?.label
                            ?: anchorLabelFor(payload, anchorPrefix)
                        anchorRepository.save(
                            anchors
                                .set(PhysicalAnchor(code = code, label = label))
                                .bind(alarmId, code),
                        )
                        anchorStatus = context.getString(R.string.onboarding_anchor_bound, label)
                        refreshKey++
                    }
                },
                enabled = primaryAlarm != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(text = stringResource(R.string.settings_change_anchor)) }
            anchorStatus?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }

            HorizontalDivider()
            Section(title = stringResource(R.string.settings_escape_hatch_password))
            Text(
                text = if (passwordSet) stringResource(R.string.settings_password_set)
                else stringResource(R.string.settings_password_none),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = { changingPassword = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(text = stringResource(R.string.settings_change_password)) }

            HorizontalDivider()
            Section(title = stringResource(R.string.settings_language))
            val language = LocalAppLanguage.current
            val setLanguage = LocalSetAppLanguage.current
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguageChip(R.string.language_system, language == AppLanguage.System) {
                    setLanguage(AppLanguage.System)
                }
                LanguageChip(R.string.language_chinese, language == AppLanguage.Chinese) {
                    setLanguage(AppLanguage.Chinese)
                }
                LanguageChip(R.string.language_english, language == AppLanguage.English) {
                    setLanguage(AppLanguage.English)
                }
            }

            HorizontalDivider()
            Section(title = stringResource(R.string.settings_build))
            Text(
                text = stringResource(
                    R.string.settings_build_value,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.BUILD_IDENTIFIER,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    editingAlarm?.let { alarm ->
        AlarmEditorDialog(
            initial = alarm,
            onDismiss = { editingAlarm = null },
            onSave = { updated ->
                editingAlarm = null
                scope.launch {
                    alarmRepository.save(AlarmCatalog.of(alarms).update(updated).alarms)
                    refreshKey++
                }
            },
            onDelete = null,
        )
    }

    if (changingPassword) {
        ChangePasswordDialog(
            onDismiss = { changingPassword = false },
            onSave = { password ->
                changingPassword = false
                scope.launch {
                    escapeHatchRepository.save(password)
                    refreshKey++
                }
            },
        )
    }
}

@Composable
private fun Section(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleMedium)
}

/** One choice in the in-app language selector. */
@Composable
private fun LanguageChip(@StringRes labelRes: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = stringResource(labelRes)) },
    )
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSave: (EscapeHatchPassword) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_escape_hatch_password)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(text = stringResource(R.string.change_password_new)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    label = { Text(text = stringResource(R.string.change_password_confirm)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        password.isBlank() ->
                            error = context.getString(R.string.onboarding_password_blank)
                        password != confirmation ->
                            error = context.getString(R.string.onboarding_password_mismatch)
                        else -> onSave(EscapeHatchPassword(password))
                    }
                },
            ) { Text(text = stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.action_cancel)) }
        },
    )
}

private fun formatSettingsTime(alarm: Alarm): String =
    "%02d:%02d".format(alarm.time.hour, alarm.time.minute)
