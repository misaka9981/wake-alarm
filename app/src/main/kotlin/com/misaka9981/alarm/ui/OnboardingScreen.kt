package com.misaka9981.alarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
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
import com.misaka9981.alarm.R
import com.misaka9981.alarm.core.AlarmId
import com.misaka9981.alarm.core.AlarmRepository
import com.misaka9981.alarm.core.AlarmTime
import com.misaka9981.alarm.core.AnchorCode
import com.misaka9981.alarm.core.AnchorRepository
import com.misaka9981.alarm.core.AnchorScanner
import com.misaka9981.alarm.core.EscapeHatchPassword
import com.misaka9981.alarm.core.EscapeHatchRepository
import com.misaka9981.alarm.core.Onboarding
import com.misaka9981.alarm.core.OnboardingStep
import com.misaka9981.alarm.core.PhysicalAnchor
import com.misaka9981.alarm.schedule.AlarmScheduler
import java.util.UUID
import kotlinx.coroutines.launch

/**
 * The first-run flow: set the wake time (which creates the first Alarm), bind a
 * Physical Anchor away from the bed, and set the private Escape Hatch password.
 *
 * This screen only renders and drives the platform concerns — loading the
 * configuration, the camera, and storage. Every rule about what an Alarm is,
 * what counts as a bound anchor, and whether setup is complete lives in `core`
 * ([Onboarding]); the step to resume at comes from its [Onboarding.missingSteps].
 * There is no Snooze anywhere in the flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onboarding: Onboarding,
    alarmRepository: AlarmRepository,
    anchorRepository: AnchorRepository,
    escapeHatchRepository: EscapeHatchRepository,
    scanner: AnchorScanner,
    scheduler: AlarmScheduler,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var step by remember {
        mutableStateOf(onboarding.missingSteps.firstOrNull() ?: OnboardingStep.WakeTime)
    }
    var firstAlarmId by remember { mutableStateOf(onboarding.firstAlarm?.id) }
    var anchorStatus by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var passwordStatus by remember { mutableStateOf<String?>(null) }
    val timePicker = rememberTimePickerState(
        initialHour = onboarding.firstAlarm?.time?.hour ?: 7,
        initialMinute = onboarding.firstAlarm?.time?.minute ?: 0,
        is24Hour = true,
    )
    val anchorPrefix = stringResource(R.string.anchor_default_label)
    val scanningLabel = stringResource(R.string.status_scanning)
    val scanCancelledLabel = stringResource(R.string.status_scan_cancelled)
    val passwordBlankLabel = stringResource(R.string.onboarding_password_blank)
    val passwordMismatchLabel = stringResource(R.string.onboarding_password_mismatch)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.onboarding_welcome),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.onboarding_intro),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(text = stepProgress(step), style = MaterialTheme.typography.labelLarge)

        when (step) {
            OnboardingStep.WakeTime -> {
                Text(
                    text = stringResource(R.string.onboarding_wake_time),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.onboarding_wake_time_detail),
                    style = MaterialTheme.typography.bodySmall,
                )
                TimePicker(state = timePicker)
                Button(
                    onClick = {
                        val alarm = Onboarding.firstAlarm(
                            id = AlarmId(UUID.randomUUID().toString()),
                            time = AlarmTime(timePicker.hour, timePicker.minute),
                        )
                        scope.launch {
                            alarmRepository.save(listOf(alarm))
                            scheduler.reschedule(listOf(alarm))
                            firstAlarmId = alarm.id
                            step = OnboardingStep.PhysicalAnchor
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(text = stringResource(R.string.onboarding_set_wake_time)) }
            }

            OnboardingStep.PhysicalAnchor -> {
                Text(
                    text = stringResource(R.string.anchor_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.onboarding_anchor_detail),
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    onClick = {
                        val alarmId = firstAlarmId ?: return@Button
                        scope.launch {
                            anchorStatus = scanningLabel
                            val payload = scanner.scan()
                            if (payload == null) {
                                anchorStatus = scanCancelledLabel
                                return@launch
                            }
                            val code = AnchorCode(payload)
                            val catalog = anchorRepository.load()
                            val label = catalog.findByCode(code)?.label
                                ?: anchorLabelFor(payload, anchorPrefix)
                            anchorRepository.save(
                                catalog
                                    .set(PhysicalAnchor(code = code, label = label))
                                    .bind(alarmId, code),
                            )
                            anchorStatus = context.getString(R.string.onboarding_anchor_bound, label)
                            step = OnboardingStep.EscapeHatchPassword
                        }
                    },
                    enabled = firstAlarmId != null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(text = stringResource(R.string.anchor_scan)) }
                anchorStatus?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
            }

            OnboardingStep.EscapeHatchPassword -> {
                Text(
                    text = stringResource(R.string.onboarding_escape_hatch_password),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.onboarding_escape_hatch_detail),
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(text = stringResource(R.string.label_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    label = { Text(text = stringResource(R.string.onboarding_confirm_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        when {
                            password.isBlank() ->
                                passwordStatus = passwordBlankLabel

                            password != confirmation ->
                                passwordStatus = passwordMismatchLabel

                            else -> scope.launch {
                                escapeHatchRepository.save(EscapeHatchPassword(password))
                                onComplete()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(text = stringResource(R.string.onboarding_set_password_and_finish)) }
                passwordStatus?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
            }
        }

        HorizontalDivider()
        Text(
            text = stringResource(R.string.onboarding_footer),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun stepProgress(step: OnboardingStep): String {
    val number = step.ordinal + 1
    val total = OnboardingStep.entries.size
    return stringResource(R.string.onboarding_step_progress, number, total)
}
