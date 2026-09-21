package com.misaka9981.alarm.core

/**
 * Whether the owner has physically reached their bound Physical Anchor.
 *
 * [Reached] only when the payload the camera read is exactly the bound anchor's
 * code. [NotReached] otherwise, including when the owner cancelled the scan —
 * [NotReached.scanned] is the raw payload that was read, or `null` for a
 * cancellation, so the UI can report what happened.
 */
sealed interface AnchorScanResult {
    data object Reached : AnchorScanResult

    data class NotReached(val scanned: String?) : AnchorScanResult
}

/**
 * Port for reading a Physical Anchor. The Android adapter implements this with
 * the camera and returns the raw scanned payload, or `null` if the owner
 * cancelled. Nothing about judging that payload belongs on the Android side;
 * [AnchorVerifier] decides.
 */
interface AnchorScanner {
    /** Performs one scan and returns the raw payload, or `null` when cancelled. */
    suspend fun scan(): String?
}

/**
 * Decides whether a scanned payload reached the bound Physical Anchor.
 *
 * When the Alarm has no bound anchor ([bound] is `null`) nothing can reach it,
 * so every scan fails. This pure comparison is the whole decision, which is why
 * it lives in `core` with no Android dependency.
 */
class AnchorVerifier(private val bound: PhysicalAnchor?) {
    fun verify(scanned: String?): AnchorScanResult =
        if (bound != null && scanned == bound.code.value) {
            AnchorScanResult.Reached
        } else {
            AnchorScanResult.NotReached(scanned)
        }
}

/**
 * Pulls one scan from the [AnchorScanner] port and reports whether it reached
 * the bound anchor.
 *
 * This is the seam the dismissal flow uses: the camera is platform-specific, but
 * reached-or-not is decided here, so it can be required alongside the Dismiss
 * Challenge without any Android dependency in the decision.
 */
class AnchorGate(
    private val scanner: AnchorScanner,
    private val verifier: AnchorVerifier,
) {
    /** Scans once and returns the resulting [AnchorScanResult]. */
    suspend fun scan(): AnchorScanResult = verifier.verify(scanner.scan())
}
