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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.worn.domain.engine.DurationFormat
import app.worn.domain.engine.TimelineSegment
import app.worn.service.RemovalTimerService
import app.worn.ui.TodayUiState
import app.worn.ui.WornViewModel
import app.worn.ui.components.DayMetrics
import app.worn.ui.components.Hairline
import app.worn.ui.components.PrimaryButton
import app.worn.ui.components.SectionLabel
import app.worn.ui.components.TextAction
import app.worn.ui.components.TimelineList
import app.worn.ui.components.WearRing
import app.worn.ui.components.WearingStatus
import app.worn.ui.components.activityIcon
import app.worn.ui.edit.AddIntervalSheet
import app.worn.ui.edit.EditSegmentSheet
import app.worn.ui.theme.WornTheme
import java.time.LocalDate
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
    val headerDate = state.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    val today = LocalDate.now(zone)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (state.isToday) "Today" else state.selectedDate.format(DateTimeFormatter.ofPattern("EEE d MMM")),
                    color = colors.text,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.clickable { vm.goToday() },
                )
                Text(headerDate, color = colors.secondary, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                state.currentAligner?.let { set ->
                    Text("Aligner ${set.setNumber}", color = colors.secondary, fontSize = 14.sp)
                    Text(
                        set.startDate.format(DateTimeFormatter.ofPattern("d MMM")),
                        color = colors.tertiary,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.ChevronLeft,
                contentDescription = "Previous day",
                tint = colors.tertiary,
                modifier = Modifier
                    .size(44.dp)
                    .semantics { role = Role.Button }
                    .clickable { vm.shiftDay(-1) }
                    .padding(10.dp),
            )
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = "Next day",
                tint = if (state.selectedDate < today) colors.tertiary else colors.ringTrack,
                modifier = Modifier
                    .size(44.dp)
                    .semantics { role = Role.Button }
                    .clickable(enabled = state.selectedDate < today) { vm.shiftDay(1) }
                    .padding(10.dp),
            )
        }

        WearRing(
            wornMillis = totals.wornMillis,
            targetMillis = totals.targetMillis,
            wearing = state.wearing,
            targetReached = totals.targetReached,
        )
        Text(
            if (totals.targetReached) DurationFormat.hoursMinutes(totals.targetMillis) + " target"
            else DurationFormat.percent(totals.progress.coerceAtMost(1f)),
            color = colors.tertiary,
            fontSize = 13.sp,
        )
        if (!totals.targetReached) {
            Spacer(Modifier.height(4.dp))
            Text(
                DurationFormat.hoursMinutes(totals.targetMillis) + " target",
                color = colors.secondary,
                fontSize = 15.sp,
            )
        }

        Spacer(Modifier.height(28.dp))
        Hairline()
        Spacer(Modifier.height(22.dp))
        DayMetrics(totals.wornMillis, totals.remainingMillis, totals.notWornMillis, totals.targetReached)

        Spacer(Modifier.height(32.dp))
        AnimatedContent(
            targetState = Triple(state.wearing, state.timer?.paused, state.timer?.overdue),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "status",
        ) { (wearing, paused, overdue) ->
            val timer = state.timer
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                WearingStatus(wearing)
                if (!wearing && timer != null) {
                    val activityName = state.activities.firstOrNull { it.id == state.openSession?.activityTypeId }?.name ?: "Activity"
                    Spacer(Modifier.height(20.dp))
                    Text(activityName, color = colors.text, fontSize = 18.sp)
                    Spacer(Modifier.height(6.dp))
                    when {
                        paused == true -> {
                            Text("Paused", color = colors.tertiary, fontSize = 13.sp, letterSpacing = 1.2.sp)
                            Text(
                                DurationFormat.timer(timer.remainingMillis.coerceAtLeast(0)),
                                color = colors.text,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Light,
                            )
                            Text("remaining", color = colors.secondary, fontSize = 15.sp)
                        }
                        overdue == true -> {
                            Text("Time’s up", color = colors.warning, fontSize = 13.sp, letterSpacing = 1.2.sp)
                            Text(
                                DurationFormat.timer(timer.overdueMillis),
                                color = colors.text,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Light,
                            )
                            Text("over", color = colors.secondary, fontSize = 15.sp)
                        }
                        else -> {
                            Text(
                                DurationFormat.timer(timer.remainingMillis),
                                color = colors.text,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Light,
                            )
                            Text("remaining", color = colors.secondary, fontSize = 15.sp)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                if (state.isToday) {
                    if (wearing) {
                        PrimaryButton("REMOVE ALIGNER", onClick = {
                            if (Build.VERSION.SDK_INT >= 33 && state.settings?.notificationsEnabled != true) {
                                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            showActivities = true
                        })
                    } else {
                        PrimaryButton("PUT ALIGNER BACK", onClick = {
                            vm.putBack { RemovalTimerService.sync(context) }
                        })
                        TextAction(if (paused == true) "Resume" else "Pause") {
                            if (paused == true) vm.resume { RemovalTimerService.sync(context) }
                            else vm.pause { RemovalTimerService.sync(context) }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(36.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SectionLabel("Timeline")
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Add",
                    color = colors.secondary,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 44.dp)
                        .clickable { adding = true }
                        .padding(top = 12.dp),
                )
            }
        }
        if (state.segments.isEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Intervals you wear and remove will appear here.", color = colors.tertiary, fontSize = 14.sp)
        } else {
            Spacer(Modifier.height(16.dp))
            TimelineList(state.segments, state.activities, zone, state.nowMillis) { editing = it }
        }
        Spacer(Modifier.height(36.dp))
    }

    if (showActivities) {
        ModalBottomSheet(
            onDismissRequest = { showActivities = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.background,
        ) {
            Column(Modifier.padding(horizontal = 28.dp).padding(bottom = 40.dp)) {
                Text("Why are you taking it out?", color = colors.text, fontSize = 22.sp, fontWeight = FontWeight.Light)
                Spacer(Modifier.height(8.dp))
                state.activities.forEach { activity ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .clickable {
                                vm.removeAligner(activity.id) { RemovalTimerService.sync(context) }
                                showActivities = false
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            activityIcon(activity.iconKey, activity.id),
                            contentDescription = null,
                            tint = colors.secondary,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            activity.name,
                            color = colors.text,
                            fontSize = 17.sp,
                            modifier = Modifier.padding(start = 16.dp).weight(1f),
                        )
                        Text("${activity.defaultDurationMinutes} min", color = colors.tertiary, fontSize = 14.sp)
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
            onSave = { session -> vm.editSession(session) { editing = null } },
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
