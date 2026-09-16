package app.worn.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class SessionKind {
    WEAR,
    REMOVAL,
}

data class ActivityType(
    val id: String,
    val name: String,
    val defaultDurationMinutes: Int,
    val iconKey: String,
    val enabled: Boolean,
    val isDefault: Boolean,
    val sortOrder: Int,
)

data class TrackingSession(
    val id: String,
    val kind: SessionKind,
    val startMillis: Long,
    val endMillis: Long?,
    val activityTypeId: String?,
    val configuredDurationMillis: Long?,
    val pauseAccumulatedMillis: Long,
    val pauseStartedMillis: Long?,
) {
    val isOpen: Boolean get() = endMillis == null
    val isPaused: Boolean get() = pauseStartedMillis != null

    fun closedEnd(nowMillis: Long): Long = endMillis ?: nowMillis
}

data class DailyRecord(
    val localDate: LocalDate,
    val zoneId: String,
    val targetMinutes: Int,
) {
    fun zone(): ZoneId = ZoneId.of(zoneId)
}

data class AlignerSet(
    val id: String,
    val setNumber: Int,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val notes: String,
)

data class UserSettings(
    val dailyWearTargetMinutes: Int,
    val onboardingComplete: Boolean,
    val notificationsEnabled: Boolean,
    val replacementRemindersEnabled: Boolean,
    val replacementIntervalDays: Int,
    val removalReminderSoundEnabled: Boolean,
    val currentZoneId: String,
)

data class PauseInterval(
    val startMillis: Long,
    val endMillis: Long?,
)

object DefaultActivities {
    const val TEA = "activity_tea"
    const val LUNCH = "activity_lunch"
    const val SNACK = "activity_snack"
    const val OTHER = "activity_other"

    fun seed(): List<ActivityType> = listOf(
        ActivityType(TEA, "Tea", 10, "tea", true, true, 0),
        ActivityType(LUNCH, "Lunch / Dinner", 30, "restaurant", true, true, 1),
        ActivityType(SNACK, "Snack", 15, "cookie", true, true, 2),
        ActivityType(OTHER, "Other", 15, "more", true, true, 3),
    )
}

fun LocalDate.toEpochMillis(zone: ZoneId): Long =
    atStartOfDay(zone).toInstant().toEpochMilli()

fun Long.toLocalDate(zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
