package app.worn.ui.today

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.worn.domain.engine.DurationFormat
import app.worn.domain.engine.TimelineSegment
import app.worn.service.RemovalTimerService
import app.worn.ui.TodayUiState
import app.worn.ui.WornViewModel
import app.worn.ui.components.DayMetrics
import app.worn.ui.components.PrimaryButton
import app.worn.ui.components.StatusDot
import app.worn.ui.components.TimelineList
import app.worn.ui.components.WearRing
import app.worn.ui.edit.AddIntervalSheet
import app.worn.ui.edit.EditSegmentSheet
import app.worn.ui.theme.WornTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(state: TodayUiState, vm: WornViewModel) {
    val colors = WornTheme.colors
    val context = LocalContext.current
    val totals = state.totals ?: return
    var showActivities by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TimelineSegment?>(null) }
    var adding by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setNotifications(granted)
    }
    val zone = ZoneId.of(state.settings?.currentZoneId ?: ZoneId.systemDefault().id)
    val dateLabel = if (state.isToday) "Today" else state.selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
    val headerDate = state.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.shiftDay(-1) }) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous day", tint = colors.secondary)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    dateLabel.uppercase(),
                    color = colors.text,
                    fontSize = 13.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { vm.goToday() },
                )
                Text(headerDate, color = colors.secondary, fontSize = 13.sp)
            }
            IconButton(onClick = { vm.shiftDay(1) }) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = "Next day", tint = colors.secondary)
            }
        }

        state.currentAligner?.let { set ->
            Spacer(Modifier.height(8.dp))
            Text(
                "Aligner ${set.setNumber}",
                color = colors.tertiary,
                fontSize = 13.sp,
            )
            Text(
                "Started ${set.startDate.format(DateTimeFormatter.ofPattern("d MMM"))}",
                color = colors.tertiary,
                fontSize = 12.sp,
            )
        }

        Spacer(Modifier.height(20.dp))
        WearRing(
            wornMillis = totals.wornMillis,
            targetMillis = totals.targetMillis,
            wearing = state.wearing,
            targetReached = totals.targetReached,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            DurationFormat.hoursMinutes(totals.targetMillis) + " target",
            color = colors.secondary,
            fontSize = 15.sp,
        )
        if (totals.targetReached && state.isToday) {
            Spacer(Modifier.height(6.dp))
            Text("That’s the day.", color = colors.accent, fontSize = 14.sp)
        }
        Spacer(Modifier.height(28.dp))
        DayMetrics(totals.wornMillis, totals.remainingMillis, totals.notWornMillis, totals.targetReached)
        if (state.streak >= 2 && state.isToday) {
            Spacer(Modifier.height(14.dp))
            Text("${state.streak} days on target", color = colors.tertiary, fontSize = 13.sp)
        }

        Spacer(Modifier.height(32.dp))
        AnimatedContent(
            targetState = state.wearing to state.timer,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "status",
        ) { (wearing, timer) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (wearing) {
                    StatusDot(true, "Wearing aligner")
                    Spacer(Modifier.height(20.dp))
                    if (state.isToday) {
                        PrimaryButton("REMOVE ALIGNER", onClick = {
                            if (Build.VERSION.SDK_INT >= 33 && state.settings?.notificationsEnabled != true) {
                                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            showActivities = true
                        })
                    }
                } else {
                    StatusDot(false, "Aligner removed")
                    val activityName = state.activities.firstOrNull { it.id == state.openSession?.activityTypeId }?.name ?: "Activity"
                    Spacer(Modifier.height(12.dp))
                    if (timer != null) {
                        Text(activityName, color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        val timeText = when {
                            timer.paused -> "${DurationFormat.timer(timer.remainingMillis.coerceAtLeast(0))} paused"
                            timer.overdue -> "${DurationFormat.timer(timer.overdueMillis)} over"
                            else -> "${DurationFormat.timer(timer.remainingMillis)} remaining"
                        }
                        Text(
                            timeText,
                            color = if (timer.overdue && !timer.paused) colors.warning else colors.secondary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Light,
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    if (state.isToday) {
                        PrimaryButton("PUT ALIGNER BACK", onClick = {
                            vm.putBack { RemovalTimerService.sync(context) }
                        })
                        Spacer(Modifier.height(10.dp))
                        PrimaryButton(
                            text = if (timer?.paused == true) "RESUME" else "PAUSE",
                            onClick = {
                                if (timer?.paused == true) {
                                    vm.resume { RemovalTimerService.sync(context) }
                                } else {
                                    vm.pause { RemovalTimerService.sync(context) }
                                }
                            },
                            subtle = true,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(40.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("TIMELINE", color = colors.tertiary, fontSize = 11.sp, letterSpacing = 1.8.sp)
            Text("Add", color = colors.secondary, fontSize = 13.sp, modifier = Modifier.clickable { adding = true })
        }
        if (state.segments.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            TimelineList(state.segments, state.activities, zone, state.nowMillis) { editing = it }
        }
        Spacer(Modifier.height(32.dp))
    }

    if (showActivities) {
        ModalBottomSheet(
            onDismissRequest = { showActivities = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.background,
        ) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp).padding(bottom = 36.dp)) {
                Text("Why are you taking it out?", color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(20.dp))
                state.activities.forEach { activity ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                vm.removeAligner(activity.id) { RemovalTimerService.sync(context) }
                                showActivities = false
                            }
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(activity.name, color = colors.text, fontSize = 17.sp)
                        Text("${activity.defaultDurationMinutes} min", color = colors.secondary, fontSize = 15.sp)
                    }
                }
            }
        }
    }
    editing?.let { segment ->
        EditSegmentSheet(
            segment = segment,
            activities = state.activities,
            zone = zone,
            onDismiss = { editing = null },
            onSave = { session ->
                vm.editSession(session) { editing = null }
            },
            onDelete = {
                vm.deleteSession(segment.session.id)
                editing = null
            },
        )
    }
    if (adding) {
        AddIntervalSheet(
            date = state.selectedDate,
            zone = zone,
            activities = state.activities,
            onDismiss = { adding = false },
            onAdd = { kind, start, end, activityId ->
                vm.addSession(kind, start, end, activityId, 15)
                adding = false
            },
        )
    }
}
