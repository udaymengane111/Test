package app.worn.data

import app.worn.data.local.DailyRecordEntity
import app.worn.data.local.SettingsEntity
import app.worn.data.local.WornDatabase
import app.worn.domain.engine.ActivityTimerCalculator
import app.worn.domain.engine.InstantClock
import app.worn.domain.engine.SessionEditor
import app.worn.domain.engine.SystemClock
import app.worn.domain.model.ActivityType
import app.worn.domain.model.AlignerSet
import app.worn.domain.model.DailyRecord
import app.worn.domain.model.DefaultActivities
import app.worn.domain.model.SessionKind
import app.worn.domain.model.TrackingSession
import app.worn.domain.model.UserSettings
import app.worn.domain.model.toLocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class AppSnapshot(
    val settings: UserSettings,
    val activities: List<ActivityType>,
    val sessions: List<TrackingSession>,
    val days: List<DailyRecord>,
    val alignerSets: List<AlignerSet>,
    val openSession: TrackingSession?,
    val currentAligner: AlignerSet?,
)

class TrackingRepository(
    private val db: WornDatabase,
    private val clock: InstantClock = SystemClock,
    private val zoneProvider: () -> ZoneId = { ZoneId.systemDefault() },
) {
    val snapshot: Flow<AppSnapshot> = combine(
        db.settings().observe(),
        db.activities().observeAll(),
        db.sessions().observeAll(),
        db.days().observeAll(),
        db.aligners().observeAll(),
    ) { settings, activities, sessions, days, sets ->
        val mappedSessions = sessions.map { it.toModel() }
        AppSnapshot(
            settings = settings?.toModel() ?: defaultSettings(),
            activities = activities.map { it.toModel() },
            sessions = mappedSessions,
            days = days.map { it.toModel() },
            alignerSets = sets.map { it.toModel() },
            openSession = mappedSessions.firstOrNull { it.isOpen },
            currentAligner = sets.map { it.toModel() }.firstOrNull { it.endDate == null },
        )
    }

    fun observeActivities(): Flow<List<ActivityType>> =
        db.activities().observeAll().map { list -> list.map { it.toModel() } }

    suspend fun initialize() {
        val existing = db.settings().get()
        if (existing == null) {
            db.settings().upsert(
                SettingsEntity(
                    dailyWearTargetMinutes = 22 * 60,
                    onboardingComplete = false,
                    notificationsEnabled = false,
                    currentZoneId = zoneProvider().id,
                ),
            )
        } else {
            db.settings().upsert(existing.copy(currentZoneId = zoneProvider().id))
        }
        if (db.activities().getAll().isEmpty()) {
            db.activities().upsertAll(DefaultActivities.seed().map { it.toEntity() })
        }
    }

    suspend fun completeOnboarding(
        targetMinutes: Int,
        activityDurations: Map<String, Int>,
        firstAlignerSet: Int?,
    ) {
        val zone = zoneProvider()
        db.settings().upsert(
            SettingsEntity(
                dailyWearTargetMinutes = targetMinutes,
                onboardingComplete = true,
                notificationsEnabled = false,
                currentZoneId = zone.id,
            ),
        )
        db.activities().getAll().forEach { activity ->
            val minutes = activityDurations[activity.id] ?: activity.defaultDurationMinutes
            db.activities().upsert(activity.copy(defaultDurationMinutes = minutes))
        }
        ensureDailyRecord(LocalDate.now(zone))
        if (firstAlignerSet != null && db.aligners().getCurrent() == null) {
            db.aligners().upsert(
                AlignerSet(
                    id = UUID.randomUUID().toString(),
                    setNumber = firstAlignerSet,
                    startDate = LocalDate.now(zone),
                    endDate = null,
                    notes = "",
                ).toEntity(),
            )
        }
        startWearingIfNeeded()
    }

    suspend fun settings(): UserSettings =
        db.settings().get()?.toModel() ?: defaultSettings()

    suspend fun updateTargetMinutes(minutes: Int) {
        val current = db.settings().get() ?: return
        db.settings().upsert(current.copy(dailyWearTargetMinutes = minutes))
        val today = java.time.LocalDate.now(zoneProvider()).toString()
        val todayRecord = db.days().get(today)
        if (todayRecord != null) {
            db.days().update(todayRecord.copy(targetMinutes = minutes))
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        val current = db.settings().get() ?: return
        db.settings().upsert(current.copy(notificationsEnabled = enabled))
    }

    suspend fun ensureDailyRecord(date: LocalDate): DailyRecord {
        val existing = db.days().get(date.toString())
        if (existing != null) return existing.toModel()
        val settings = settings()
        val zone = zoneProvider()
        val entity = DailyRecordEntity(
            localDate = date.toString(),
            zoneId = zone.id,
            targetMinutes = settings.dailyWearTargetMinutes,
        )
        db.days().insertIfAbsent(entity)
        return db.days().get(date.toString())!!.toModel()
    }

    suspend fun startWearingIfNeeded(): TrackingSession {
        val open = db.sessions().getOpen()?.toModel()
        if (open != null) return open
        val now = clock.nowMillis()
        val zone = zoneProvider()
        ensureDailyRecord(now.toLocalDate(zone))
        val session = TrackingSession(
            id = UUID.randomUUID().toString(),
            kind = SessionKind.WEAR,
            startMillis = now,
            endMillis = null,
            activityTypeId = null,
            configuredDurationMillis = null,
            pauseAccumulatedMillis = 0L,
            pauseStartedMillis = null,
        )
        db.sessions().upsert(session.toEntity())
        return session
    }

    suspend fun removeAligner(activityTypeId: String): TrackingSession {
        val now = clock.nowMillis()
        val zone = zoneProvider()
        ensureDailyRecord(now.toLocalDate(zone))
        val open = db.sessions().getOpen()?.toModel()
        if (open != null && open.kind == SessionKind.REMOVAL) {
            return open
        }
        if (open != null && open.kind == SessionKind.WEAR) {
            db.sessions().upsert(open.copy(endMillis = now).toEntity())
        }
        val activity = db.activities().get(activityTypeId)?.toModel()
        val duration = (activity?.defaultDurationMinutes ?: 15) * 60_000L
        val removal = TrackingSession(
            id = UUID.randomUUID().toString(),
            kind = SessionKind.REMOVAL,
            startMillis = now,
            endMillis = null,
            activityTypeId = activityTypeId,
            configuredDurationMillis = duration,
            pauseAccumulatedMillis = 0L,
            pauseStartedMillis = null,
        )
        db.sessions().upsert(removal.toEntity())
        return removal
    }

    suspend fun putAlignerBack(): TrackingSession {
        val now = clock.nowMillis()
        val zone = zoneProvider()
        ensureDailyRecord(now.toLocalDate(zone))
        val open = db.sessions().getOpen()?.toModel()
        if (open != null && open.kind == SessionKind.REMOVAL) {
            val closed = if (open.isPaused) {
                ActivityTimerCalculator.withResumed(open, now).copy(endMillis = now)
            } else {
                open.copy(endMillis = now)
            }
            db.sessions().upsert(closed.toEntity())
        } else if (open != null && open.kind == SessionKind.WEAR) {
            return open
        }
        val wear = TrackingSession(
            id = UUID.randomUUID().toString(),
            kind = SessionKind.WEAR,
            startMillis = now,
            endMillis = null,
            activityTypeId = null,
            configuredDurationMillis = null,
            pauseAccumulatedMillis = 0L,
            pauseStartedMillis = null,
        )
        db.sessions().upsert(wear.toEntity())
        return wear
    }

    suspend fun pauseTimer(): TrackingSession? {
        val open = db.sessions().getOpen()?.toModel() ?: return null
        if (open.kind != SessionKind.REMOVAL || open.isPaused) return open
        val paused = ActivityTimerCalculator.withPauseStarted(open, clock.nowMillis())
        db.sessions().upsert(paused.toEntity())
        return paused
    }

    suspend fun resumeTimer(): TrackingSession? {
        val open = db.sessions().getOpen()?.toModel() ?: return null
        if (open.kind != SessionKind.REMOVAL || !open.isPaused) return open
        val resumed = ActivityTimerCalculator.withResumed(open, clock.nowMillis())
        db.sessions().upsert(resumed.toEntity())
        return resumed
    }

    suspend fun snooze(extraMillis: Long = 5 * 60_000L): TrackingSession? {
        val open = db.sessions().getOpen()?.toModel() ?: return null
        if (open.kind != SessionKind.REMOVAL) return open
        val running = if (open.isPaused) {
            ActivityTimerCalculator.withResumed(open, clock.nowMillis())
        } else {
            open
        }
        val snoozed = ActivityTimerCalculator.withSnooze(running, extraMillis)
        db.sessions().upsert(snoozed.toEntity())
        return snoozed
    }

    suspend fun changeOpenActivity(activityTypeId: String): TrackingSession? {
        val open = db.sessions().getOpen()?.toModel() ?: return null
        if (open.kind != SessionKind.REMOVAL) return open
        val updated = open.copy(activityTypeId = activityTypeId)
        db.sessions().upsert(updated.toEntity())
        return updated
    }

    suspend fun upsertActivity(activity: ActivityType) {
        db.activities().upsert(activity.toEntity())
    }

    suspend fun deleteCustomActivity(id: String) {
        db.activities().deleteCustom(id)
    }

    suspend fun setReplacementIntervalDays(days: Int) {
        val current = db.settings().get() ?: return
        db.settings().upsert(current.copy(replacementIntervalDays = days.coerceIn(1, 90)))
    }

    suspend fun setReplacementRemindersEnabled(enabled: Boolean) {
        val current = db.settings().get() ?: return
        db.settings().upsert(current.copy(replacementRemindersEnabled = enabled))
    }

    /**
     * Records an aligner set with an actual start date (today or historical).
     * Returns a user-facing error, or null on success.
     */
    suspend fun recordAlignerSet(setNumber: Int, startDate: LocalDate, notes: String = ""): String? {
        val today = LocalDate.now(zoneProvider())
        val existing = db.aligners().getAll().map { it.toModel() }
        val error = app.worn.domain.engine.TreatmentPlanCalculator.validateNewSet(
            existing,
            setNumber,
            startDate,
            today,
        )
        if (error != null) return error
        val prior = existing.firstOrNull { it.setNumber == setNumber }
        db.aligners().upsert(
            AlignerSet(
                id = prior?.id ?: UUID.randomUUID().toString(),
                setNumber = setNumber,
                startDate = startDate,
                endDate = null,
                notes = notes,
            ).toEntity(),
        )
        reconcileAlignerEndDates()
        return null
    }

    suspend fun reconcileAlignerEndDates() {
        val periods = app.worn.domain.engine.TreatmentPlanCalculator.derivePeriods(
            db.aligners().getAll().map { it.toModel() },
        )
        periods.forEach { period ->
            db.aligners().upsert(period.set.toEntity())
        }
    }

    suspend fun updateAlignerSet(set: AlignerSet) {
        db.aligners().upsert(set.toEntity())
    }

    suspend fun editSession(updated: TrackingSession): Boolean {
        val all = db.sessions().getAll().map { it.toModel() }
        val next = SessionEditor.replace(all, updated)
        if (!SessionEditor.validateNoOverlap(next)) return false
        ensureRecordsFor(updated)
        db.sessions().upsert(updated.toEntity())
        return true
    }

    suspend fun addSession(session: TrackingSession): Boolean {
        val all = db.sessions().getAll().map { it.toModel() } + session
        if (!SessionEditor.validateNoOverlap(all)) return false
        ensureRecordsFor(session)
        db.sessions().upsert(session.toEntity())
        return true
    }

    private suspend fun ensureRecordsFor(session: TrackingSession) {
        val zone = zoneProvider()
        ensureDailyRecord(session.startMillis.toLocalDate(zone))
        session.endMillis?.let { ensureDailyRecord(it.toLocalDate(zone)) }
    }

    suspend fun deleteSession(id: String) {
        db.sessions().delete(id)
        val open = db.sessions().getOpen()
        if (open == null) {
            startWearingIfNeeded()
        }
    }

    suspend fun openSession(): TrackingSession? = db.sessions().getOpen()?.toModel()

    suspend fun alignerSets() = db.aligners().getAll().map { it.toModel() }

    private fun defaultSettings() = UserSettings(
        dailyWearTargetMinutes = 22 * 60,
        onboardingComplete = false,
        notificationsEnabled = false,
        replacementRemindersEnabled = true,
        replacementIntervalDays = 10,
        currentZoneId = zoneProvider().id,
    )
}

fun AppSnapshot.recordFor(date: LocalDate, fallbackZone: ZoneId, fallbackTarget: Int): DailyRecord {
    return days.firstOrNull { it.localDate == date }
        ?: DailyRecord(date, fallbackZone.id, fallbackTarget)
}
