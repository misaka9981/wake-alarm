package com.misaka9981.alarm.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.misaka9981.alarm.BuildConfig
import com.misaka9981.alarm.core.AlarmRepository
import com.misaka9981.alarm.core.AnchorRepository
import com.misaka9981.alarm.core.AnchorScanner

/** The screens the app can show. */
private enum class Screen { Alarms, Challenge, Anchor }

/**
 * Hosts the app's screens and, in debug builds only, the development entry
 * points to the Dismiss Challenge and the Physical Anchor flow.
 */
@Composable
fun WakeAlarmRoot(
    alarmRepository: AlarmRepository,
    anchorRepository: AnchorRepository,
    scanner: AnchorScanner,
    modifier: Modifier = Modifier,
) {
    var screen by remember { mutableStateOf(Screen.Alarms) }
    val developmentEntryPoints = BuildConfig.DEBUG

    when (screen) {
        Screen.Alarms -> AlarmListScreen(
            repository = alarmRepository,
            modifier = modifier,
            onOpenDevChallenge = if (developmentEntryPoints) {
                { screen = Screen.Challenge }
            } else {
                null
            },
            onOpenDevAnchor = if (developmentEntryPoints) {
                { screen = Screen.Anchor }
            } else {
                null
            },
        )

        Screen.Challenge -> DismissChallengeScreen(
            onClose = { screen = Screen.Alarms },
            modifier = modifier,
        )

        Screen.Anchor -> AnchorScreen(
            alarmRepository = alarmRepository,
            anchorRepository = anchorRepository,
            scanner = scanner,
            onClose = { screen = Screen.Alarms },
            modifier = modifier,
        )
    }
}
