package app.worn.domain.engine

import app.worn.domain.model.DailyRecord
import app.worn.domain.model.DefaultActivities
import app.worn.domain.model.SessionKind
import app.worn.domain.model.TrackingSession
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

class UserFlowTest {

    private val zone = ZoneId.of("UTC")
    private val day = LocalDate.of(2026, 9, 15)
    private val record = DailyRecord(day, zone.id, 22 * 60)

    private fun at(h: Int, m: Int, s: Int = 0) =
        ZonedDateTime.of(day, java.time.LocalTime.of(h, m, s), zone).toInstant().toEpochMilli()

    @Test
    fun firstLaunchThroughLunchPauseTeaAndHistory() {
        val sessions = mutableListOf<TrackingSession>()

        fun open() = sessions.first { it.endMillis == null }
        fun closeOpen(now: Long) {
            val current = open()
            sessions[sessions.indexOfFirst { it.id == current.id }] = current.copy(endMillis = now)
        }

        // Start wearing after setup
        sessions += TrackingSession(UUID.randomUUID().toString(), SessionKind.WEAR, at(8, 0), null, null, null, 0, null)

        // Remove for lunch 30 min
        closeOpen(at(12, 0))
        sessions += TrackingSession(
            UUID.randomUUID().toString(),
            SessionKind.REMOVAL,
            at(12, 0),
            null,
            DefaultActivities.LUNCH,
            TimeUnit.MINUTES.toMillis(30),
            0,
            null,
        )
        var lunch = open()
        assertThat(ActivityTimerCalculator.snapshot(lunch, at(12, 0)).remainingMillis)
            .isEqualTo(TimeUnit.MINUTES.toMillis(30))

        // Pause after 8 minutes, resume after 5, then put back
        lunch = ActivityTimerCalculator.withPauseStarted(lunch, at(12, 8))
        sessions[sessions.indexOfFirst { it.id == lunch.id }] = lunch
        assertThat(ActivityTimerCalculator.snapshot(lunch, at(12, 20)).remainingMillis)
            .isEqualTo(TimeUnit.MINUTES.toMillis(22))
        lunch = ActivityTimerCalculator.withResumed(lunch, at(12, 13))
        sessions[sessions.indexOfFirst { it.id == lunch.id }] = lunch
        closeOpen(at(12, 20))
        sessions += TrackingSession(UUID.randomUUID().toString(), SessionKind.WEAR, at(12, 20), null, null, null, 0, null)

        // Remove for tea 10 min — timer expires, then overdue, then put back
        closeOpen(at(15, 0))
        sessions += TrackingSession(
            UUID.randomUUID().toString(),
            SessionKind.REMOVAL,
            at(15, 0),
            null,
            DefaultActivities.TEA,
            TimeUnit.MINUTES.toMillis(10),
            0,
            null,
        )
        val tea = open()
        val expiry = ActivityTimerCalculator.expiryEpochMillis(tea)!!
        assertThat(expiry).isEqualTo(at(15, 10))
        assertThat(ActivityTimerCalculator.snapshot(tea, at(15, 10, 7)).overdue).isTrue()
        closeOpen(at(15, 12))
        sessions += TrackingSession(UUID.randomUUID().toString(), SessionKind.WEAR, at(15, 12), null, null, null, 0, null)

        val now = at(16, 0)
        val totals = WearCalculator.totals(sessions, record, now)
        val expectedWorn =
            (at(12, 0) - at(8, 0)) + (at(15, 0) - at(12, 20)) + (at(16, 0) - at(15, 12))
        assertThat(totals.wornMillis).isEqualTo(expectedWorn)
        assertThat(totals.targetReached).isFalse()

        val history = WearCalculator.segmentsForDay(sessions, record, now)
        assertThat(history.map { it.session.kind }).containsExactly(
            SessionKind.WEAR,
            SessionKind.REMOVAL,
            SessionKind.WEAR,
            SessionKind.REMOVAL,
            SessionKind.WEAR,
        ).inOrder()
    }
}
