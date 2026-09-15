package app.worn.data

import app.worn.data.local.ActivityTypeEntity
import app.worn.data.local.AlignerSetEntity
import app.worn.data.local.DailyRecordEntity
import app.worn.data.local.SessionEntity
import app.worn.data.local.SettingsEntity
import app.worn.domain.model.ActivityType
import app.worn.domain.model.AlignerSet
import app.worn.domain.model.DailyRecord
import app.worn.domain.model.SessionKind
import app.worn.domain.model.TrackingSession
import app.worn.domain.model.UserSettings
import java.time.LocalDate

fun SettingsEntity.toModel() = UserSettings(
    dailyWearTargetMinutes = dailyWearTargetMinutes,
    onboardingComplete = onboardingComplete,
    notificationsEnabled = notificationsEnabled,
    replacementRemindersEnabled = replacementRemindersEnabled,
    replacementIntervalDays = replacementIntervalDays,
    currentZoneId = currentZoneId,
)

fun ActivityTypeEntity.toModel() = ActivityType(
    id = id,
    name = name,
    defaultDurationMinutes = defaultDurationMinutes,
    iconKey = iconKey,
    enabled = enabled,
    isDefault = isDefault,
    sortOrder = sortOrder,
)

fun ActivityType.toEntity() = ActivityTypeEntity(
    id = id,
    name = name,
    defaultDurationMinutes = defaultDurationMinutes,
    iconKey = iconKey,
    enabled = enabled,
    isDefault = isDefault,
    sortOrder = sortOrder,
)

fun SessionEntity.toModel() = TrackingSession(
    id = id,
    kind = SessionKind.valueOf(kind),
    startMillis = startMillis,
    endMillis = endMillis,
    activityTypeId = activityTypeId,
    configuredDurationMillis = configuredDurationMillis,
    pauseAccumulatedMillis = pauseAccumulatedMillis,
    pauseStartedMillis = pauseStartedMillis,
)

fun TrackingSession.toEntity() = SessionEntity(
    id = id,
    kind = kind.name,
    startMillis = startMillis,
    endMillis = endMillis,
    activityTypeId = activityTypeId,
    configuredDurationMillis = configuredDurationMillis,
    pauseAccumulatedMillis = pauseAccumulatedMillis,
    pauseStartedMillis = pauseStartedMillis,
)

fun DailyRecordEntity.toModel() = DailyRecord(
    localDate = LocalDate.parse(localDate),
    zoneId = zoneId,
    targetMinutes = targetMinutes,
)

fun AlignerSetEntity.toModel() = AlignerSet(
    id = id,
    setNumber = setNumber,
    startDate = LocalDate.parse(startDate),
    endDate = endDate?.let(LocalDate::parse),
    notes = notes,
)

fun AlignerSet.toEntity() = AlignerSetEntity(
    id = id,
    setNumber = setNumber,
    startDate = startDate.toString(),
    endDate = endDate?.toString(),
    notes = notes,
)
