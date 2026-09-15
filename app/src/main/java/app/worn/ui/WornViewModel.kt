package app.worn.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.worn.data.AppSnapshot
import app.worn.data.TrackingRepository
import app.worn.data.recordFor
import app.worn.domain.engine.ActivityTimerCalculator
import app.worn.domain.engine.DayTotals
import app.worn.domain.engine.InstantClock
import app.worn.domain.engine.SystemClock
import app.worn.domain.engine.TimelineSegment
import app.worn.domain.engine.TimerSnapshot
import app.worn.domain.engine.PeriodReport
import app.worn.domain.engine.ReportCalculator
import app.worn.domain.engine.ReportGrain
import app.worn.domain.engine.TreatmentPlanCalculator
import app.worn.domain.engine.TreatmentSchedule
import app.worn.domain.engine.WearCalculator
import app.worn.domain.model.ActivityType
import app.worn.domain.model.AlignerSet
import app.worn.domain.model.SessionKind
import app.worn.domain.model.TrackingSession
import app.worn.domain.model.UserSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.UUID

data class TodayUiState(
    val ready: Boolean = false,
    val onboardingComplete: Boolean = false,
    val settings: UserSettings? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val isToday: Boolean = true,
    val totals: DayTotals? = null,
    val segments: List<TimelineSegment> = emptyList(),
    val activities: List<ActivityType> = emptyList(),
    val wearing: Boolean = true,
    val openSession: TrackingSession? = null,
    val timer: TimerSnapshot? = null,
    val currentAligner: AlignerSet? = null,
    val streak: Int = 0,
    val nowMillis: Long = 0L,
    val historyDays: List<HistoryRow> = emptyList(),
    val thisWeek: List<HistoryRow> = emptyList(),
    val earlierDays: List<HistoryRow> = emptyList(),
    val weekHits: List<Boolean> = emptyList(),
    val sevenDayAverageMillis: Long = 0L,
    val alignerSets: List<AlignerSet> = emptyList(),
    val hasHistory: Boolean = false,
    val treatment: TreatmentSchedule? = null,
    val sessions: List<TrackingSession> = emptyList(),
    val report: PeriodReport? = null,
    val reportGrain: ReportGrain = ReportGrain.MONTH,
)

data class HistoryRow(
    val date: LocalDate,
    val totals: DayTotals,
)

class WornViewModel(
    private val repository: TrackingRepository,
    private val clock: InstantClock = SystemClock,
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val now = MutableStateFlow(clock.nowMillis())
    private val reportGrain = MutableStateFlow(ReportGrain.MONTH)
    private val reportAnchor = MutableStateFlow(LocalDate.now())
    private val _openTreatment = MutableStateFlow(false)
    val openTreatment: StateFlow<Boolean> = _openTreatment
    private val _startNewSet = MutableStateFlow(false)
    val startNewSet: StateFlow<Boolean> = _startNewSet

    val uiState: StateFlow<TodayUiState> = combine(
        combine(repository.snapshot, selectedDate, now) { a, b, c -> Triple(a, b, c) },
        reportGrain,
        reportAnchor,
    ) { triple, grain, anchor ->
        val (snapshot, date, nowMillis) = triple
        buildState(snapshot, date, nowMillis, grain, anchor)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    init {
        viewModelScope.launch {
            repository.initialize()
            while (true) {
                now.value = clock.nowMillis()
                delay(1_000)
            }
        }
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun shiftDay(delta: Int) {
        val zone = ZoneId.systemDefault()
        val next = selectedDate.value.plusDays(delta.toLong())
        if (next.isAfter(LocalDate.now(zone))) return
        selectedDate.value = next
    }

    fun goToday() {
        selectedDate.value = LocalDate.now(ZoneId.systemDefault())
    }

    fun completeOnboarding(targetHours: Int, tea: Int, lunch: Int, snack: Int, setNumber: Int?) {
        viewModelScope.launch {
            repository.completeOnboarding(
                targetMinutes = targetHours * 60,
                activityDurations = mapOf(
                    "activity_tea" to tea,
                    "activity_lunch" to lunch,
                    "activity_snack" to snack,
                ),
                firstAlignerSet = setNumber,
            )
        }
    }

    fun removeAligner(activityId: String, after: () -> Unit = {}) {
        viewModelScope.launch {
            repository.removeAligner(activityId)
            after()
        }
    }

    fun putBack(after: () -> Unit = {}) {
        viewModelScope.launch {
            repository.putAlignerBack()
            after()
        }
    }

    fun pause(after: () -> Unit = {}) {
        viewModelScope.launch {
            repository.pauseTimer()
            after()
        }
    }

    fun resume(after: () -> Unit = {}) {
        viewModelScope.launch {
            repository.resumeTimer()
            after()
        }
    }

    fun updateTarget(minutes: Int) {
        viewModelScope.launch { repository.updateTargetMinutes(minutes) }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch { repository.setNotificationsEnabled(enabled) }
    }

    fun saveActivity(activity: ActivityType) {
        viewModelScope.launch { repository.upsertActivity(activity) }
    }

    fun deleteActivity(id: String) {
        viewModelScope.launch { repository.deleteCustomActivity(id) }
    }

    fun startNewSet(number: Int, date: LocalDate, notes: String = "", onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            onResult(repository.recordAlignerSet(number, date, notes))
        }
    }

    fun updateReplacementInterval(days: Int) {
        viewModelScope.launch { repository.setReplacementIntervalDays(days) }
    }

    fun setReplacementReminders(enabled: Boolean) {
        viewModelScope.launch { repository.setReplacementRemindersEnabled(enabled) }
    }

    fun editSession(session: TrackingSession, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(repository.editSession(session)) }
    }

    fun addSession(kind: SessionKind, start: Long, end: Long?, activityId: String?, durationMin: Int?) {
        viewModelScope.launch {
            repository.addSession(
                TrackingSession(
                    id = UUID.randomUUID().toString(),
                    kind = kind,
                    startMillis = start,
                    endMillis = end,
                    activityTypeId = activityId,
                    configuredDurationMillis = durationMin?.times(60_000L),
                    pauseAccumulatedMillis = 0L,
                    pauseStartedMillis = null,
                ),
            )
        }
    }

    fun deleteSession(id: String) {
        viewModelScope.launch { repository.deleteSession(id) }
    }

    fun changeActivity(id: String) {
        viewModelScope.launch { repository.changeOpenActivity(id) }
    }

    fun setReportGrain(grain: ReportGrain) {
        reportGrain.value = grain
        reportAnchor.value = LocalDate.now()
    }

    fun shiftReport(delta: Long) {
        val next = ReportCalculator.shift(
            ReportCalculator.window(reportGrain.value, reportAnchor.value),
            delta,
        )
        val today = LocalDate.now()
        if (next.start.isAfter(today)) return
        reportAnchor.value = next.start
    }

    fun requestTreatmentTab() {
        _openTreatment.value = true
    }

    fun consumeTreatmentTab() {
        _openTreatment.value = false
    }

    fun requestStartNewSet() {
        _startNewSet.value = true
    }

    fun consumeStartNewSet() {
        _startNewSet.value = false
    }

    private fun buildState(
        snapshot: AppSnapshot,
        date: LocalDate,
        nowMillis: Long,
        grain: ReportGrain,
        anchor: LocalDate,
    ): TodayUiState {
        val zone = ZoneId.of(snapshot.settings.currentZoneId)
        val today = LocalDate.now(zone)
        val record = snapshot.recordFor(date, zone, snapshot.settings.dailyWearTargetMinutes)
        val totals = WearCalculator.totals(snapshot.sessions, record, nowMillis)
        val segments = WearCalculator.segmentsForDay(snapshot.sessions, record, nowMillis)
        val wearing = snapshot.openSession?.kind != SessionKind.REMOVAL
        val timer = snapshot.openSession
            ?.takeIf { it.kind == SessionKind.REMOVAL }
            ?.let { ActivityTimerCalculator.snapshot(it, nowMillis) }

        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        fun rowFor(d: LocalDate): HistoryRow {
            val rec = snapshot.recordFor(d, zone, snapshot.settings.dailyWearTargetMinutes)
            return HistoryRow(d, WearCalculator.totals(snapshot.sessions, rec, nowMillis))
        }
        val thisWeek = (0..6).map { rowFor(monday.plusDays(it.toLong())) }
        val firstSessionDate = snapshot.sessions.minOfOrNull {
            Instant.ofEpochMilli(it.startMillis).atZone(zone).toLocalDate()
        }
        val earlier = if (firstSessionDate != null && firstSessionDate.isBefore(monday)) {
            generateSequence(monday.minusDays(1)) { it.minusDays(1) }
                .takeWhile { !it.isBefore(firstSessionDate) }
                .map(::rowFor)
                .toList()
        } else {
            emptyList()
        }
        val last7 = (0..6).map { rowFor(today.minusDays(it.toLong())) }
        val history = thisWeek + earlier
        val treatment = TreatmentPlanCalculator.schedule(
            snapshot.alignerSets,
            snapshot.settings.replacementIntervalDays,
            today,
        )
        return TodayUiState(
            ready = true,
            onboardingComplete = snapshot.settings.onboardingComplete,
            settings = snapshot.settings,
            selectedDate = date,
            isToday = date == today,
            totals = totals,
            segments = segments,
            activities = snapshot.activities.filter { it.enabled },
            wearing = wearing,
            openSession = snapshot.openSession,
            timer = timer,
            currentAligner = treatment.current?.set ?: snapshot.currentAligner,
            streak = WearCalculator.streak(last7.map { it.totals }, today),
            nowMillis = nowMillis,
            historyDays = history,
            thisWeek = thisWeek,
            earlierDays = earlier,
            weekHits = thisWeek.map { it.totals.targetReached },
            sevenDayAverageMillis = WearCalculator.averageWornMillis(last7.map { it.totals }),
            alignerSets = snapshot.alignerSets,
            hasHistory = snapshot.sessions.isNotEmpty(),
            treatment = treatment,
            sessions = snapshot.sessions,
            report = ReportCalculator.report(
                grain = grain,
                anchor = anchor,
                today = today,
                nowMillis = nowMillis,
                sessions = snapshot.sessions,
                recordFor = { d -> snapshot.recordFor(d, zone, snapshot.settings.dailyWearTargetMinutes) },
                activities = snapshot.activities,
            ),
            reportGrain = grain,
        )
    }

    companion object {
        fun factory(repository: TrackingRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WornViewModel(repository) as T
                }
            }
    }
}
