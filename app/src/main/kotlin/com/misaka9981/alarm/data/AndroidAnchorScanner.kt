package com.misaka9981.alarm.data

import android.content.Context
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.misaka9981.alarm.core.AnchorScanner
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Thin Android adapter for the [AnchorScanner] port: it opens the camera and
 * returns the raw payload it read, or `null` when the owner cancels or the scan
 * fails.
 *
 * It makes no judgement about the payload — deciding whether a scan reached the
 * bound Physical Anchor is [com.misaka9981.alarm.core.AnchorVerifier]'s job in
 * `core`.
 */
class AndroidAnchorScanner(context: Context) : AnchorScanner {
    private val client = GmsBarcodeScanning.getClient(context)

    override suspend fun scan(): String? = suspendCancellableCoroutine { continuation ->
        client.startScan()
            .addOnSuccessListener { barcode -> continuation.resume(barcode.rawValue) }
            .addOnCanceledListener { continuation.resume(null) }
            .addOnFailureListener { continuation.resume(null) }
    }
}
