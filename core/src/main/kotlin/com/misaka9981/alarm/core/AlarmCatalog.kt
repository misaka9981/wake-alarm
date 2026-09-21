package com.misaka9981.alarm.core

/**
 * The owner's Alarm configuration, held as one immutable value.
 *
 * Every create, edit, enable/disable, and delete is a pure transformation that
 * returns a new catalog, so the rules can be tested without Android or storage.
 * The catalog is always ordered by time, which is the order the list shows.
 *
 * See `CONTEXT.md`: an Alarm, never a reminder or a timer.
 */
class AlarmCatalog private constructor(val alarms: List<Alarm>) {

    fun find(id: AlarmId): Alarm? = alarms.firstOrNull { it.id == id }

    /** Creates a new Alarm. Adding an id that already exists replaces it. */
    fun add(alarm: Alarm): AlarmCatalog = of(alarms.filterNot { it.id == alarm.id } + alarm)

    /** Edits an existing Alarm, matched by id. */
    fun update(alarm: Alarm): AlarmCatalog = add(alarm)

    fun delete(id: AlarmId): AlarmCatalog = of(alarms.filterNot { it.id == id })

    /** Pauses or resumes an Alarm without deleting it. */
    fun setEnabled(id: AlarmId, enabled: Boolean): AlarmCatalog =
        of(alarms.map { if (it.id == id) it.copy(enabled = enabled) else it })

    companion object {
        private val byTime = compareBy<Alarm>({ it.time.hour }, { it.time.minute }, { it.id.value })

        fun of(alarms: List<Alarm>): AlarmCatalog = AlarmCatalog(alarms.sortedWith(byTime))

        val empty: AlarmCatalog = of(emptyList())
    }
}
