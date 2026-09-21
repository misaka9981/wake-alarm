package com.misaka9981.alarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.misaka9981.alarm.core.AlarmRepository
import com.misaka9981.alarm.core.AnchorRepository
import com.misaka9981.alarm.core.AnchorScanner
import com.misaka9981.alarm.data.AndroidAnchorScanner
import com.misaka9981.alarm.data.DataStoreAlarmRepository
import com.misaka9981.alarm.data.DataStoreAnchorRepository
import com.misaka9981.alarm.ui.WakeAlarmRoot

/**
 * Thin Android adapter around the pure `core` module.
 *
 * It wires the persistent repositories and the camera port to the Compose
 * screens; every rule about what an Alarm or a Physical Anchor is, and how they
 * change, lives in `core`.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val alarmRepository: AlarmRepository = DataStoreAlarmRepository(applicationContext)
        val anchorRepository: AnchorRepository = DataStoreAnchorRepository(applicationContext)
        val anchorScanner: AnchorScanner = AndroidAnchorScanner(this)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WakeAlarmRoot(alarmRepository, anchorRepository, anchorScanner)
                }
            }
        }
    }
}
