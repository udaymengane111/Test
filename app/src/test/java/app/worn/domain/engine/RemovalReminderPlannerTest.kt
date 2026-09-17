package app.worn.domain.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RemovalReminderPlannerTest {

    private val expiry = 1_000_000L

    @Test
    fun timerExpirationSchedulesFirstAlertAtExpiry() {
        val next = RemovalReminderPlanner.nextTriggerMillis(expiry, expiry - 1, muted = false, paused = false, sessionOpen = true)
        assertThat(next).isEqualTo(expiry)
        assertThat(RemovalReminderPlanner.shouldPostAlert(expiry, expiry, muted = false, paused = false, sessionOpen = true, lastPostedIndex = null)).isTrue()
    }

    @Test
    fun reminderRepeatsEveryFiveMinutes() {
        val five = RemovalReminderPlanner.INTERVAL_MS
        val atExpiry = RemovalReminderPlanner.nextTriggerMillis(expiry, expiry, false, false, true)
        assertThat(atExpiry).isEqualTo(expiry + five)
        val atPlusOne = RemovalReminderPlanner.nextTriggerMillis(expiry, expiry + 1, false, false, true)
        assertThat(atPlusOne).isEqualTo(expiry + five)
        val atFive = RemovalReminderPlanner.nextTriggerMillis(expiry, expiry + five, false, false, true)
        assertThat(atFive).isEqualTo(expiry + 2 * five)
        assertThat(RemovalReminderPlanner.alertIndex(expiry, expiry + five)).isEqualTo(1)
        assertThat(RemovalReminderPlanner.alertIndex(expiry, expiry + 2 * five)).isEqualTo(2)
    }

    @Test
    fun reminderStopsAfterPutBack() {
        assertThat(RemovalReminderPlanner.stopsAfterPutBack(sessionOpen = false)).isTrue()
        assertThat(
            RemovalReminderPlanner.nextTriggerMillis(expiry, expiry + 1, muted = false, paused = false, sessionOpen = false),
        ).isNull()
        assertThat(
            RemovalReminderPlanner.shouldPostAlert(expiry, expiry, muted = false, paused = false, sessionOpen = false, lastPostedIndex = null),
        ).isFalse()
    }

    @Test
    fun reminderStopsAfterMute() {
        assertThat(
            RemovalReminderPlanner.nextTriggerMillis(expiry, expiry, muted = true, paused = false, sessionOpen = true),
        ).isNull()
        assertThat(
            RemovalReminderPlanner.shouldPostAlert(expiry, expiry, muted = true, paused = false, sessionOpen = true, lastPostedIndex = null),
        ).isFalse()
    }

    @Test
    fun mutingDoesNotStopTracking() {
        assertThat(RemovalReminderPlanner.muteStopsSoundOnly(muted = true, sessionStillOpen = true)).isTrue()
    }

    @Test
    fun resumeAfterPauseDoesNotAlertWhilePaused() {
        assertThat(
            RemovalReminderPlanner.nextTriggerMillis(expiry, expiry, muted = false, paused = true, sessionOpen = true),
        ).isNull()
        val afterResume = RemovalReminderPlanner.nextTriggerMillis(expiry, expiry - 5_000, muted = false, paused = false, sessionOpen = true)
        assertThat(afterResume).isEqualTo(expiry)
    }

    @Test
    fun backgroundSchedulingUsesExpiryTimestampNotUi() {
        val scheduled = RemovalReminderPlanner.nextTriggerMillis(expiry, expiry - 60_000, false, false, true)
        assertThat(scheduled).isEqualTo(expiry)
    }

    @Test
    fun noDuplicateFiveMinuteReminders() {
        assertThat(
            RemovalReminderPlanner.shouldPostAlert(expiry, expiry, false, false, true, lastPostedIndex = 0),
        ).isFalse()
        assertThat(
            RemovalReminderPlanner.shouldPostAlert(expiry, expiry + RemovalReminderPlanner.INTERVAL_MS, false, false, true, lastPostedIndex = 0),
        ).isTrue()
        assertThat(
            RemovalReminderPlanner.shouldPostAlert(expiry, expiry + RemovalReminderPlanner.INTERVAL_MS + 1, false, false, true, lastPostedIndex = 1),
        ).isFalse()
    }

    @Test
    fun putBackActionClosesReminders() {
        assertThat(RemovalReminderPlanner.shouldPostAlert(expiry, expiry, false, false, sessionOpen = false, lastPostedIndex = null)).isFalse()
    }

    @Test
    fun repeatsCanBeDisabledWithoutBlockingFirstAlert() {
        val expiry = 1_000_000L
        assertThat(
            RemovalReminderPlanner.nextTriggerMillis(expiry, expiry - 1, muted = false, paused = false, sessionOpen = true, repeatsEnabled = false),
        ).isEqualTo(expiry)
        assertThat(
            RemovalReminderPlanner.shouldPostAlert(expiry, expiry, muted = false, paused = false, sessionOpen = true, lastPostedIndex = null, repeatsEnabled = false),
        ).isTrue()
        assertThat(
            RemovalReminderPlanner.nextTriggerMillis(expiry, expiry, muted = false, paused = false, sessionOpen = true, repeatsEnabled = false),
        ).isNull()
        assertThat(
            RemovalReminderPlanner.shouldPostAlert(expiry, expiry + RemovalReminderPlanner.INTERVAL_MS, muted = false, paused = false, sessionOpen = true, lastPostedIndex = 0, repeatsEnabled = false),
        ).isFalse()
    }
}
