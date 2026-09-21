package com.misaka9981.alarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.misaka9981.alarm.core.AlarmRepository
import com.misaka9981.alarm.data.DataStoreAlarmRepository
import com.misaka9981.alarm.ui.AlarmListScreen

/**
 * Thin Android adapter around the pure `core` module.
 *
 * It wires the persistent [AlarmRepository] adapter to the Compose screen; every
 * rule about what an Alarm is and how it changes lives in `core`.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository: AlarmRepository = DataStoreAlarmRepository(applicationContext)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AlarmListScreen(repository)
                }
            }
        }
    }
}
