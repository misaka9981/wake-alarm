package com.misaka9981.alarm.core

/**
 * The owner's choice of in-app language.
 *
 * [System] follows the phone's locale; [Chinese] and [English] pin the app to one
 * language without changing the phone. The default resources are Chinese, so a
 * device in any language other than English displays Chinese unless the owner
 * chooses otherwise.
 *
 * [explicitTag] is the BCP-47 language tag a pinned choice applies, or `null` for
 * [System], which applies whatever the device already uses. This is the decidable
 * half of applying the language; turning a tag into a platform `Locale` is the
 * Android adapter's job.
 */
enum class AppLanguage(val explicitTag: String?) {
    System(null),
    Chinese("zh"),
    English("en"),
}
