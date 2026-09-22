package com.misaka9981.alarm.ui

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.misaka9981.alarm.BuildConfig
import com.misaka9981.alarm.core.AlarmRepository
import com.misaka9981.alarm.core.AnchorRepository
import com.misaka9981.alarm.core.AnchorScanner
import com.misaka9981.alarm.core.EscapeHatchRepository
import com.misaka9981.alarm.core.Onboarding
import com.misaka9981.alarm.core.ReliabilityCheck
import com.misaka9981.alarm.reliability.AndroidReliabilityGrants
import com.misaka9981.alarm.schedule.AlarmScheduler

/** The screens the app can show. */
private enum class Screen {
    Alarms,
    Reliability,
    Challenge,
    Anchor,
    DiagnosticLog,
    Statistics,
    Settings,
}

/**
 * Hosts the app's screens and, in debug builds only, the development entry
 * points to the Dismiss Challenge, the Physical Anchor flow, and the end-to-end
 * firing flow.
 *
 * Before anything else it decides, in `core` ([Onboarding]), whether the owner
 * has finished first-run setup. Until the wake time, the Physical Anchor, and
 * the Escape Hatch password are all in place, the onboarding flow is shown and
 * the rest of the app is withheld, so an installed app cannot be mistaken for a
 * usable one.
 *
 * It also decides whether to warn about missing reliability guarantees: it reads
 * the platform grants (a thin adapter) and renders the warning
 * [com.misaka9981.alarm.core.ReliabilityCheck] reports.
 */
@Composable
fun WakeAlarmRoot(
    alarmRepository: AlarmRepository,
    anchorRepository: AnchorRepository,
    escapeHatchRepository: EscapeHatchRepository,
    scanner: AnchorScanner,
    scheduler: AlarmScheduler,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var screen by remember { mutableStateOf(Screen.Alarms) }
    var reliabilityRefreshKey by remember { mutableStateOf(0) }
    var onboarding by remember { mutableStateOf<Onboarding?>(null) }
    var onboardingRefreshKey by remember { mutableStateOf(0) }
    val missingRequirements = remember(reliabilityRefreshKey) {
        ReliabilityCheck(Build.VERSION.SDK_INT).missing(AndroidReliabilityGrants.read(context))
    }
    val developmentEntryPoints = BuildConfig.DEBUG

    LaunchedEffect(onboardingRefreshKey) {
        onboarding = Onboarding.of(
            alarms = alarmRepository.load(),
            anchors = anchorRepository.load(),
            password = escapeHatchRepository.load(),
        )
    }

    when (val current = onboarding) {
        null -> Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        else -> if (!current.isComplete) {
            OnboardingScreen(
                onboarding = current,
                alarmRepository = alarmRepository,
                anchorRepository = anchorRepository,
                escapeHatchRepository = escapeHatchRepository,
                scanner = scanner,
                scheduler = scheduler,
                onComplete = {
                    onboarding = null
                    onboardingRefreshKey++
                    screen = Screen.Alarms
                },
                modifier = modifier,
            )
        } else {
            when (screen) {
                Screen.Alarms -> AlarmListScreen(
                    repository = alarmRepository,
                    scheduler = scheduler,
                    modifier = modifier,
                    missingRequirements = missingRequirements,
                    onOpenReliability = { screen = Screen.Reliability },
                    onOpenDiagnosticLog = { screen = Screen.DiagnosticLog },
                    onOpenStatistics = { screen = Screen.Statistics },
                    onOpenSettings = { screen = Screen.Settings },
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
                    onOpenDevFire = if (developmentEntryPoints) {
                        { alarmId ->
                            context.startActivity(
                                AlarmFiringActivity.intent(context, alarmId.value),
                            )
                        }
                    } else {
                        null
                    },
                )

                Screen.Reliability -> ReliabilityScreen(
                    onClose = {
                        reliabilityRefreshKey++
                        screen = Screen.Alarms
                    },
                    modifier = modifier,
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

                Screen.DiagnosticLog -> DiagnosticLogScreen(
                    onClose = { screen = Screen.Alarms },
                    modifier = modifier,
                )

                Screen.Statistics -> StatisticsScreen(
                    onClose = { screen = Screen.Alarms },
                    modifier = modifier,
                )

                Screen.Settings -> SettingsScreen(
                    alarmRepository = alarmRepository,
                    anchorRepository = anchorRepository,
                    escapeHatchRepository = escapeHatchRepository,
                    scanner = scanner,
                    onClose = { screen = Screen.Alarms },
                    modifier = modifier,
                )
            }
        }
    }
}
