package app.worn.ui.today

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.worn.domain.engine.DurationFormat
import app.worn.domain.engine.TimelineSegment
import app.worn.domain.model.SessionKind
import app.worn.notifications.RemovalAlerts
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
    val muted by RemovalAlerts.muted.collectAsStateWithLifecycle()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setNotifications(granted)
        showActivities = true
    }
    val zone = ZoneId.of(state.settings?.currentZoneId ?: ZoneId.systemDefault().id)
    val headerDate = state.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    val today = LocalDate.now(zone)
    val treatment = state.treatment
    val overdue = treatment?.isOverdue == true
    val dueToday = treatment?.dueToday == true

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (state.isToday) "Today" else state.selectedDate.format(DateTimeFormatter.ofPattern("EEE d MMM")),
                    color = colors.text,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clickable { vm.goToday() }
                        .padding(vertical = 8.dp),
                )
                Text(headerDate, color = colors.secondary, fontSize = 14.sp)
            }
            Row {
                Icon(
                    Icons.Outlined.ChevronLeft,
                    contentDescription = "Previous day",
                    tint = colors.secondary,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { role = Role.Button }
                        .clickable { vm.shiftDay(-1) }
                        .padding(12.dp),
                )
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = "Next day",
                    tint = if (state.selectedDate < today) colors.secondary else colors.ringTrack,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { role = Role.Button }
                        .clickable(enabled = state.selectedDate < today) { vm.shiftDay(1) }
                        .padding(12.dp),
                )
            }
        }

        state.currentAligner?.let { set ->
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = if (overdue) {
                            "Aligner ${set.setNumber} replacement overdue ${treatment?.overdueDays} days"
                        } else if (dueToday) {
                            "Aligner ${set.setNumber} next set due today"
                        } else {
                            "Aligner ${set.setNumber}"
                        }
                    },
            ) {
                Text(
                    "ALIGNER ${set.setNumber}",
                    color = if (overdue || dueToday) colors.warning else colors.secondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.4.sp,
                )
                Spacer(Modifier.height(4.dp))
                when {
                    overdue -> {
                        Text("REPLACEMENT OVERDUE", color = colors.warning, fontSize = 16.sp)
                        Text(
                            "${treatment?.overdueDays} day${if (treatment?.overdueDays == 1L) "" else "s"}",
                            color = colors.warning,
                            fontSize = 14.sp,
                        )
                        if (state.isToday) {
                            Spacer(Modifier.height(12.dp))
                            PrimaryButton("START NEW ALIGNER") {
                                vm.requestTreatmentTab()
                                vm.requestStartNewSet()
                            }
                        }
                    }
                    dueToday -> Text("NEXT SET DUE TODAY", color = colors.warning, fontSize = 15.sp)
                    else -> {
                        val days = treatment?.daysUntilReplacement
                        Text(
                            when {
                                days == null -> "Next set"
                                days == 1L -> "Next set in 1 day"
                                else -> "Next set in $days days"
                            },
                            color = colors.secondary,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        WearRing(
            wornMillis = totals.wornMillis,
            targetMillis = totals.targetMillis,
            wearing = state.wearing,
            targetReached = totals.targetReached,
        )
        Text(
            if (totals.targetReached) DurationFormat.hoursMinutes(totals.targetMillis) + " target"
            else DurationFormat.percent(totals.progress.coerceAtMost(1f)),
            color = colors.secondary,
            fontSize = 14.sp,
        )
        if (!totals.targetReached) {
            Spacer(Modifier.height(2.dp))
            Text(
                DurationFormat.hoursMinutes(totals.targetMillis) + " target",
                color = colors.secondary,
                fontSize = 15.sp,
            )
        }

        Spacer(Modifier.height(24.dp))
        Hairline()
        Spacer(Modifier.height(20.dp))
        DayMetrics(totals.wornMillis, totals.remainingMillis, totals.notWornMillis, totals.targetReached)

        Spacer(Modifier.height(28.dp))
        AnimatedContent(
            targetState = Triple(state.wearing, state.timer?.paused, state.timer?.overdue),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "status",
        ) { (wearing, paused, overdueTimer) ->
            val timer = state.timer
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                WearingStatus(wearing)
                if (!wearing && timer != null) {
                    val activityName = state.activities.firstOrNull { it.id == state.openSession?.activityTypeId }?.name ?: "Activity"
                    Spacer(Modifier.height(16.dp))
                    Text(activityName, color = colors.text, fontSize = 18.sp)
                    Spacer(Modifier.height(6.dp))
                    when {
                        paused == true -> {
                            Text("Paused", color = colors.secondary, fontSize = 13.sp, letterSpacing = 1.2.sp)
                            Text(
                                DurationFormat.timer(timer.remainingMillis.coerceAtLeast(0)),
                                color = colors.text,
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Light,
                            )
                            Text("remaining", color = colors.secondary, fontSize = 15.sp)
                        }
                        overdueTimer == true -> {
                            Text("TIME'S UP", color = colors.warning, fontSize = 13.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Medium)
                            Text(
                                DurationFormat.timer(timer.overdueMillis),
                                color = colors.warning,
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Light,
                            )
                            Text("over — put your aligner back", color = colors.secondary, fontSize = 15.sp)
                        }
                        else -> {
                            Text(
                                DurationFormat.timer(timer.remainingMillis),
                                color = colors.text,
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Light,
                            )
                            Text("remaining", color = colors.secondary, fontSize = 15.sp)
                            Text(
                                "${DurationFormat.span(timer.elapsedMillis)} elapsed",
                                color = colors.tertiary,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                if (state.isToday) {
                    if (wearing) {
                        PrimaryButton("REMOVE ALIGNER", onClick = {
                            val needPermission = Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                                PackageManager.PERMISSION_GRANTED
                            if (needPermission) {
                                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                vm.setNotifications(true)
                                showActivities = true
                            }
                        })
                    } else {
                        PrimaryButton("PUT ALIGNER BACK", onClick = {
                            vm.putBack {
                                RemovalAlerts.clear(context)
                                RemovalTimerService.sync(context)
                            }
                        })
                        TextAction(if (paused == true) "Resume" else "Pause") {
                            if (paused == true) vm.resume { RemovalTimerService.sync(context) }
                            else vm.pause { RemovalTimerService.sync(context) }
                        }
                        if (overdueTimer == true) {
                            Text(
                                if (muted) "🔇 Sound reminders muted" else "🔊 Sound reminders on",
                                color = colors.secondary,
                                fontSize = 15.sp,
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clickable { RemovalAlerts.setMuted(context, !muted) }
                                    .padding(vertical = 12.dp)
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = if (muted) "Unmute sound reminders" else "Mute sound reminders"
                                    },
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Timeline")
            Text(
                "Add",
                color = colors.secondary,
                fontSize = 15.sp,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .clickable { adding = true }
                    .padding(vertical = 12.dp),
            )
        }
        if (state.segments.isNotEmpty()) {
            val wornCount = state.segments.count { it.session.kind == SessionKind.WEAR }
            val outCount = state.segments.count { it.session.kind == SessionKind.REMOVAL }
            Text(
                "Worn ${DurationFormat.span(totals.wornMillis)}  ·  Out ${DurationFormat.span(totals.notWornMillis)}  ·  $wornCount wear, $outCount out",
                color = colors.tertiary,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.segments.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Intervals you wear and remove will appear here.", color = colors.secondary, fontSize = 14.sp)
        } else {
            Spacer(Modifier.height(8.dp))
            TimelineList(state.segments, state.activities, zone, state.nowMillis, state.timer) { editing = it }
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
                        Text("${activity.defaultDurationMinutes} min", color = colors.secondary, fontSize = 14.sp)
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
                val minutes = state.activities.firstOrNull { it.id == activityId }?.defaultDurationMinutes
                vm.addSession(kind, start, end, activityId, minutes)
                adding = false
            },
        )
    }
}
