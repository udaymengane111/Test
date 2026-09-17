package app.worn.domain.engine

import app.worn.domain.model.DailyRecord
import app.worn.domain.model.SessionKind
import app.worn.domain.model.TrackingSession
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class WearCalculatorTest {

    private val zone = ZoneId.of("America/New_York")
    private val day = LocalDate.of(2026, 9, 15)
    private val record = DailyRecord(day, zone.id, 22 * 60)

    private fun ts(
        kind: SessionKind,
        start: Long,
        end: Long? = null,
        activity: String? = null,
        configured: Long? = null,
        pauseAcc: Long = 0,
        pauseAt: Long? = null,
        id: String = "$start",
    ) = TrackingSession(id, kind, start, end, activity, configured, pauseAcc, pauseAt)

    private fun at(hour: Int, minute: Int, second: Int = 0, date: LocalDate = day): Long {
        return ZonedDateTime.of(date, java.time.LocalTime.of(hour, minute, second), zone)
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun normalWearSession() {
        val sessions = listOf(ts(SessionKind.WEAR, at(8, 0), at(12, 30)))
        val now = at(12, 30)
        val totals = WearCalculator.totals(sessions, record, now)
        assertThat(totals.wornMillis).isEqualTo(TimeUnit.HOURS.toMillis(4) + TimeUnit.MINUTES.toMillis(30))
    }

    @Test
    fun normalRemovalSessionDoesNotCountAsWorn() {
        val sessions = listOf(
            ts(SessionKind.WEAR, at(8, 0), at(12, 30)),
            ts(SessionKind.REMOVAL, at(12, 30), at(13, 0), "lunch", TimeUnit.MINUTES.toMillis(30)),
        )
        val now = at(13, 0)
        val totals = WearCalculator.totals(sessions, record, now)
        assertThat(totals.wornMillis).isEqualTo(TimeUnit.HOURS.toMillis(4) + TimeUnit.MINUTES.toMillis(30))
        val elapsed = now - at(0, 0)
        assertThat(totals.notWornMillis).isEqualTo(elapsed - totals.wornMillis)
        assertThat(totals.notWornMillis).isAtLeast(TimeUnit.MINUTES.toMillis(30))
    }

    @Test
    fun multipleRemovalSessions() {
        val sessions = listOf(
            ts(SessionKind.WEAR, at(8, 0), at(12, 30)),
            ts(SessionKind.REMOVAL, at(12, 30), at(13, 0), "lunch", TimeUnit.MINUTES.toMillis(30)),
            ts(SessionKind.WEAR, at(13, 0), at(15, 30)),
            ts(SessionKind.REMOVAL, at(15, 30), at(15, 40), "tea", TimeUnit.MINUTES.toMillis(10)),
            ts(SessionKind.WEAR, at(15, 40), null),
        )
        val now = at(16, 40)
        val totals = WearCalculator.totals(sessions, record, now)
        val expectedWorn =
            TimeUnit.HOURS.toMillis(4) + TimeUnit.MINUTES.toMillis(30) +
                TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(30) +
                TimeUnit.HOURS.toMillis(1)
        assertThat(totals.wornMillis).isEqualTo(expectedWorn)
        assertThat(totals.notWornMillis).isEqualTo((now - at(0, 0)) - expectedWorn)
        assertThat(totals.notWornMillis).isAtLeast(TimeUnit.MINUTES.toMillis(40))
    }

    @Test
    fun activityTimerExpiration() {
        val start = at(12, 0)
        val session = ts(
            SessionKind.REMOVAL,
            start,
            configured = TimeUnit.MINUTES.toMillis(30),
        )
        val atExpiry = start + TimeUnit.MINUTES.toMillis(30)
        val snap = ActivityTimerCalculator.snapshot(session, atExpiry)
        assertThat(snap.remainingMillis).isEqualTo(0)
        assertThat(snap.overdue).isFalse()

        val later = atExpiry + TimeUnit.SECONDS.toMillis(7)
        val over = ActivityTimerCalculator.snapshot(session, later)
        assertThat(over.overdue).isTrue()
        assertThat(over.overdueMillis).isEqualTo(TimeUnit.SECONDS.toMillis(7))
        assertThat(ActivityTimerCalculator.expiryEpochMillis(session)).isEqualTo(atExpiry)
    }

    @Test
    fun pauseAndResume() {
        val start = at(12, 0)
        var session = ts(SessionKind.REMOVAL, start, configured = TimeUnit.MINUTES.toMillis(30))
        val pauseAt = start + TimeUnit.MINUTES.toMillis(8)
        session = ActivityTimerCalculator.withPauseStarted(session, pauseAt)
        val duringPause = pauseAt + TimeUnit.MINUTES.toMillis(10)
        val pausedSnap = ActivityTimerCalculator.snapshot(session, duringPause)
        assertThat(pausedSnap.paused).isTrue()
        assertThat(pausedSnap.remainingMillis).isEqualTo(TimeUnit.MINUTES.toMillis(22))
        assertThat(ActivityTimerCalculator.expiryEpochMillis(session)).isNull()

        session = ActivityTimerCalculator.withResumed(session, duringPause)
        val afterResume = duringPause + TimeUnit.MINUTES.toMillis(5)
        val resumed = ActivityTimerCalculator.snapshot(session, afterResume)
        assertThat(resumed.paused).isFalse()
        assertThat(resumed.remainingMillis).isEqualTo(TimeUnit.MINUTES.toMillis(17))
        assertThat(ActivityTimerCalculator.expiryEpochMillis(session))
            .isEqualTo(duringPause + TimeUnit.MINUTES.toMillis(22))
    }

    @Test
    fun midnightCrossingSplitsWearAcrossDays() {
        val sessions = listOf(
            ts(SessionKind.WEAR, at(20, 0), at(0, 10, date = day.plusDays(1))),
        )
        val endOfDay = at(23, 59, 59)
        val day1 = WearCalculator.totals(sessions, record, at(0, 10, date = day.plusDays(1)))
        assertThat(day1.wornMillis).isEqualTo(at(0, 0, date = day.plusDays(1)) - at(20, 0))

        val day2Record = DailyRecord(day.plusDays(1), zone.id, 22 * 60)
        val day2 = WearCalculator.totals(sessions, day2Record, at(0, 10, date = day.plusDays(1)))
        assertThat(day2.wornMillis).isEqualTo(TimeUnit.MINUTES.toMillis(10))
        assertThat(endOfDay).isLessThan(at(0, 0, date = day.plusDays(1)))
    }

    @Test
    fun midnightCrossingRemovalAllocatedPerDay() {
        val sessions = listOf(
            ts(SessionKind.WEAR, at(8, 0), at(23, 50)),
            ts(SessionKind.REMOVAL, at(23, 50), at(0, 10, date = day.plusDays(1)), "other", TimeUnit.MINUTES.toMillis(30)),
            ts(SessionKind.WEAR, at(0, 10, date = day.plusDays(1)), null),
        )
        val later = at(1, 0, date = day.plusDays(1))
        val day1 = WearCalculator.totals(sessions, record, later)
        val expectedDay1Wear = at(23, 50) - at(8, 0)
        assertThat(day1.wornMillis).isEqualTo(expectedDay1Wear)

        val day2 = WearCalculator.totals(sessions, DailyRecord(day.plusDays(1), zone.id, 22 * 60), later)
        assertThat(day2.wornMillis).isEqualTo(TimeUnit.MINUTES.toMillis(50))
        assertThat(day2.notWornMillis).isEqualTo(TimeUnit.MINUTES.toMillis(10))
    }

    @Test
    fun appRestartRecoversRemainingFromTimestamps() {
        val start = at(12, 0)
        val session = ts(SessionKind.REMOVAL, start, configured = TimeUnit.MINUTES.toMillis(30))
        val afterRestart = start + TimeUnit.MINUTES.toMillis(13)
        val snap = ActivityTimerCalculator.snapshot(session, afterRestart)
        assertThat(snap.remainingMillis).isEqualTo(TimeUnit.MINUTES.toMillis(17))
    }

    @Test
    fun editingHistoricalSessionChangesWornTime() {
        val original = listOf(ts(SessionKind.WEAR, at(8, 0), at(12, 0)))
        val edited = original.map { it.copy(endMillis = at(13, 0)) }
        val now = at(18, 0)
        val before = WearCalculator.totals(original, record, now).wornMillis
        val after = WearCalculator.totals(edited, record, now).wornMillis
        assertThat(after - before).isEqualTo(TimeUnit.HOURS.toMillis(1))
        assertThat(SessionEditor.validateNoOverlap(edited)).isTrue()
    }

    @Test
    fun dailyTargetReachedAndRemaining() {
        val sessions = listOf(ts(SessionKind.WEAR, at(0, 0), at(22, 0)))
        val now = at(22, 30)
        val totals = WearCalculator.totals(sessions, record, now)
        assertThat(totals.targetReached).isTrue()
        assertThat(totals.remainingMillis).isEqualTo(0)
        assertThat(totals.progress).isAtLeast(1f)
    }

    @Test
    fun historicalTargetIsNotRecomputedWhenSettingsChange() {
        val historical = DailyRecord(day.minusDays(1), zone.id, 20 * 60)
        val sessions = listOf(
            ts(SessionKind.WEAR, at(0, 0, date = day.minusDays(1)), at(21, 0, date = day.minusDays(1))),
        )
        val now = at(12, 0)
        val totals = WearCalculator.totals(sessions, historical, now)
        assertThat(totals.targetMillis).isEqualTo(TimeUnit.HOURS.toMillis(20))
        assertThat(totals.targetReached).isTrue()
    }

    @Test
    fun timezoneChangeUsesStoredDayZone() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        val tokyoRecord = DailyRecord(day, tokyo.id, 22 * 60)
        val sessions = listOf(
            ts(SessionKind.WEAR, at(22, 0), at(2, 0, date = day.plusDays(1))),
        )
        val now = at(12, 0, date = day.plusDays(1))
        val nyTotals = WearCalculator.totals(sessions, record, now)
        val tokyoTotals = WearCalculator.totals(sessions, tokyoRecord, now)
        assertThat(nyTotals.wornMillis).isNotEqualTo(tokyoTotals.wornMillis)
    }

    @Test
    fun daylightSavingSpringForwardShortensDay() {
        val dst = LocalDate.of(2026, 3, 8)
        val dstRecord = DailyRecord(dst, zone.id, 22 * 60)
        val window = WearCalculator.dayWindow(dstRecord)
        assertThat(window.lengthMillis).isEqualTo(TimeUnit.HOURS.toMillis(23))
        val sessions = listOf(
            ts(
                SessionKind.WEAR,
                ZonedDateTime.of(dst, java.time.LocalTime.of(0, 0), zone).toInstant().toEpochMilli(),
                ZonedDateTime.of(dst.plusDays(1), java.time.LocalTime.of(0, 0), zone).toInstant().toEpochMilli(),
            ),
        )
        val now = window.endMillis + 1
        val totals = WearCalculator.totals(sessions, dstRecord, now)
        assertThat(totals.wornMillis).isEqualTo(TimeUnit.HOURS.toMillis(23))
        assertThat(totals.elapsedMillis).isEqualTo(TimeUnit.HOURS.toMillis(23))
    }

    @Test
    fun overlappingEditIsRejected() {
        val sessions = listOf(
            ts(SessionKind.WEAR, at(8, 0), at(12, 0), id = "a"),
            ts(SessionKind.WEAR, at(11, 0), at(13, 0), id = "b"),
        )
        assertThat(SessionEditor.validateNoOverlap(sessions)).isFalse()
    }

    @Test
    fun multipleEventsSameDayTimelineOrder() {
        val sessions = listOf(
            ts(SessionKind.WEAR, at(15, 40), at(18, 0), id = "c"),
            ts(SessionKind.REMOVAL, at(12, 30), at(13, 0), id = "b"),
            ts(SessionKind.WEAR, at(8, 0), at(12, 30), id = "a"),
        )
        val segs = WearCalculator.segmentsForDay(sessions, record, at(18, 0))
        assertThat(segs.map { it.session.id }).containsExactly("a", "b", "c").inOrder()
    }

    @Test
    fun streakCountsConsecutiveTargetDays() {
        val t1 = WearCalculator.totals(
            listOf(ts(SessionKind.WEAR, at(0, 0), at(22, 0))),
            record,
            at(23, 0),
        )
        val prev = DailyRecord(day.minusDays(1), zone.id, 22 * 60)
        val t0 = WearCalculator.totals(
            listOf(ts(SessionKind.WEAR, at(0, 0, date = day.minusDays(1)), at(22, 0, date = day.minusDays(1)))),
            prev,
            at(23, 0),
        )
        assertThat(WearCalculator.streak(listOf(t0, t1), day)).isEqualTo(2)
    }

    @Test
    fun snoozeExtendsConfiguredDuration() {
        val start = at(12, 0)
        var session = ts(SessionKind.REMOVAL, start, configured = TimeUnit.MINUTES.toMillis(30))
        val expiry = ActivityTimerCalculator.expiryEpochMillis(session)!!
        session = ActivityTimerCalculator.withSnooze(session, TimeUnit.MINUTES.toMillis(5))
        assertThat(ActivityTimerCalculator.expiryEpochMillis(session))
            .isEqualTo(expiry + TimeUnit.MINUTES.toMillis(5))
    }
}
