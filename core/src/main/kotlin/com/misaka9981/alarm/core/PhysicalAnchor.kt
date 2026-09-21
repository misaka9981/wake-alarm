package com.misaka9981.alarm.core

/**
 * The payload a scan of a Physical Anchor must produce.
 *
 * It is whatever the owner's chosen real-world object encodes — a printed QR
 * code, a product barcode, anything the camera can read. Reaching the anchor is
 * decided by comparing this payload exactly, so an object the owner did not
 * choose cannot stand in for it. See `CONTEXT.md`: a Physical Anchor, never a
 * QR code or checkpoint.
 */
@JvmInline
value class AnchorCode(val value: String) {
    init {
        require(value.isNotBlank()) { "an AnchorCode must not be blank" }
    }
}

/**
 * A real-world object the owner places away from the bed, identified by the
 * [code] it encodes.
 *
 * The code is the anchor's identity: two objects carrying the same code are the
 * same anchor, and relabelling one keeps every binding that pointed at it.
 * [label] is how the owner recognises it; it is shown on one line.
 */
data class PhysicalAnchor(val code: AnchorCode, val label: String) {
    init {
        require(label.isNotBlank()) { "a PhysicalAnchor must have a label" }
        require(label.none { it == '\n' || it == '\r' }) {
            "a PhysicalAnchor label must be a single line"
        }
    }
}
