package app.worn.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val dailyWearTargetMinutes: Int,
    val onboardingComplete: Boolean,
    val notificationsEnabled: Boolean,
    val replacementRemindersEnabled: Boolean = true,
    val replacementIntervalDays: Int = 10,
    val currentZoneId: String,
)

@Entity(tableName = "activity_types")
data class ActivityTypeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val defaultDurationMinutes: Int,
    val iconKey: String,
    val enabled: Boolean,
    val isDefault: Boolean,
    val sortOrder: Int,
)

@Entity(
    tableName = "sessions",
    indices = [Index("startMillis"), Index("kind")],
)
data class SessionEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val startMillis: Long,
    val endMillis: Long?,
    val activityTypeId: String?,
    val configuredDurationMillis: Long?,
    val pauseAccumulatedMillis: Long,
    val pauseStartedMillis: Long?,
)

@Entity(tableName = "daily_records")
data class DailyRecordEntity(
    @PrimaryKey val localDate: String,
    val zoneId: String,
    val targetMinutes: Int,
)

@Entity(tableName = "aligner_sets")
data class AlignerSetEntity(
    @PrimaryKey val id: String,
    val setNumber: Int,
    val startDate: String,
    val endDate: String?,
    val notes: String,
)
