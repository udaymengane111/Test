package app.worn.domain.engine

import app.worn.domain.model.DailyRecord
import app.worn.domain.model.SessionKind
import app.worn.domain.model.TrackingSession
import app.worn.domain.model.toEpochMillis
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min

data class DayWindow(
    val date: LocalDate,
    val zone: ZoneId,
    val startMillis: Long,
    val endMillis: Long,
) {
    val lengthMillis: Long get() = endMillis - startMillis
}

data class DayTotals(
    val date: LocalDate,
    val targetMillis: Long,
    val wornMillis: Long,
    val elapsedMillis: Long,
    val notWornMillis: Long,
    val remainingMillis: Long,
    val progress: Float,
    val targetReached: Boolean,
) {
    val remainingOrZero: Long get() = max(0L, remainingMillis)
}

data class TimelineSegment(
    val session: TrackingSession,
    val startMillis: Long,
    val endMillis: Long,
    val clippedFromOriginal: Boolean,
)

data class TimerSnapshot(
    val remainingMillis: Long,
    val elapsedMillis: Long,
    val configuredMillis: Long,
    val paused: Boolean,
    val overdue: Boolean,
    val overdueMillis: Long,
)

object WearCalculator {

    fun dayWindow(record: DailyRecord, nowMillis: Long? = null): DayWindow {
        val zone = record.zone()
        val start = record.localDate.toEpochMillis(zone)
        val end = record.localDate.plusDays(1).toEpochMillis(zone)
        return DayWindow(record.localDate, zone, start, end)
    }

    fun dayWindow(date: LocalDate, zone: ZoneId): DayWindow {
        val start = date.toEpochMillis(zone)
        val end = date.plusDays(1).toEpochMillis(zone)
        return DayWindow(date, zone, start, end)
    }

    fun overlapMillis(startA: Long, endA: Long, startB: Long, endB: Long): Long {
        val start = max(startA, startB)
        val end = min(endA, endB)
        return max(0L, end - start)
    }

    fun wornMillis(
        sessions: List<TrackingSession>,
        windowStart: Long,
        windowEnd: Long,
        nowMillis: Long,
    ): Long {
        return sessions
            .filter { it.kind == SessionKind.WEAR }
            .sumOf { session ->
                overlapMillis(
                    session.startMillis,
                    session.closedEnd(nowMillis),
                    windowStart,
                    windowEnd,
                )
            }
    }

    fun totals(
        sessions: List<TrackingSession>,
        record: DailyRecord,
        nowMillis: Long,
    ): DayTotals {
        val window = dayWindow(record)
        val clipEnd = min(window.endMillis, max(window.startMillis, nowMillis))
        val elapsed = if (nowMillis <= window.startMillis) {
            0L
        } else {
            min(window.lengthMillis, clipEnd - window.startMillis)
        }
        val worn = wornMillis(sessions, window.startMillis, clipEnd, nowMillis)
        val clampedWorn = min(elapsed, worn)
        val notWorn = max(0L, elapsed - clampedWorn)
        val target = record.targetMinutes * 60_000L
        val remaining = max(0L, target - clampedWorn)
        val progress = if (target <= 0L) 0f else (clampedWorn.toFloat() / target.toFloat()).coerceIn(0f, 1.2f)
        return DayTotals(
            date = record.localDate,
            targetMillis = target,
            wornMillis = clampedWorn,
            elapsedMillis = elapsed,
            notWornMillis = notWorn,
            remainingMillis = remaining,
            progress = progress,
            targetReached = clampedWorn >= target && target > 0L,
        )
    }

    fun segmentsForDay(
        sessions: List<TrackingSession>,
        record: DailyRecord,
        nowMillis: Long,
    ): List<TimelineSegment> {
        val window = dayWindow(record)
        val clipEnd = min(window.endMillis, max(window.startMillis, nowMillis))
        return sessions.mapNotNull { session ->
            val sessionEnd = session.closedEnd(nowMillis)
            val start = max(session.startMillis, window.startMillis)
            val end = min(sessionEnd, clipEnd)
            if (end <= start) {
                null
            } else {
                TimelineSegment(
                    session = session,
                    startMillis = start,
                    endMillis = end,
                    clippedFromOriginal = start != session.startMillis || (session.endMillis != null && end != session.endMillis),
                )
            }
        }.sortedBy { it.startMillis }
    }

    /**
     * Consecutive days meeting or exceeding the daily target, counting back from [fromDate].
     * Incomplete "today" still counts if the target is already reached.
     */
    fun streak(
        days: List<DayTotals>,
        fromDate: LocalDate,
    ): Int {
        val byDate = days.associateBy { it.date }
        var count = 0
        var cursor = fromDate
        while (true) {
            val totals = byDate[cursor] ?: break
            if (!totals.targetReached) break
            count += 1
            cursor = cursor.minusDays(1)
        }
        return count
    }

    fun averageWornMillis(days: List<DayTotals>): Long {
        if (days.isEmpty()) return 0L
        return days.sumOf { it.wornMillis } / days.size
    }
}

object ActivityTimerCalculator {

    fun snapshot(session: TrackingSession, nowMillis: Long): TimerSnapshot {
        require(session.kind == SessionKind.REMOVAL) { "Timer applies to removal sessions" }
        val configured = session.configuredDurationMillis ?: 0L
        val elapsed = elapsedMillis(session, nowMillis)
        val remaining = configured - elapsed
        val overdue = remaining < 0L
        return TimerSnapshot(
            remainingMillis = remaining,
            elapsedMillis = elapsed,
            configuredMillis = configured,
            paused = session.isPaused,
            overdue = overdue,
            overdueMillis = if (overdue) -remaining else 0L,
        )
    }

    fun elapsedMillis(session: TrackingSession, nowMillis: Long): Long {
        val end = when {
            session.endMillis != null -> session.endMillis
            session.pauseStartedMillis != null -> session.pauseStartedMillis
            else -> nowMillis
        }
        val raw = end - session.startMillis - session.pauseAccumulatedMillis
        return max(0L, raw)
    }

    /**
     * Wall-clock instant when the countdown hits zero, or null if paused/closed.
     */
    fun expiryEpochMillis(session: TrackingSession): Long? {
        if (session.kind != SessionKind.REMOVAL) return null
        if (session.endMillis != null) return null
        if (session.pauseStartedMillis != null) return null
        val configured = session.configuredDurationMillis ?: return null
        return session.startMillis + configured + session.pauseAccumulatedMillis
    }

    fun withPauseStarted(session: TrackingSession, nowMillis: Long): TrackingSession {
        if (session.pauseStartedMillis != null) return session
        return session.copy(pauseStartedMillis = nowMillis)
    }

    fun withResumed(session: TrackingSession, nowMillis: Long): TrackingSession {
        val pausedAt = session.pauseStartedMillis ?: return session
        val extra = max(0L, nowMillis - pausedAt)
        return session.copy(
            pauseAccumulatedMillis = session.pauseAccumulatedMillis + extra,
            pauseStartedMillis = null,
        )
    }

    fun withSnooze(session: TrackingSession, extraMillis: Long): TrackingSession {
        val configured = session.configuredDurationMillis ?: 0L
        return session.copy(configuredDurationMillis = configured + extraMillis)
    }
}

object SessionEditor {

    fun overlapping(a: TrackingSession, b: TrackingSession): Boolean {
        val aEnd = a.endMillis ?: Long.MAX_VALUE
        val bEnd = b.endMillis ?: Long.MAX_VALUE
        return a.startMillis < bEnd && b.startMillis < aEnd && a.id != b.id
    }

    fun validateNoOverlap(sessions: List<TrackingSession>): Boolean {
        val sorted = sessions.sortedBy { it.startMillis }
        for (i in 0 until sorted.lastIndex) {
            val a = sorted[i]
            val b = sorted[i + 1]
            val aEnd = a.endMillis ?: return false
            if (aEnd > b.startMillis) return false
        }
        val openCount = sessions.count { it.isOpen }
        return openCount <= 1
    }

    fun replace(sessions: List<TrackingSession>, updated: TrackingSession): List<TrackingSession> {
        return sessions.map { if (it.id == updated.id) updated else it }
    }

    fun delete(sessions: List<TrackingSession>, id: String): List<TrackingSession> {
        return sessions.filterNot { it.id == id }
    }
}
