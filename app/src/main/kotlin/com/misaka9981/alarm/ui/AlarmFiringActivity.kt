package com.misaka9981.alarm.ui

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.unit.dp
import com.misaka9981.alarm.core.Alarm
import com.misaka9981.alarm.core.AlarmId
import com.misaka9981.alarm.core.AnchorGate
import com.misaka9981.alarm.core.ArithmeticChallengeGenerator
import com.misaka9981.alarm.core.EscalationPolicy
import com.misaka9981.alarm.core.FiringEvent
import com.misaka9981.alarm.core.FiringSession
import com.misaka9981.alarm.core.FiringState
import com.misaka9981.alarm.data.AndroidAnchorScanner
import com.misaka9981.alarm.data.DataStoreAlarmRepository
import com.misaka9981.alarm.data.DataStoreAnchorRepository
import com.misaka9981.alarm.firing.AlarmNotification
import com.misaka9981.alarm.firing.AlarmService
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
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (alarmId == null) {
                        MissingAlarm(onClose = ::endFiring)
                    } else {
                        FiringRoute(alarmId = alarmId, onDismissed = ::endFiring)
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

        /** An Intent that fires [alarmId] on this screen. */
        fun intent(context: Context, alarmId: String): Intent =
            Intent(context, AlarmFiringActivity::class.java).putExtra(EXTRA_ALARM_ID, alarmId)
    }
}

@Composable
private fun FiringRoute(
    alarmId: String,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val alarmRepository = remember(context) { DataStoreAlarmRepository(context) }
    val anchorRepository = remember(context) { DataStoreAnchorRepository(context) }
    val scanner = remember(context) { AndroidAnchorScanner(context) }

    var alarm by remember { mutableStateOf<Alarm?>(null) }
    var anchorLabel by remember { mutableStateOf<String?>(null) }
    var session by remember { mutableStateOf<FiringSession?>(null) }
    var gate by remember { mutableStateOf<AnchorGate?>(null) }
    var state by remember { mutableStateOf<FiringState?>(null) }
    var typed by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(alarmId) {
        val loadedAlarm = alarmRepository.load().firstOrNull { it.id == AlarmId(alarmId) }
        val catalog = anchorRepository.load()
        alarm = loadedAlarm
        if (loadedAlarm != null) {
            val started = FiringSession.start(
                generator = ArithmeticChallengeGenerator(Random(System.currentTimeMillis())),
                policy = EscalationPolicy(),
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
            is FiringState.Dismissed -> onDismissed()
            // The cap stops the sound but not the Alarm: the service keeps the
            // ongoing notification until the challenge and anchor are done.
            is FiringState.Ringing -> if (current.capExpired) AlarmService.stopSignalling(context)
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

        else -> FiringScreen(
            state = current,
            alarmLabel = formatFiringAlarm(alarm!!),
            anchorLabel = anchorLabel,
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
            modifier = modifier,
        )
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
        Text(text = "Alarm dismissed.", style = MaterialTheme.typography.headlineSmall)
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
        Text(text = "This Alarm no longer exists.", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onClose) { Text(text = "Stop the Alarm") }
    }
}

private fun formatFiringAlarm(alarm: Alarm): String =
    "%02d:%02d".format(alarm.time.hour, alarm.time.minute)
