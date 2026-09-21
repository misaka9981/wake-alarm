package com.misaka9981.alarm.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.misaka9981.alarm.BuildConfig
import com.misaka9981.alarm.core.AlarmRepository

/**
 * Hosts the app's screens and, in debug builds only, the development entry point
 * to the Dismiss Challenge.
 */
@Composable
fun WakeAlarmRoot(repository: AlarmRepository, modifier: Modifier = Modifier) {
    var showChallenge by remember { mutableStateOf(false) }
    val openDevChallenge: (() -> Unit)? =
        if (BuildConfig.DEBUG) { { showChallenge = true } } else null

    if (showChallenge) {
        DismissChallengeScreen(onClose = { showChallenge = false }, modifier = modifier)
    } else {
        AlarmListScreen(
            repository = repository,
            modifier = modifier,
            onOpenDevChallenge = openDevChallenge,
        )
    }
}
