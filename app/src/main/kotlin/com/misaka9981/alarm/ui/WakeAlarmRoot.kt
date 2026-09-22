package com.misaka9981.alarm.ui

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.misaka9981.alarm.BuildConfig
import com.misaka9981.alarm.core.AlarmRepository
import com.misaka9981.alarm.core.AnchorRepository
import com.misaka9981.alarm.core.AnchorScanner
import com.misaka9981.alarm.core.ReliabilityCheck
import com.misaka9981.alarm.reliability.AndroidReliabilityGrants
import com.misaka9981.alarm.schedule.AlarmScheduler

/** The screens the app can show. */
private enum class Screen { Alarms, Reliability, Challenge, Anchor, DiagnosticLog }

/**
 * Hosts the app's screens and, in debug builds only, the development entry
 * points to the Dismiss Challenge, the Physical Anchor flow, and the end-to-end
 * firing flow.
 *
 * It also decides whether to warn about missing reliability guarantees: it reads
 * the platform grants (a thin adapter) and renders the warning
 * [com.misaka9981.alarm.core.ReliabilityCheck] reports.
 */
@Composable
fun WakeAlarmRoot(
    alarmRepository: AlarmRepository,
    anchorRepository: AnchorRepository,
    scanner: AnchorScanner,
    scheduler: AlarmScheduler,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var screen by remember { mutableStateOf(Screen.Alarms) }
    var reliabilityRefreshKey by remember { mutableStateOf(0) }
    val missingRequirements = remember(reliabilityRefreshKey) {
        ReliabilityCheck(Build.VERSION.SDK_INT).missing(AndroidReliabilityGrants.read(context))
    }
    val developmentEntryPoints = BuildConfig.DEBUG

    when (screen) {
        Screen.Alarms -> AlarmListScreen(
            repository = alarmRepository,
            scheduler = scheduler,
            modifier = modifier,
            missingRequirements = missingRequirements,
            onOpenReliability = { screen = Screen.Reliability },
            onOpenDiagnosticLog = { screen = Screen.DiagnosticLog },
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
                { alarmId -> context.startActivity(AlarmFiringActivity.intent(context, alarmId.value)) }
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
    }
}
