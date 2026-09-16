package app.worn.domain.engine

import app.worn.domain.model.ActivityType
import app.worn.domain.model.DailyRecord
import app.worn.domain.model.DefaultActivities
import app.worn.domain.model.SessionKind
import app.worn.domain.model.TrackingSession
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class ReportCalculatorTest {

    private val zone = ZoneId.of("America/New_York")
    private val tea = ActivityType(DefaultActivities.TEA, "Tea", 10, "tea", true, true, 0)
    private val lunch = ActivityType(DefaultActivities.LUNCH, "Lunch / Dinner", 30, "restaurant", true, true, 1)

    private fun at(date: LocalDate, hour: Int, minute: Int = 0): Long =
        ZonedDateTime.of(date, LocalTime.of(hour, minute), zone).toInstant().toEpochMilli()

    private fun wear(date: LocalDate, startH: Int, endH: Int, endM: Int = 0, id: String = "$date-$startH") =
        TrackingSession(id, SessionKind.WEAR, at(date, startH), at(date, endH, endM), null, null, 0, null)

    private fun removal(
        date: LocalDate,
        startH: Int,
        startM: Int,
        endH: Int,
        endM: Int,
        activity: String,
        configuredMin: Int,
        id: String = "$date-$activity-$startH$startM",
    ) = TrackingSession(
        id,
        SessionKind.REMOVAL,
        at(date, startH, startM),
        at(date, endH, endM),
        activity,
        TimeUnit.MINUTES.toMillis(configuredMin.toLong()),
        0,
        null,
    )

    private fun record(date: LocalDate, targetHours: Int) = DailyRecord(date, zone.id, targetHours * 60)

    @Test
    fun dailyWearReport() {
        val day = LocalDate.of(2026, 9, 15)
        val sessions = listOf(wear(day, 0, 20, 42))
        val report = ReportCalculator.report(
            ReportGrain.DAY,
            day,
            day,
            at(day, 23, 0),
            sessions,
            { record(it, 22) },
            listOf(tea),
        )
        assertThat(report.totalWornMillis).isEqualTo(TimeUnit.HOURS.toMillis(20) + TimeUnit.MINUTES.toMillis(42))
        assertThat(report.daysWithData).hasSize(1)
    }

    @Test
    fun weeklyWearReportUsesMondayToSunday() {
        val wed = LocalDate.of(2026, 9, 16)
        val window = ReportCalculator.window(ReportGrain.WEEK, wed)
        assertThat(window.start).isEqualTo(LocalDate.of(2026, 9, 14))
        assertThat(window.endInclusive).isEqualTo(LocalDate.of(2026, 9, 20))
        val sessions = listOf(wear(LocalDate.of(2026, 9, 14), 0, 21), wear(LocalDate.of(2026, 9, 15), 0, 22))
        val report = ReportCalculator.report(
            ReportGrain.WEEK,
            wed,
            LocalDate.of(2026, 9, 16),
            at(LocalDate.of(2026, 9, 16), 12),
            sessions,
            { record(it, 22) },
            emptyList(),
        )
        assertThat(report.totalWornMillis).isEqualTo(TimeUnit.HOURS.toMillis(43))
        assertThat(report.daysWithData).hasSize(2)
    }

    @Test
    fun monthlyWearReportUsesCalendarMonth() {
        val window = ReportCalculator.window(ReportGrain.MONTH, LocalDate.of(2026, 9, 15))
        assertThat(window.start).isEqualTo(LocalDate.of(2026, 9, 1))
        assertThat(window.endInclusive).isEqualTo(LocalDate.of(2026, 9, 30))
    }

    @Test
    fun quarterlyReportUsesCalendarQuarters() {
        val q3 = ReportCalculator.window(ReportGrain.QUARTER, LocalDate.of(2026, 9, 15))
        assertThat(q3.start).isEqualTo(LocalDate.of(2026, 7, 1))
        assertThat(q3.endInclusive).isEqualTo(LocalDate.of(2026, 9, 30))
        val q1 = ReportCalculator.window(ReportGrain.QUARTER, LocalDate.of(2026, 2, 1))
        assertThat(q1.start.monthValue).isEqualTo(1)
        assertThat(q1.endInclusive.monthValue).isEqualTo(3)
    }

    @Test
    fun yearlyReportUsesCalendarYear() {
        val window = ReportCalculator.window(ReportGrain.YEAR, LocalDate.of(2026, 9, 15))
        assertThat(window.start).isEqualTo(LocalDate.of(2026, 1, 1))
        assertThat(window.endInclusive).isEqualTo(LocalDate.of(2026, 12, 31))
    }

    @Test
    fun missedTimeIsSummedPerDay() {
        val d1 = LocalDate.of(2026, 9, 10)
        val d2 = LocalDate.of(2026, 9, 11)
        val sessions = listOf(wear(d1, 0, 20), wear(d2, 0, 22))
        val report = ReportCalculator.report(
            ReportGrain.WEEK,
            d1,
            d2,
            at(d2, 23),
            sessions,
            { record(it, 22) },
            emptyList(),
        )
        assertThat(report.missedMillis).isEqualTo(TimeUnit.HOURS.toMillis(2))
    }

    @Test
    fun extraWearDoesNotCompensateMissedTimeOnAnotherDay() {
        val d1 = LocalDate.of(2026, 9, 10)
        val d2 = LocalDate.of(2026, 9, 11)
        val sessions = listOf(wear(d1, 0, 20), wear(d2, 0, 23))
        val report = ReportCalculator.report(
            ReportGrain.WEEK,
            d1,
            d2,
            at(d2, 23),
            sessions,
            { record(it, 22) },
            emptyList(),
        )
        assertThat(report.missedMillis).isEqualTo(TimeUnit.HOURS.toMillis(2))
        assertThat(report.totalWornMillis).isEqualTo(TimeUnit.HOURS.toMillis(43))
        val naive = TimeUnit.HOURS.toMillis(44) - report.totalWornMillis
        assertThat(report.missedMillis).isNotEqualTo(naive)
    }

    @Test
    fun historicalTargetsAreRespectedPerDay() {
        val d1 = LocalDate.of(2026, 9, 10)
        val d2 = LocalDate.of(2026, 9, 15)
        val sessions = listOf(wear(d1, 0, 22), wear(d2, 0, 21))
        val records = mapOf(d1 to record(d1, 22), d2 to record(d2, 21))
        val report = ReportCalculator.report(
            ReportGrain.MONTH,
            d1,
            d2,
            at(d2, 23),
            sessions,
            { records[it] ?: record(it, 21) },
            emptyList(),
        )
        assertThat(report.days.first { it.date == d1 }.targetMillis).isEqualTo(TimeUnit.HOURS.toMillis(22))
        assertThat(report.days.first { it.date == d2 }.targetMillis).isEqualTo(TimeUnit.HOURS.toMillis(21))
        assertThat(report.missedMillis).isEqualTo(0)
        assertThat(report.daysOnTarget).isEqualTo(2)
    }

    @Test
    fun historicalConfiguredDurationIsKeptOnTheSession() {
        val day = LocalDate.of(2026, 9, 10)
        val later = LocalDate.of(2026, 9, 20)
        val sessions = listOf(
            removal(day, 12, 0, 12, 10, DefaultActivities.TEA, 10),
            removal(later, 12, 0, 12, 15, DefaultActivities.TEA, 15),
        )
        assertThat(sessions[0].configuredDurationMillis).isEqualTo(TimeUnit.MINUTES.toMillis(10))
        assertThat(sessions[1].configuredDurationMillis).isEqualTo(TimeUnit.MINUTES.toMillis(15))
        val report = ReportCalculator.report(
            ReportGrain.MONTH,
            day,
            later,
            at(later, 23),
            sessions + wear(day, 0, 12) + wear(later, 0, 12),
            { record(it, 22) },
            listOf(tea),
        )
        val teaStat = report.removals.first { it.activityId == DefaultActivities.TEA }
        assertThat(teaStat.count).isEqualTo(2)
        assertThat(teaStat.actualMillis).isEqualTo(TimeUnit.MINUTES.toMillis(25))
    }

    @Test
    fun actualActivityDurationUsesTimestampsNotConfiguredLength() {
        val day = LocalDate.of(2026, 9, 15)
        val sessions = listOf(
            wear(day, 8, 12, 30),
            removal(day, 12, 30, 12, 42, DefaultActivities.TEA, 10),
        )
        val report = ReportCalculator.report(
            ReportGrain.DAY,
            day,
            day,
            at(day, 18),
            sessions,
            { record(it, 22) },
            listOf(tea),
        )
        assertThat(report.removals.first().actualMillis).isEqualTo(TimeUnit.MINUTES.toMillis(12))
        assertThat(report.removals.first().actualMillis).isNotEqualTo(TimeUnit.MINUTES.toMillis(10))
    }

    @Test
    fun noDataDayIsDifferentFromZeroWearDay() {
        val empty = LocalDate.of(2026, 9, 6)
        val zero = LocalDate.of(2026, 9, 15)
        val sessions = listOf(removal(zero, 8, 0, 10, 0, DefaultActivities.TEA, 10))
        val report = ReportCalculator.report(
            ReportGrain.MONTH,
            empty,
            zero,
            at(zero, 12),
            sessions,
            { record(it, 22) },
            listOf(tea),
        )
        val emptyDay = report.days.first { it.date == empty }
        val zeroDay = report.days.first { it.date == zero }
        assertThat(emptyDay.hasData).isFalse()
        assertThat(emptyDay.wornMillis).isEqualTo(0)
        assertThat(zeroDay.hasData).isTrue()
        assertThat(zeroDay.wornMillis).isEqualTo(0)
        assertThat(report.daysWithData.map { it.date }).doesNotContain(empty)
    }

    @Test
    fun incompleteCurrentDayIsNotTreatedAsFailed() {
        val today = LocalDate.of(2026, 9, 15)
        val yesterday = today.minusDays(1)
        val sessions = listOf(wear(yesterday, 0, 22), wear(today, 8, 10))
        val report = ReportCalculator.report(
            ReportGrain.WEEK,
            today,
            today,
            at(today, 10),
            sessions,
            { record(it, 22) },
            emptyList(),
        )
        assertThat(report.todayInProgress).isTrue()
        assertThat(report.days.first { it.date == today }.targetReached).isFalse()
        assertThat(report.daysOnTarget).isEqualTo(1)
        assertThat(report.completeDaysWithData).isEqualTo(1)
    }

    @Test
    fun midnightSessionsSplitAcrossCalendarDays() {
        val day = LocalDate.of(2026, 9, 15)
        val next = day.plusDays(1)
        val sessions = listOf(
            TrackingSession(
                "overnight",
                SessionKind.WEAR,
                at(day, 20),
                at(next, 0, 10),
                null,
                null,
                0,
                null,
            ),
        )
        val report = ReportCalculator.report(
            ReportGrain.WEEK,
            day,
            next,
            at(next, 1),
            sessions,
            { record(it, 22) },
            emptyList(),
        )
        val day1 = report.days.first { it.date == day }
        val day2 = report.days.first { it.date == next }
        assertThat(day1.wornMillis).isEqualTo(at(next, 0) - at(day, 20))
        assertThat(day2.wornMillis).isEqualTo(TimeUnit.MINUTES.toMillis(10))
    }

    @Test
    fun historicalManualEditsUpdateReports() {
        val day = LocalDate.of(2026, 9, 10)
        val original = listOf(wear(day, 8, 12))
        val edited = listOf(wear(day, 8, 13))
        val before = ReportCalculator.report(
            ReportGrain.DAY, day, day, at(day, 18), original, { record(it, 22) }, emptyList(),
        )
        val after = ReportCalculator.report(
            ReportGrain.DAY, day, day, at(day, 18), edited, { record(it, 22) }, emptyList(),
        )
        assertThat(after.totalWornMillis - before.totalWornMillis).isEqualTo(TimeUnit.HOURS.toMillis(1))
    }

    @Test
    fun treatmentDatesDoNotFabricateWearData() {
        val install = LocalDate.of(2026, 9, 15)
        val report = ReportCalculator.report(
            ReportGrain.MONTH,
            install,
            install,
            at(install, 12),
            emptyList(),
            { record(it, 22) },
            emptyList(),
        )
        assertThat(report.daysWithData).isEmpty()
        assertThat(report.days.first { it.date == LocalDate.of(2026, 9, 6) }.hasData).isFalse()
        assertThat(report.totalWornMillis).isEqualTo(0)
    }

    @Test
    fun quarterAndYearUseMonthlyBuckets() {
        val jul = LocalDate.of(2026, 7, 2)
        val aug = LocalDate.of(2026, 8, 2)
        val sessions = listOf(wear(jul, 0, 20), wear(aug, 0, 21))
        val quarter = ReportCalculator.report(
            ReportGrain.QUARTER,
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 16),
            at(LocalDate.of(2026, 9, 16), 12),
            sessions,
            { record(it, 22) },
            emptyList(),
        )
        assertThat(quarter.window.start).isEqualTo(LocalDate.of(2026, 7, 1))
        assertThat(quarter.monthBuckets).hasSize(3)
        assertThat(quarter.monthBuckets[0].hasData).isTrue()
        assertThat(quarter.monthBuckets[2].hasData).isFalse()
        val year = ReportCalculator.report(
            ReportGrain.YEAR,
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 16),
            at(LocalDate.of(2026, 9, 16), 12),
            sessions,
            { record(it, 22) },
            emptyList(),
        )
        assertThat(year.monthBuckets).hasSize(12)
    }
}
