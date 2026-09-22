package com.misaka9981.alarm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.misaka9981.alarm.core.AlarmRepository
import com.misaka9981.alarm.core.AnchorRepository
import com.misaka9981.alarm.core.AnchorScanner
import com.misaka9981.alarm.core.EscapeHatchRepository
import com.misaka9981.alarm.data.AndroidAnchorScanner
import com.misaka9981.alarm.data.DataStoreAlarmRepository
import com.misaka9981.alarm.data.DataStoreAnchorRepository
import com.misaka9981.alarm.data.DataStoreEscapeHatchRepository
import com.misaka9981.alarm.firing.AlarmNotification
import com.misaka9981.alarm.schedule.AlarmScheduler
import com.misaka9981.alarm.ui.WakeAlarmRoot

/**
 * Thin Android adapter around the pure `core` module.
 *
 * It wires the persistent repositories, the scheduling adapter, and the camera
 * port to the Compose screens; every rule about what an Alarm or a Physical
 * Anchor is, and how they change, lives in `core`.
 */
class MainActivity : ComponentActivity() {
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val alarmRepository: AlarmRepository = DataStoreAlarmRepository(applicationContext)
        val anchorRepository: AnchorRepository = DataStoreAnchorRepository(applicationContext)
        val escapeHatchRepository: EscapeHatchRepository =
            DataStoreEscapeHatchRepository(applicationContext)
        val anchorScanner: AnchorScanner = AndroidAnchorScanner(this)
        val scheduler = AlarmScheduler(applicationContext)

        AlarmNotification.ensureChannel(applicationContext)
        requestNotificationPermissionIfNeeded()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WakeAlarmRoot(
                        alarmRepository,
                        anchorRepository,
                        escapeHatchRepository,
                        anchorScanner,
                        scheduler,
                    )
                }
            }
        }
    }

    /**
     * The Alarm's full-screen notification cannot appear without notification
     * permission on Android 13 and above, so ask once.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
