package app.worn.domain.engine

import app.worn.domain.model.AlignerSet
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TreatmentPlanCalculatorTest {

    private val zone = ZoneId.of("America/New_York")
    private val today = LocalDate.of(2026, 9, 15)

    private fun set(number: Int, start: LocalDate, id: String = number.toString()) =
        AlignerSet(id, number, start, null, "")

    @Test
    fun firstAlignerCanUseHistoricalDate() {
        val error = TreatmentPlanCalculator.validateNewSet(emptyList(), 1, LocalDate.of(2026, 9, 6), today)
        assertThat(error).isNull()
        val schedule = TreatmentPlanCalculator.schedule(
            listOf(set(1, LocalDate.of(2026, 9, 6))),
            10,
            today,
        )
        assertThat(schedule.current?.start).isEqualTo(LocalDate.of(2026, 9, 6))
        assertThat(schedule.current?.start).isNotEqualTo(today)
    }

    @Test
    fun historicalDateIsNotForcedToToday() {
        val start = LocalDate.of(2026, 9, 6)
        assertThat(TreatmentPlanCalculator.validateNewSet(emptyList(), 1, start, today)).isNull()
        assertThat(start).isNotEqualTo(today)
    }

    @Test
    fun multipleHistoricalSetsCanBeEntered() {
        val existing = listOf(set(1, LocalDate.of(2026, 9, 6)))
        assertThat(
            TreatmentPlanCalculator.validateNewSet(existing, 2, LocalDate.of(2026, 9, 16), LocalDate.of(2026, 9, 30)),
        ).isNull()
        assertThat(
            TreatmentPlanCalculator.validateNewSet(
                existing + set(2, LocalDate.of(2026, 9, 16)),
                3,
                LocalDate.of(2026, 9, 26),
                LocalDate.of(2026, 9, 30),
            ),
        ).isNull()
    }

    @Test
    fun actualHistoricalDateOverridesExpectedDate() {
        val sets = listOf(set(1, LocalDate.of(2026, 9, 6)), set(2, LocalDate.of(2026, 9, 15)))
        val schedule = TreatmentPlanCalculator.schedule(sets, 10, LocalDate.of(2026, 9, 20))
        assertThat(schedule.expectedNextDate).isEqualTo(LocalDate.of(2026, 9, 25))
        assertThat(schedule.expectedNextDate).isNotEqualTo(LocalDate.of(2026, 9, 26))
    }

    @Test
    fun nextReplacementRecalculatesAfterHistoricalEntry() {
        val before = TreatmentPlanCalculator.schedule(
            listOf(set(1, LocalDate.of(2026, 9, 6)), set(2, LocalDate.of(2026, 9, 16))),
            10,
            LocalDate.of(2026, 9, 20),
        )
        assertThat(before.expectedNextDate).isEqualTo(LocalDate.of(2026, 9, 26))
        val after = TreatmentPlanCalculator.schedule(
            listOf(set(1, LocalDate.of(2026, 9, 6)), set(2, LocalDate.of(2026, 9, 16)), set(3, LocalDate.of(2026, 9, 25))),
            10,
            LocalDate.of(2026, 9, 26),
        )
        assertThat(after.expectedNextDate).isEqualTo(LocalDate.of(2026, 10, 5))
        assertThat(after.isOverdue).isFalse()
    }

    @Test
    fun invalidSetOrderingIsDetected() {
        val existing = listOf(set(8, LocalDate.of(2026, 9, 12)))
        val error = TreatmentPlanCalculator.validateNewSet(existing, 9, LocalDate.of(2026, 9, 10), today)
        assertThat(error).contains("Set 9 starts before Set 8")
    }

    @Test
    fun futureAlignerDatesAreRejected() {
        val error = TreatmentPlanCalculator.validateNewSet(emptyList(), 1, today.plusDays(1), today)
        assertThat(error).contains("today or a past date")
    }

    @Test
    fun derivedEndDatesUseDayBeforeNextStart() {
        val periods = TreatmentPlanCalculator.derivePeriods(
            listOf(set(1, LocalDate.of(2026, 9, 6)), set(2, LocalDate.of(2026, 9, 16))),
        )
        assertThat(periods[0].endInclusive).isEqualTo(LocalDate.of(2026, 9, 15))
        assertThat(periods[1].endInclusive).isNull()
        assertThat(periods[1].isCurrent).isTrue()
    }

    @Test
    fun tenDayIntervalCalculatesFromActualStart() {
        assertThat(TreatmentPlanCalculator.expectedReplacement(LocalDate.of(2026, 9, 12), 10))
            .isEqualTo(LocalDate.of(2026, 9, 22))
    }

    @Test
    fun sevenDayIntervalCalculatesCorrectly() {
        assertThat(TreatmentPlanCalculator.expectedReplacement(LocalDate.of(2026, 9, 12), 7))
            .isEqualTo(LocalDate.of(2026, 9, 19))
    }

    @Test
    fun customIntervalWorks() {
        assertThat(TreatmentPlanCalculator.expectedReplacement(LocalDate.of(2026, 9, 1), 14))
            .isEqualTo(LocalDate.of(2026, 9, 15))
    }

    @Test
    fun reminderDateUsesActualStartNotInstallDate() {
        val plan = TreatmentPlanCalculator.planReminder(
            sets = listOf(set(8, LocalDate.of(2026, 9, 12))),
            intervalDays = 10,
            today = LocalDate.of(2026, 9, 15),
            zone = zone,
            enabled = true,
        )
        assertThat(plan.action).isEqualTo(ReminderAction.SCHEDULE)
        assertThat(plan.reminderDate).isEqualTo(LocalDate.of(2026, 9, 22))
        assertThat(plan.triggerAtMillis)
            .isEqualTo(TreatmentPlanCalculator.reminderInstantMillis(LocalDate.of(2026, 9, 22), zone))
        assertThat(plan.triggerAtMillis)
            .isNotEqualTo(TreatmentPlanCalculator.reminderInstantMillis(LocalDate.of(2026, 9, 15), zone))
    }

    @Test
    fun startingNewSetCancelsPreviousReminderDate() {
        val oldPlan = TreatmentPlanCalculator.planReminder(
            listOf(set(8, LocalDate.of(2026, 9, 12))),
            10,
            LocalDate.of(2026, 9, 15),
            zone,
            true,
        )
        val newPlan = TreatmentPlanCalculator.planReminder(
            listOf(set(8, LocalDate.of(2026, 9, 12)), set(9, LocalDate.of(2026, 9, 15))),
            10,
            LocalDate.of(2026, 9, 15),
            zone,
            true,
        )
        assertThat(oldPlan.reminderDate).isEqualTo(LocalDate.of(2026, 9, 22))
        assertThat(newPlan.reminderDate).isEqualTo(LocalDate.of(2026, 9, 25))
        assertThat(newPlan.alarmRequestCode).isEqualTo(oldPlan.alarmRequestCode)
    }

    @Test
    fun overdueReplacementIsDetected() {
        val schedule = TreatmentPlanCalculator.schedule(
            listOf(set(8, LocalDate.of(2026, 9, 1))),
            10,
            LocalDate.of(2026, 9, 12),
        )
        assertThat(schedule.expectedNextDate).isEqualTo(LocalDate.of(2026, 9, 11))
        assertThat(schedule.expectedNextSetNumber).isEqualTo(9)
        assertThat(schedule.isOverdue).isTrue()
        assertThat(schedule.overdueDays).isEqualTo(1)
    }

    @Test
    fun historicalCorrectionRemovesOverdueAndReschedules() {
        val overdue = TreatmentPlanCalculator.schedule(
            listOf(set(8, LocalDate.of(2026, 9, 1))),
            10,
            LocalDate.of(2026, 9, 15),
        )
        assertThat(overdue.isOverdue).isTrue()
        val corrected = TreatmentPlanCalculator.schedule(
            listOf(set(8, LocalDate.of(2026, 9, 1)), set(9, LocalDate.of(2026, 9, 11))),
            10,
            LocalDate.of(2026, 9, 15),
        )
        assertThat(corrected.isOverdue).isFalse()
        assertThat(corrected.expectedNextDate).isEqualTo(LocalDate.of(2026, 9, 21))
        val reminder = TreatmentPlanCalculator.planReminder(
            listOf(set(8, LocalDate.of(2026, 9, 1)), set(9, LocalDate.of(2026, 9, 11))),
            10,
            LocalDate.of(2026, 9, 15),
            zone,
            true,
        )
        assertThat(reminder.action).isEqualTo(ReminderAction.SCHEDULE)
        assertThat(reminder.reminderDate).isEqualTo(LocalDate.of(2026, 9, 21))
    }

    @Test
    fun reminderUsesSingleAlarmRequestCode() {
        val a = TreatmentPlanCalculator.planReminder(listOf(set(1, LocalDate.of(2026, 9, 6))), 10, today, zone, true)
        val b = TreatmentPlanCalculator.planReminder(listOf(set(1, LocalDate.of(2026, 9, 6))), 10, today, zone, true)
        assertThat(a.alarmRequestCode).isEqualTo(ReplacementReminderPlan.ALARM_REQUEST_CODE)
        assertThat(a.alarmRequestCode).isEqualTo(b.alarmRequestCode)
        assertThat(TreatmentPlanCalculator.shouldPostNotice(today, today)).isFalse()
        assertThat(TreatmentPlanCalculator.shouldPostNotice(null, today)).isTrue()
        assertThat(TreatmentPlanCalculator.shouldPostNotice(today.minusDays(1), today)).isTrue()
    }

    @Test
    fun dueTodayIsVisibleWithoutBeingOverdue() {
        val schedule = TreatmentPlanCalculator.schedule(
            listOf(set(8, LocalDate.of(2026, 9, 6))),
            10,
            LocalDate.of(2026, 9, 16),
        )
        assertThat(schedule.dueToday).isTrue()
        assertThat(schedule.isOverdue).isFalse()
        assertThat(schedule.daysUntilReplacement).isEqualTo(0)
    }

    @Test
    fun snoozeDoesNotChangeTreatmentDueDate() {
        val due = LocalDate.of(2026, 9, 16)
        val schedule = TreatmentPlanCalculator.schedule(listOf(set(8, LocalDate.of(2026, 9, 6))), 10, due)
        val snoozed = TreatmentPlanCalculator.planReminder(
            sets = listOf(set(8, LocalDate.of(2026, 9, 6))),
            intervalDays = 10,
            today = due,
            zone = zone,
            enabled = true,
            snoozeUntil = due.plusDays(1),
        )
        assertThat(schedule.expectedNextDate).isEqualTo(due)
        assertThat(snoozed.reminderDate).isEqualTo(due)
        assertThat(snoozed.action).isEqualTo(ReminderAction.SCHEDULE)
        assertThat(snoozed.triggerAtMillis)
            .isEqualTo(TreatmentPlanCalculator.reminderInstantMillis(due.plusDays(1), zone))
        val nextDay = TreatmentPlanCalculator.schedule(listOf(set(8, LocalDate.of(2026, 9, 6))), 10, due.plusDays(1))
        assertThat(nextDay.expectedNextDate).isEqualTo(due)
        assertThat(nextDay.isOverdue).isTrue()
        assertThat(nextDay.overdueDays).isEqualTo(1)
    }
}
