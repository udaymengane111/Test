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
import java.time.LocalDate
import java.time.ZoneId
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
    val weekHits: List<Boolean> = emptyList(),
    val sevenDayAverageMillis: Long = 0L,
    val alignerSets: List<AlignerSet> = emptyList(),
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

    val uiState: StateFlow<TodayUiState> = combine(
        repository.snapshot,
        selectedDate,
        now,
    ) { snapshot, date, nowMillis ->
        buildState(snapshot, date, nowMillis)
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
        selectedDate.value = selectedDate.value.plusDays(delta.toLong())
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

    fun startNewSet(number: Int, date: LocalDate, notes: String) {
        viewModelScope.launch { repository.startNewAlignerSet(number, date, notes) }
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

    private fun buildState(snapshot: AppSnapshot, date: LocalDate, nowMillis: Long): TodayUiState {
        val zone = ZoneId.of(snapshot.settings.currentZoneId)
        val today = LocalDate.now(zone)
        val record = snapshot.recordFor(date, zone, snapshot.settings.dailyWearTargetMinutes)
        val totals = WearCalculator.totals(snapshot.sessions, record, nowMillis)
        val segments = WearCalculator.segmentsForDay(snapshot.sessions, record, nowMillis)
        val wearing = snapshot.openSession?.kind != SessionKind.REMOVAL
        val timer = snapshot.openSession
            ?.takeIf { it.kind == SessionKind.REMOVAL }
            ?.let { ActivityTimerCalculator.snapshot(it, nowMillis) }

        val historyStart = today.minusDays(21)
        val history = generateSequence(today) { it.minusDays(1) }
            .takeWhile { it >= historyStart }
            .map { d ->
                val rec = snapshot.recordFor(d, zone, snapshot.settings.dailyWearTargetMinutes)
                HistoryRow(d, WearCalculator.totals(snapshot.sessions, rec, nowMillis))
            }
            .toList()

        val week = (6 downTo 0).map { offset ->
            val d = today.minusDays(offset.toLong())
            val rec = snapshot.recordFor(d, zone, snapshot.settings.dailyWearTargetMinutes)
            WearCalculator.totals(snapshot.sessions, rec, nowMillis).targetReached
        }
        val last7 = history.take(7)
        val streakDays = history.map { it.totals }
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
            currentAligner = snapshot.currentAligner,
            streak = WearCalculator.streak(streakDays, today),
            nowMillis = nowMillis,
            historyDays = history,
            weekHits = week,
            sevenDayAverageMillis = WearCalculator.averageWornMillis(last7.map { it.totals }),
            alignerSets = snapshot.alignerSets,
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
