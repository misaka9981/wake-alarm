package com.misaka9981.alarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.misaka9981.alarm.R
import com.misaka9981.alarm.core.ArithmeticChallengeGenerator
import com.misaka9981.alarm.core.DismissalEvent
import com.misaka9981.alarm.core.DismissalSession
import com.misaka9981.alarm.core.DismissalState
import com.misaka9981.alarm.core.EscalationPolicy
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/**
 * Thin Android adapter that renders a [DismissalSession].
 *
 * It owns only the platform concerns — a clock to drive [DismissalEvent.Tick]
 * and text input — and renders whatever `core` reports. Every rule about how
 * hard the challenge is and when the session ends lives in `core`. There is no
 * Snooze anywhere.
 */
@Composable
fun DismissChallengeScreen(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val session = remember {
        DismissalSession.start(
            generator = ArithmeticChallengeGenerator(Random(System.currentTimeMillis())),
            policy = EscalationPolicy(),
        )
    }
    var state by remember { mutableStateOf(session.state) }
    var typed by remember { mutableStateOf("") }

    LaunchedEffect(session) {
        val startedAt = System.currentTimeMillis()
        while (state is DismissalState.Ongoing) {
            delay(1_000)
            val elapsed = (System.currentTimeMillis() - startedAt).milliseconds
            state = session.onEvent(DismissalEvent.Tick(elapsed))
        }
    }

    when (val current = state) {
        is DismissalState.Ongoing -> OngoingChallenge(
            state = current,
            typed = typed,
            onTypedChange = { typed = it },
            onSubmit = {
                state = session.onEvent(DismissalEvent.AnswerSubmitted(typed))
                typed = ""
            },
            onClose = onClose,
            modifier = modifier,
        )

        is DismissalState.Dismissed -> DismissedChallenge(onClose = onClose, modifier = modifier)
    }
}

@Composable
private fun OngoingChallenge(
    state: DismissalState.Ongoing,
    typed: String,
    onTypedChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.dismiss_challenge_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(
                R.string.dismiss_challenge_progress,
                state.difficulty,
                state.elapsed.inWholeSeconds.toInt(),
                state.wrongAnswers,
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(text = state.challenge.problem, style = MaterialTheme.typography.displaySmall)
        state.feedback?.let {
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
        TextButton(onClick = onClose) { Text(text = stringResource(R.string.action_dev_close)) }
    }
}

@Composable
private fun DismissedChallenge(onClose: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = stringResource(R.string.alarm_dismissed), style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onClose) { Text(text = stringResource(R.string.action_close)) }
    }
}
