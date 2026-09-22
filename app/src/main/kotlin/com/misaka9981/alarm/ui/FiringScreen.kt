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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
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
            text = "Alarm — $alarmLabel",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onLongPress = { onEscapeHatchLongPress() })
            },
        )

        if (silentMode) {
            Text(
                text = "Silent Mode — this Alarm signals by vibration only, with no sound. " +
                    "The Dismiss Challenge and Physical Anchor are unchanged.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (ringing.capExpired) {
            Text(
                text = "Signalling has stopped to keep this Alarm from ringing forever, " +
                    "but the Alarm is not cleared. Solve the Dismiss Challenge and reach " +
                    "the Physical Anchor to clear it.",
                color = MaterialTheme.colorScheme.error,
            )
        }

        when (challenge) {
            is DismissalState.Ongoing -> {
                Text(
                    text = "Dismiss Challenge · difficulty ${challenge.difficulty} · " +
                        "${challenge.elapsed.inWholeSeconds}s · ${challenge.wrongAnswers} wrong",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(text = challenge.challenge.problem, style = MaterialTheme.typography.displaySmall)
                challenge.feedback?.let {
                    Text(
                        text = "Wrong answer — the next challenge is harder.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                OutlinedTextField(
                    value = typed,
                    onValueChange = onTypedChange,
                    label = { Text(text = "Your answer") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Submit")
                }
            }

            is DismissalState.Dismissed -> Text(
                text = "Dismiss Challenge solved.",
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Text(text = "Physical Anchor", style = MaterialTheme.typography.titleSmall)
        if (ringing.anchorReached) {
            Text(
                text = anchorLabel?.let { "Anchor reached: $it." } ?: "Physical Anchor reached.",
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Text(
                text = anchorLabel
                    ?.let { "Reach your Physical Anchor: $it." }
                    ?: "No Physical Anchor is bound to this Alarm.",
            )
            ringing.anchorFeedback?.let { feedback ->
                Text(
                    text = if (feedback is AnchorScanResult.NotReached && feedback.scanned == null) {
                        "Scan cancelled."
                    } else {
                        "Not the bound Physical Anchor."
                    },
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = onScanAnchor,
                enabled = anchorLabel != null && !scanning,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = if (scanning) "Scanning…" else "Scan Physical Anchor")
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
        title = { Text(text = "Escape Hatch") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Force-silences the Alarm without the Dismiss Challenge or " +
                        "the Physical Anchor, and breaks the day's Streak. Only the " +
                        "correct password will silence it.",
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(text = "Password") },
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
            ) { Text(text = "Force-silence") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(text = "Cancel") }
        },
    )
}
