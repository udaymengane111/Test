package app.worn.domain.engine

import app.worn.domain.model.AlignerSet
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class AlignerPeriod(
    val set: AlignerSet,
    val start: LocalDate,
    val endInclusive: LocalDate?,
) {
    val isCurrent: Boolean get() = endInclusive == null
}

enum class ReminderAction { NONE, NOTIFY_NOW, SCHEDULE }

data class ReplacementReminderPlan(
    val action: ReminderAction,
    val reminderDate: LocalDate?,
    val triggerAtMillis: Long?,
    val nextSetNumber: Int?,
    val overdue: Boolean,
    val alarmRequestCode: Int = ALARM_REQUEST_CODE,
) {
    companion object {
        const val ALARM_REQUEST_CODE = 24
    }
}

data class TreatmentSchedule(
    val periods: List<AlignerPeriod>,
    val current: AlignerPeriod?,
    val intervalDays: Int,
    val expectedNextDate: LocalDate?,
    val expectedNextSetNumber: Int?,
    val overdueDays: Long,
    val reminderDate: LocalDate?,
    val daysUntilReplacement: Long?,
) {
    val isOverdue: Boolean get() = overdueDays > 0L
    val dueToday: Boolean get() = daysUntilReplacement == 0L
}

object TreatmentPlanCalculator {

    fun expectedReplacement(actualStart: LocalDate, intervalDays: Int): LocalDate {
        require(intervalDays > 0)
        return actualStart.plusDays(intervalDays.toLong())
    }

    fun derivePeriods(sets: List<AlignerSet>): List<AlignerPeriod> {
        val sorted = sets.sortedWith(compareBy<AlignerSet> { it.startDate }.thenBy { it.setNumber })
        return sorted.mapIndexed { index, set ->
            val next = sorted.getOrNull(index + 1)
            AlignerPeriod(
                set = set.copy(endDate = next?.startDate?.minusDays(1)),
                start = set.startDate,
                endInclusive = next?.startDate?.minusDays(1),
            )
        }
    }

    fun schedule(
        sets: List<AlignerSet>,
        intervalDays: Int,
        today: LocalDate,
    ): TreatmentSchedule {
        val periods = derivePeriods(sets)
        val current = periods.maxByOrNull { it.start }
        val expected = current?.let { expectedReplacement(it.start, intervalDays) }
        val nextNumber = (current?.set?.setNumber ?: 0) + 1
        val overdue = if (current != null && expected != null && today.isAfter(expected)) {
            ChronoUnit.DAYS.between(expected, today)
        } else {
            0L
        }
        val until = expected?.let { ChronoUnit.DAYS.between(today, it) }
        return TreatmentSchedule(
            periods = periods,
            current = current,
            intervalDays = intervalDays,
            expectedNextDate = expected,
            expectedNextSetNumber = if (current == null) 1 else nextNumber,
            overdueDays = overdue,
            reminderDate = expected,
            daysUntilReplacement = until,
        )
    }

    fun validateNewSet(
        existing: List<AlignerSet>,
        setNumber: Int,
        startDate: LocalDate,
        today: LocalDate,
    ): String? {
        if (startDate.isAfter(today)) {
            return "Choose today or a past date."
        }
        if (setNumber <= 0) {
            return "Set number must be at least 1."
        }
        val others = existing.filterNot { it.setNumber == setNumber }
        val previous = others.filter { it.setNumber < setNumber }.maxByOrNull { it.setNumber }
        if (previous != null && !startDate.isAfter(previous.startDate)) {
            return "Set $setNumber starts before Set ${previous.setNumber}. Please check the date."
        }
        val next = others.filter { it.setNumber > setNumber }.minByOrNull { it.setNumber }
        if (next != null && !startDate.isBefore(next.startDate)) {
            return "Set $setNumber starts after Set ${next.setNumber}. Please check the date."
        }
        return null
    }

    fun reminderInstantMillis(reminderDate: LocalDate, zoneId: ZoneId, hour: Int = 9): Long {
        return reminderDate.atTime(hour, 0).atZone(zoneId).toInstant().toEpochMilli()
    }

    fun planReminder(
        sets: List<AlignerSet>,
        intervalDays: Int,
        today: LocalDate,
        zone: ZoneId,
        enabled: Boolean,
        snoozeUntil: LocalDate? = null,
    ): ReplacementReminderPlan {
        val schedule = schedule(sets, intervalDays, today)
        if (!enabled || schedule.current == null || schedule.reminderDate == null) {
            return ReplacementReminderPlan(
                action = ReminderAction.NONE,
                reminderDate = null,
                triggerAtMillis = null,
                nextSetNumber = null,
                overdue = false,
            )
        }
        val due = schedule.reminderDate
        val nextNumber = schedule.expectedNextSetNumber
        if (snoozeUntil != null && today.isBefore(snoozeUntil)) {
            return ReplacementReminderPlan(
                action = ReminderAction.SCHEDULE,
                reminderDate = due,
                triggerAtMillis = reminderInstantMillis(snoozeUntil, zone),
                nextSetNumber = nextNumber,
                overdue = schedule.isOverdue,
            )
        }
        if (!due.isAfter(today)) {
            return ReplacementReminderPlan(
                action = ReminderAction.NOTIFY_NOW,
                reminderDate = due,
                triggerAtMillis = null,
                nextSetNumber = nextNumber,
                overdue = today.isAfter(due),
            )
        }
        return ReplacementReminderPlan(
            action = ReminderAction.SCHEDULE,
            reminderDate = due,
            triggerAtMillis = reminderInstantMillis(due, zone),
            nextSetNumber = nextNumber,
            overdue = false,
        )
    }

    fun shouldPostNotice(lastPostedOn: LocalDate?, today: LocalDate): Boolean = lastPostedOn != today
}
