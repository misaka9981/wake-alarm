package com.misaka9981.alarm.ui

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.misaka9981.alarm.R
import com.misaka9981.alarm.core.ReliabilityCheck
import com.misaka9981.alarm.core.ReliabilityRequirement
import com.misaka9981.alarm.reliability.AndroidReliabilityGrants
import com.misaka9981.alarm.reliability.ReliabilityLauncher

/**
 * Guides the owner to grant the platform guarantees an Alarm's reliability
 * depends on, and warns about any that are missing.
 *
 * Which requirements apply and which are missing is decided by
 * [ReliabilityCheck] in `core`; this screen only reads the platform state and
 * opens the right settings page. See the spec's "Firing reliably" stories.
 */
@Composable
fun ReliabilityScreen(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val check = remember { ReliabilityCheck(Build.VERSION.SDK_INT) }
    var grants by remember { mutableStateOf(AndroidReliabilityGrants.read(context)) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        grants = AndroidReliabilityGrants.read(context)
    }

    val required = check.required()
    val missing = check.missing(grants)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = stringResource(R.string.reliability_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(R.string.reliability_intro),
            style = MaterialTheme.typography.bodySmall,
        )

        Text(
            text = if (missing.isEmpty()) {
                stringResource(R.string.reliability_all_granted)
            } else {
                stringResource(R.string.reliability_missing_warning, missing.size)
            },
            color = if (missing.isEmpty()) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
        )

        ReliabilityRequirement.entries.forEach { requirement ->
            val applies = requirement in required
            val granted = grants.isGranted(requirement)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = requirementLabel(requirement), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = when {
                        !applies -> stringResource(R.string.reliability_not_needed)
                        granted -> stringResource(R.string.reliability_granted)
                        else -> requirementGuidance(requirement)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                if (applies && !granted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = {
                                ReliabilityLauncher.intentFor(context, requirement)
                                    ?.let { context.startActivity(it) }
                            },
                        ) { Text(text = stringResource(R.string.action_grant)) }
                    }
                }
            }
            HorizontalDivider()
        }

        Button(onClick = { refreshKey++ }, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.reliability_refresh))
        }
        TextButton(onClick = onClose) { Text(text = stringResource(R.string.action_close)) }
    }
}

@Composable
private fun requirementLabel(requirement: ReliabilityRequirement): String = stringResource(
    when (requirement) {
        ReliabilityRequirement.ExactAlarm -> R.string.requirement_exact_alarm
        ReliabilityRequirement.FullScreenIntent -> R.string.requirement_full_screen_intent
        ReliabilityRequirement.BatteryOptimisation -> R.string.requirement_battery_optimisation
        ReliabilityRequirement.DoNotDisturbAccess -> R.string.requirement_do_not_disturb
    },
)

@Composable
private fun requirementGuidance(requirement: ReliabilityRequirement): String = stringResource(
    when (requirement) {
        ReliabilityRequirement.ExactAlarm -> R.string.guidance_exact_alarm
        ReliabilityRequirement.FullScreenIntent -> R.string.guidance_full_screen_intent
        ReliabilityRequirement.BatteryOptimisation -> R.string.guidance_battery_optimisation
        ReliabilityRequirement.DoNotDisturbAccess -> R.string.guidance_do_not_disturb
    },
)
