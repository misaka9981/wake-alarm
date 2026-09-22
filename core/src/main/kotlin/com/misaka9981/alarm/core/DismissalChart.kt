package com.misaka9981.alarm.core

import java.time.LocalDate

/**
 * Which figure a [DismissalBar] plots.
 *
 * The words and units the owner reads are rendered in the Android layer from
 * resources; deciding what number each bar stands for is pure logic and lives
 * here, so the chart can never disagree with the recorded [DismissalRecord]s.
 */
enum class DismissalMetric {
    /** How long the Alarm rang before it was dismissed. */
    RingingSeconds {
        override fun measure(record: DismissalRecord): Long =
            record.ringingDuration.inWholeSeconds
    },

    /** How many answers were wrong on the way to the dismissal. */
    WrongAnswers {
        override fun measure(record: DismissalRecord): Long = record.wrongAnswers.toLong()
    };

    /** The figure [record] contributes to a bar, in this metric's own unit. */
    internal abstract fun measure(record: DismissalRecord): Long
}

/**
 * One bar of the recent-dismissals chart.
 *
 * [day] is the morning the bar stands for. [value] is the plotted figure in
 * [DismissalMetric]'s own unit. [fraction] is the bar's height relative to the
 * tallest bar in the series, in `0f..1f`; when every figure is zero, every
 * fraction is `0f` rather than a division by zero.
 */
data class DismissalBar(
    val day: LocalDate,
    val value: Long,
    val fraction: Float,
)

/**
 * Maps the recorded [dismissals] to the bars of a simple chart.
 *
 * The list is taken as [WakeStatistics.recentDismissals] reports it — newest
 * first — and kept in that order, so the chart reads the same way as the list
 * beside it. Every bar is scaled against the series' tallest figure, so the
 * shape of recent mornings is readable without any new persistence.
 *
 * See the spec's "Statistics" and `CONTEXT.md` (Diagnostic Log).
 */
fun List<DismissalRecord>.chartBars(metric: DismissalMetric): List<DismissalBar> {
    val values = map { metric.measure(it) }
    val tallest = values.maxOrNull() ?: 0L
    return mapIndexed { index, dismissal ->
        val value = values[index]
        DismissalBar(
            day = dismissal.day,
            value = value,
            fraction = if (tallest > 0L) value.toFloat() / tallest else 0f,
        )
    }
}
