package com.misaka9981.alarm.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.misaka9981.alarm.R
import com.misaka9981.alarm.core.AnchorScanResult
import com.misaka9981.alarm.core.DismissalState
import com.misaka9981.alarm.core.FiringState

/**
 * The screen shown while an Alarm is firing, over the lock screen.
 *
 * It renders only what [com.misaka9981.alarm.core.FiringSession] reports: the
 * Dismiss Challenge and the Physical Anchor, both of which are required before
 * the Alarm is silenced, and whether the sound cap has stopped the signalling.
 * There is no Snooze control anywhere — see `CONTEXT.md` and ADR-0002. Rendering
 * only; every decision lives in `core`.
 *
 * [silentMode] is the Alarm's Silent Mode setting, rendered so the owner can see
 * that this Alarm signals by vibration only. It changes nothing else on this
 * screen: the Dismiss Challenge and the Physical Anchor are identical.
 *
 * The Escape Hatch is deliberately not a visible button: it is reached by
 * long-pressing the Alarm title, which reveals the password prompt. Whether both
 * gestures have been made, and whether the password is correct, is decided by
 * `core`; this screen only reports them.
 */
@Composable
fun FiringScreen(
    state: FiringState,
    alarmLabel: String,
    anchorLabel: String?,
    typed: String,
    scanning: Boolean,
    onTypedChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onScanAnchor: () -> Unit,
    onEscapeHatchLongPress: () -> Unit,
    onEscapeHatchPassword: (String) -> Unit,
    onEscapeHatchCancel: () -> Unit,
    modifier: Modifier = Modifier,
    silentMode: Boolean = false,
) {
    val ringing = state as? FiringState.Ringing ?: return
    val challenge = ringing.challenge

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.firing_alarm_label, alarmLabel),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onLongPress = { onEscapeHatchLongPress() })
            },
        )

        if (silentMode) {
            Text(
                text = stringResource(R.string.firing_silent_mode),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (ringing.capExpired) {
            Text(
                text = stringResource(R.string.firing_cap_expired),
                color = MaterialTheme.colorScheme.error,
            )
        }

        when (challenge) {
            is DismissalState.Ongoing -> {
                Text(
                    text = stringResource(
                        R.string.firing_challenge_progress,
                        challenge.difficulty,
                        challenge.elapsed.inWholeSeconds.toInt(),
                        challenge.wrongAnswers,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(text = challenge.challenge.problem, style = MaterialTheme.typography.displaySmall)
                challenge.feedback?.let {
                    Text(
                        text = stringResource(R.string.wrong_answer_hint),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                OutlinedTextField(
                    value = typed,
                    onValueChange = onTypedChange,
                    label = { Text(text = stringResource(R.string.label_your_answer)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.action_submit))
                }
            }

            is DismissalState.Dismissed -> Text(
                text = stringResource(R.string.dismiss_challenge_solved),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Text(text = stringResource(R.string.anchor_title), style = MaterialTheme.typography.titleSmall)
        if (ringing.anchorReached) {
            Text(
                text = anchorLabel
                    ?.let { stringResource(R.string.anchor_reached_named, it) }
                    ?: stringResource(R.string.anchor_reached),
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Text(
                text = anchorLabel
                    ?.let { stringResource(R.string.anchor_reach_named, it) }
                    ?: stringResource(R.string.anchor_none_bound),
            )
            ringing.anchorFeedback?.let { feedback ->
                Text(
                    text = if (feedback is AnchorScanResult.NotReached && feedback.scanned == null) {
                        stringResource(R.string.status_scan_cancelled)
                    } else {
                        stringResource(R.string.anchor_not_bound)
                    },
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = onScanAnchor,
                enabled = anchorLabel != null && !scanning,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (scanning) stringResource(R.string.status_scanning)
                    else stringResource(R.string.anchor_scan),
                )
            }
        }
    }

    if (ringing.escapeHatchRevealed) {
        EscapeHatchPasswordDialog(
            onUnlock = onEscapeHatchPassword,
            onCancel = onEscapeHatchCancel,
        )
    }
}

/**
 * The password prompt the hidden long-press reveals. It is the second of the two
 * gestures the Escape Hatch requires; a wrong password leaves the Alarm ringing.
 */
@Composable
private fun EscapeHatchPasswordDialog(
    onUnlock: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = stringResource(R.string.escape_hatch_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(R.string.escape_hatch_description))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(text = stringResource(R.string.label_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = password.isNotBlank(),
                onClick = {
                    onUnlock(password)
                    password = ""
                },
            ) { Text(text = stringResource(R.string.escape_hatch_force_silence)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(text = stringResource(R.string.action_cancel)) }
        },
    )
}
