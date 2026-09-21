package com.misaka9981.alarm.core

/**
 * The owner's Physical Anchors and which Alarm each is bound to.
 *
 * Every set, delete, bind, and unbind is a pure transformation returning a new
 * catalog, so the rules can be tested without Android or storage. Anchors are
 * keyed by their code and ordered by label, which is the order the UI lists
 * them.
 *
 * See `CONTEXT.md`: a Physical Anchor, never a QR code or checkpoint.
 */
class AnchorCatalog private constructor(
    val anchors: List<PhysicalAnchor>,
    val bindings: Map<AlarmId, AnchorCode>,
) {

    /** Adds an anchor, or relabels the one that already carries the same code. */
    fun set(anchor: PhysicalAnchor): AnchorCatalog =
        of(anchors.filterNot { it.code == anchor.code } + anchor, bindings)

    /** Removes an anchor and every binding that pointed at it. */
    fun delete(code: AnchorCode): AnchorCatalog =
        of(
            anchors.filterNot { it.code == code },
            bindings.filterValues { it != code },
        )

    /** Binds [code] to [alarmId], replacing any anchor already bound to it. */
    fun bind(alarmId: AlarmId, code: AnchorCode): AnchorCatalog {
        require(anchors.any { it.code == code }) {
            "cannot bind an Alarm to an anchor that has not been set"
        }
        return AnchorCatalog(anchors, bindings + (alarmId to code))
    }

    /** Removes the binding for [alarmId], leaving the anchor itself in place. */
    fun unbind(alarmId: AlarmId): AnchorCatalog = AnchorCatalog(anchors, bindings - alarmId)

    /** The anchor bound to [alarmId], or `null` if the Alarm has none. */
    fun anchorFor(alarmId: AlarmId): PhysicalAnchor? =
        bindings[alarmId]?.let { code -> anchors.firstOrNull { it.code == code } }

    /** The anchor carrying [code], or `null` if it has not been set. */
    fun findByCode(code: AnchorCode): PhysicalAnchor? = anchors.firstOrNull { it.code == code }

    /** A verifier for [alarmId]'s bound anchor; it rejects everything if unbound. */
    fun verifierFor(alarmId: AlarmId): AnchorVerifier = AnchorVerifier(anchorFor(alarmId))

    /** A gate that scans through [scanner] and checks against [alarmId]'s anchor. */
    fun gateFor(alarmId: AlarmId, scanner: AnchorScanner): AnchorGate =
        AnchorGate(scanner, verifierFor(alarmId))

    companion object {
        private val byLabel = compareBy<PhysicalAnchor>({ it.label }, { it.code.value })

        /**
         * Builds a catalog from persisted anchors and bindings.
         *
         * A binding must point at a set anchor, otherwise the dismissal flow
         * could never reach it; rejecting that here keeps a dangling binding
         * from ever existing.
         */
        fun of(
            anchors: List<PhysicalAnchor>,
            bindings: Map<AlarmId, AnchorCode> = emptyMap(),
        ): AnchorCatalog {
            require(anchors.map { it.code }.toSet().size == anchors.size) {
                "two Physical Anchors cannot share a code"
            }
            val known = anchors.map { it.code }.toSet()
            val dangling = bindings.filterValues { it !in known }.keys
            require(dangling.isEmpty()) {
                "bindings reference anchors that have not been set: $dangling"
            }
            return AnchorCatalog(anchors.sortedWith(byLabel), bindings)
        }

        val empty: AnchorCatalog = of(emptyList())
    }
}
