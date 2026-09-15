package app.worn.domain.engine

import app.worn.domain.model.ActivityType
import app.worn.domain.model.DailyRecord
import app.worn.domain.model.SessionKind
import app.worn.domain.model.TrackingSession
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

enum class ReportGrain { DAY, WEEK, MONTH, QUARTER, YEAR }

data class ReportWindow(
    val grain: ReportGrain,
    val start: LocalDate,
    val endInclusive: LocalDate,
)

data class DayBreakdown(
    val date: LocalDate,
    val hasData: Boolean,
    val wornMillis: Long,
    val targetMillis: Long,
    val missedMillis: Long,
    val targetReached: Boolean,
    val todayInProgress: Boolean,
)

data class ActivityRemovalStat(
    val activityId: String,
    val name: String,
    val count: Int,
    val actualMillis: Long,
    val averageMillis: Long,
)

data class PeriodReport(
    val window: ReportWindow,
    val days: List<DayBreakdown>,
    val daysWithData: List<DayBreakdown>,
    val totalWornMillis: Long,
    val totalTargetMillis: Long,
    val missedMillis: Long,
    val achievement: Float,
    val averageDailyWearMillis: Long?,
    val averageOutMillis: Long?,
    val daysOnTarget: Int,
    val completeDaysWithData: Int,
    val todayInProgress: Boolean,
    val bestDay: DayBreakdown?,
    val lowestDay: DayBreakdown?,
    val removals: List<ActivityRemovalStat>,
    val totalOutMillis: Long,
)

object ReportCalculator {

    fun window(grain: ReportGrain, anchor: LocalDate): ReportWindow {
        return when (grain) {
            ReportGrain.DAY -> ReportWindow(grain, anchor, anchor)
            ReportGrain.WEEK -> {
                val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                ReportWindow(grain, start, start.plusDays(6))
            }
            ReportGrain.MONTH -> {
                val ym = YearMonth.from(anchor)
                ReportWindow(grain, ym.atDay(1), ym.atEndOfMonth())
            }
            ReportGrain.QUARTER -> {
                val q = (anchor.monthValue - 1) / 3
                val startMonth = Month.of(q * 3 + 1)
                val start = LocalDate.of(anchor.year, startMonth, 1)
                val end = start.plusMonths(3).minusDays(1)
                ReportWindow(grain, start, end)
            }
            ReportGrain.YEAR -> ReportWindow(
                grain,
                LocalDate.of(anchor.year, 1, 1),
                LocalDate.of(anchor.year, 12, 31),
            )
        }
    }

    fun shift(window: ReportWindow, delta: Long): ReportWindow {
        val anchor = when (window.grain) {
            ReportGrain.DAY -> window.start.plusDays(delta)
            ReportGrain.WEEK -> window.start.plusWeeks(delta)
            ReportGrain.MONTH -> window.start.plusMonths(delta)
            ReportGrain.QUARTER -> window.start.plusMonths(3 * delta)
            ReportGrain.YEAR -> window.start.plusYears(delta)
        }
        return window(window.grain, anchor)
    }

    fun hasData(
        sessions: List<TrackingSession>,
        record: DailyRecord,
        nowMillis: Long,
    ): Boolean = WearCalculator.segmentsForDay(sessions, record, nowMillis).isNotEmpty()

    fun report(
        grain: ReportGrain,
        anchor: LocalDate,
        today: LocalDate,
        nowMillis: Long,
        sessions: List<TrackingSession>,
        recordFor: (LocalDate) -> DailyRecord,
        activities: List<ActivityType>,
    ): PeriodReport {
        val win = window(grain, anchor)
        val days = generateSequence(win.start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(win.endInclusive) }
            .map { date ->
                val record = recordFor(date)
                val totals = WearCalculator.totals(sessions, record, nowMillis)
                val data = hasData(sessions, record, nowMillis)
                val inProgress = date == today
                val missed = if (data) maxOf(0L, totals.targetMillis - totals.wornMillis) else 0L
                DayBreakdown(
                    date = date,
                    hasData = data,
                    wornMillis = if (data) totals.wornMillis else 0L,
                    targetMillis = totals.targetMillis,
                    missedMillis = missed,
                    targetReached = data && totals.targetReached,
                    todayInProgress = inProgress,
                )
            }
            .toList()

        val withData = days.filter { it.hasData }
        val completeWithData = withData.filterNot { it.todayInProgress && !it.targetReached }
        val daysOnTarget = completeWithData.count { it.targetReached }
        val completeCount = withData.count { !it.todayInProgress } +
            withData.count { it.todayInProgress && it.targetReached }

        val totalWorn = withData.sumOf { it.wornMillis }
        val totalTarget = withData.sumOf { it.targetMillis }
        val missed = withData.sumOf { it.missedMillis }
        val achievement = if (totalTarget <= 0L) 0f else totalWorn.toFloat() / totalTarget.toFloat()

        val outMillis = withData.map { day ->
            val record = recordFor(day.date)
            val windowBounds = WearCalculator.dayWindow(record)
            val clipEnd = minOf(windowBounds.endMillis, maxOf(windowBounds.startMillis, nowMillis))
            sessions.filter { it.kind == SessionKind.REMOVAL }.sumOf { session ->
                WearCalculator.overlapMillis(
                    session.startMillis,
                    session.closedEnd(nowMillis),
                    windowBounds.startMillis,
                    clipEnd,
                )
            }
        }
        val totalOut = outMillis.sum()
        val avgWear = if (withData.isEmpty()) null else totalWorn / withData.size
        val avgOut = if (withData.isEmpty()) null else totalOut / withData.size

        val ranked = withData.filter { !it.todayInProgress || it.hasData }
        val best = ranked.maxByOrNull { it.wornMillis }
        val lowest = ranked.minByOrNull { it.wornMillis }

        return PeriodReport(
            window = win,
            days = days,
            daysWithData = withData,
            totalWornMillis = totalWorn,
            totalTargetMillis = totalTarget,
            missedMillis = missed,
            achievement = achievement,
            averageDailyWearMillis = avgWear,
            averageOutMillis = avgOut,
            daysOnTarget = daysOnTarget,
            completeDaysWithData = completeCount,
            todayInProgress = days.any { it.todayInProgress },
            bestDay = best,
            lowestDay = if (ranked.size >= 2) lowest else null,
            removals = removalStats(sessions, win, nowMillis, activities, recordFor),
            totalOutMillis = totalOut,
        )
    }

    private fun removalStats(
        sessions: List<TrackingSession>,
        win: ReportWindow,
        nowMillis: Long,
        activities: List<ActivityType>,
        recordFor: (LocalDate) -> DailyRecord,
    ): List<ActivityRemovalStat> {
        val periodStart = recordFor(win.start).let { WearCalculator.dayWindow(it).startMillis }
        val periodEnd = recordFor(win.endInclusive).let { WearCalculator.dayWindow(it).endMillis }
        val clipEnd = minOf(periodEnd, nowMillis)
        val removals = sessions.filter { it.kind == SessionKind.REMOVAL }
        val byId = LinkedHashMap<String, Pair<Int, Long>>()
        removals.forEach { session ->
            val overlap = WearCalculator.overlapMillis(
                session.startMillis,
                session.closedEnd(nowMillis),
                periodStart,
                clipEnd,
            )
            if (overlap <= 0L) return@forEach
            val id = session.activityTypeId ?: "unknown"
            val prev = byId[id] ?: (0 to 0L)
            byId[id] = (prev.first + 1) to (prev.second + overlap)
        }
        val known = activities.associateBy { it.id }
        return byId.map { (id, value) ->
            val name = known[id]?.name ?: "Other"
            ActivityRemovalStat(
                activityId = id,
                name = name,
                count = value.first,
                actualMillis = value.second,
                averageMillis = if (value.first == 0) 0L else value.second / value.first,
            )
        }.sortedBy { stat -> known[stat.activityId]?.sortOrder ?: 99 }
    }
}
