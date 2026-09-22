package com.misaka9981.alarm.ui

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.misaka9981.alarm.R
import com.misaka9981.alarm.core.Alarm
import com.misaka9981.alarm.core.AlarmId
import com.misaka9981.alarm.core.AnchorGate
import com.misaka9981.alarm.core.ArithmeticChallengeGenerator
import com.misaka9981.alarm.core.DiagnosticEntry
import com.misaka9981.alarm.core.EscapeHatch
import com.misaka9981.alarm.core.EscapeHatchUse
import com.misaka9981.alarm.core.EscalationPolicy
import com.misaka9981.alarm.core.FiringEvent
import com.misaka9981.alarm.core.FiringOutcome
import com.misaka9981.alarm.core.FiringSession
import com.misaka9981.alarm.core.FiringState
import com.misaka9981.alarm.core.ReliabilityCheck
import com.misaka9981.alarm.core.ReliabilityRequirement
import com.misaka9981.alarm.data.AndroidAnchorScanner
import com.misaka9981.alarm.data.DataStoreAlarmRepository
import com.misaka9981.alarm.data.DataStoreAnchorRepository
import com.misaka9981.alarm.data.DataStoreDiagnosticLog
import com.misaka9981.alarm.data.DataStoreEscapeHatchLog
import com.misaka9981.alarm.data.DataStoreEscapeHatchRepository
import com.misaka9981.alarm.firing.AlarmNotification
import com.misaka9981.alarm.firing.AlarmReceiver
import com.misaka9981.alarm.firing.AlarmService
import com.misaka9981.alarm.reliability.AndroidReliabilityGrants
import com.misaka9981.alarm.ui.theme.WakeAlarmTheme
import java.time.Instant
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The firing screen, brought over the lock screen by the Alarm's full-screen
 * intent and kept awake while it rings.
 *
 * It owns only the platform concerns — showing over the lock screen, keeping the
 * screen on, loading the Alarm and its bound Physical Anchor, and driving the
 * camera and the clock. When [com.misaka9981.alarm.core.FiringSession] reports
 * dismissed, this activity stops the sound and vibration and finishes. Every
 * decision about dismissal lives in `core`. There is no Snooze anywhere.
 */
class AlarmFiringActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keepScreenOn()
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID)
        val scheduledAtMillis = intent.getLongExtra(EXTRA_SCHEDULED_AT, -1L)
        setContent {
            WakeAlarmTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (alarmId == null) {
                        MissingAlarm(onClose = ::endFiring)
                    } else {
                        FiringRoute(
                            alarmId = alarmId,
                            scheduledAtMillis = scheduledAtMillis,
                            onDismissed = ::endFiring,
                        )
                    }
                }
            }
        }
    }

    /** Stops the Alarm signalling and closes the firing screen. */
    private fun endFiring() {
        AlarmService.stop(this)
        AlarmNotification.cancel(this)
        finish()
    }

    private fun keepScreenOn() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
    }

    companion object {
        private const val EXTRA_ALARM_ID = "alarm_id"
        private const val EXTRA_SCHEDULED_AT = AlarmReceiver.EXTRA_SCHEDULED_AT

        /**
         * An Intent that fires [alarmId] on this screen. [scheduledAtMillis] is
         * the instant the Alarm was armed for, or `-1` when unknown, so the
         * Diagnostic Log can record the scheduled time.
         */
        fun intent(context: Context, alarmId: String, scheduledAtMillis: Long = -1L): Intent =
            Intent(context, AlarmFiringActivity::class.java)
                .putExtra(EXTRA_ALARM_ID, alarmId)
                .putExtra(EXTRA_SCHEDULED_AT, scheduledAtMillis)
    }
}

@Composable
private fun FiringRoute(
    alarmId: String,
    scheduledAtMillis: Long,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val alarmRepository = remember(context) { DataStoreAlarmRepository(context) }
    val anchorRepository = remember(context) { DataStoreAnchorRepository(context) }
    val scanner = remember(context) { AndroidAnchorScanner(context) }
    val escapeHatchRepository = remember(context) { DataStoreEscapeHatchRepository(context) }
    val escapeHatchLog = remember(context) { DataStoreEscapeHatchLog(context) }
    val diagnosticLog = remember(context) { DataStoreDiagnosticLog(context) }

    var alarm by remember { mutableStateOf<Alarm?>(null) }
    var anchorLabel by remember { mutableStateOf<String?>(null) }
    var session by remember { mutableStateOf<FiringSession?>(null) }
    var gate by remember { mutableStateOf<AnchorGate?>(null) }
    var state by remember { mutableStateOf<FiringState?>(null) }
    var typed by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    // Facts observed when the Alarm started firing, for the Diagnostic Log.
    var firedAt by remember { mutableStateOf(Instant.now()) }
    var missingRequirements by remember { mutableStateOf<Set<ReliabilityRequirement>>(emptySet()) }
    var signallingVolume by remember { mutableStateOf(0) }
    var recorded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    /**
     * Records how this firing ended, once. The Diagnostic Log survives the app
     * closing, so this is what makes a missed Alarm explainable later. It is
     * recorded before the screen closes; a firing already recorded as cap-expired
     * is not recorded again if it is later cleared.
     */
    suspend fun recordOutcome(outcome: FiringOutcome) {
        if (recorded) return
        recorded = true
        val started = session ?: return
        val summary = started.summary()
        diagnosticLog.record(
            DiagnosticEntry(
                alarmId = AlarmId(alarmId),
                scheduledTime = scheduledAtMillis.takeIf { it >= 0 }?.let { Instant.ofEpochMilli(it) },
                firedAt = firedAt,
                missingRequirements = missingRequirements,
                signallingVolume = signallingVolume,
                challengeDuration = summary.ringingDuration,
                wrongAnswers = summary.wrongAnswers,
                outcome = outcome,
            ),
        )
    }

    LaunchedEffect(alarmId) {
        val loadedAlarm = alarmRepository.load().firstOrNull { it.id == AlarmId(alarmId) }
        val catalog = anchorRepository.load()
        val escapeHatch = EscapeHatch.of(escapeHatchRepository.load())
        alarm = loadedAlarm
        if (loadedAlarm != null) {
            firedAt = Instant.now()
            missingRequirements = ReliabilityCheck(Build.VERSION.SDK_INT)
                .missing(AndroidReliabilityGrants.read(context))
            signallingVolume = alarmStreamVolumePercent(context)
            val started = FiringSession.start(
                generator = ArithmeticChallengeGenerator(Random(System.currentTimeMillis())),
                policy = EscalationPolicy(),
                // Escalation starts from the difficulty the owner chose the night
                // before; `core` decides how it climbs from there.
                baseDifficulty = loadedAlarm.defaultDifficulty,
                escapeHatch = escapeHatch,
            )
            session = started
            state = started.state
            gate = catalog.gateFor(loadedAlarm.id, scanner)
            anchorLabel = catalog.anchorFor(loadedAlarm.id)?.label
        }
        loaded = true
    }

    LaunchedEffect(session) {
        val started = session ?: return@LaunchedEffect
        val startedAt = System.currentTimeMillis()
        while (state is FiringState.Ringing) {
            delay(1_000)
            state = started.onEvent(FiringEvent.Tick((System.currentTimeMillis() - startedAt).milliseconds))
        }
    }

    LaunchedEffect(state) {
        when (val current = state) {
            is FiringState.Dismissed -> scope.launch {
                try {
                    recordOutcome(FiringOutcome.Dismissed)
                } finally {
                    onDismissed()
                }
            }
            // The Escape Hatch is recorded before the Alarm is silenced, so the
            // use survives for the Diagnostic Log and for the broken Streak. If
            // recording fails, the Alarm is still force-silenced.
            is FiringState.EscapeHatchUsed -> scope.launch {
                try {
                    escapeHatchLog.record(EscapeHatchUse(AlarmId(alarmId), Instant.now()))
                    recordOutcome(FiringOutcome.EscapeHatch)
                } finally {
                    onDismissed()
                }
            }
            // The cap stops the sound but not the Alarm: the service keeps the
            // ongoing notification until the challenge and anchor are done. It is
            // still recorded, because letting the cap expire is a firing outcome
            // the owner needs to be able to explain.
            is FiringState.Ringing -> if (current.capExpired) {
                AlarmService.stopSignalling(context)
                scope.launch { recordOutcome(FiringOutcome.CapExpired) }
            }
            null -> Unit
        }
    }

    val current = state
    when {
        !loaded -> Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        current == null || alarm == null -> MissingAlarm(onClose = onDismissed, modifier = modifier)

        current is FiringState.Dismissed -> DismissedAlarm(modifier = modifier)

        current is FiringState.EscapeHatchUsed -> EscapeHatchUsedAlarm(modifier = modifier)

        else -> FiringScreen(
            state = current,
            alarmLabel = formatFiringAlarm(alarm!!),
            anchorLabel = anchorLabel,
            silentMode = alarm!!.silentMode,
            typed = typed,
            scanning = scanning,
            onTypedChange = { typed = it },
            onSubmit = {
                session?.let { started ->
                    state = started.onEvent(FiringEvent.AnswerSubmitted(typed))
                }
                typed = ""
            },
            onScanAnchor = {
                gate?.let { boundGate ->
                    scope.launch {
                        scanning = true
                        val result = boundGate.scan()
                        scanning = false
                        session?.let { started ->
                            state = started.onEvent(FiringEvent.AnchorScanned(result))
                        }
                    }
                }
            },
            onEscapeHatchLongPress = {
                session?.let { started ->
                    state = started.onEvent(FiringEvent.EscapeHatchLongPressed)
                }
            },
            onEscapeHatchPassword = { password ->
                session?.let { started ->
                    state = started.onEvent(FiringEvent.EscapeHatchPasswordSubmitted(password))
                }
            },
            onEscapeHatchCancel = {
                session?.let { started ->
                    state = started.onEvent(FiringEvent.EscapeHatchCancelled)
                }
            },
            modifier = modifier,
        )
    }
}

@Composable
private fun EscapeHatchUsedAlarm(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = stringResource(R.string.escape_hatch_used), style = MaterialTheme.typography.headlineSmall)
        Text(text = stringResource(R.string.escape_hatch_used_detail))
    }
}

@Composable
private fun DismissedAlarm(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = stringResource(R.string.alarm_dismissed), style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun MissingAlarm(onClose: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.firing_alarm_missing),
            style = MaterialTheme.typography.headlineSmall,
        )
        Button(onClick = onClose) { Text(text = stringResource(R.string.firing_stop_alarm)) }
    }
}

private fun formatFiringAlarm(alarm: Alarm): String =
    "%02d:%02d".format(alarm.time.hour, alarm.time.minute)

/**
 * The alarm stream volume as a percentage of its maximum, so a firing that was
 * silent can be explained by the Diagnostic Log. Reads the platform only.
 */
private fun alarmStreamVolumePercent(context: Context): Int {
    val audioManager = context.getSystemService(AudioManager::class.java) ?: return 0
    val maximum = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
    if (maximum <= 0) return 0
    val current = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
    return (current * 100 / maximum).coerceIn(0, 100)
}
