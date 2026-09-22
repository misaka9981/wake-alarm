package com.misaka9981.alarm.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.misaka9981.alarm.R

/**
 * The one titled top app bar every secondary screen shares. Its back arrow is
 * the screen's close action, replacing the per-screen headline plus Close/Back
 * buttons the screens used to draw themselves.
 *
 * Presentation only: [onBack] is whatever close callback the host passed in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WakeAlarmTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
        },
        modifier = modifier,
    )
}
