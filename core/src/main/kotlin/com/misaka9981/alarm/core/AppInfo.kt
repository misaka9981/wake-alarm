package com.misaka9981.alarm.core

/**
 * Pure-Kotlin identity of the app.
 *
 * This is deliberately trivial: it exists so the project skeleton has a real
 * `core` -> Android seam to compile and test end to end. Every decidable piece
 * of logic in this project lives in this module, with no Android dependencies.
 */
object AppInfo {
    const val NAME: String = "Wake Alarm"
}
