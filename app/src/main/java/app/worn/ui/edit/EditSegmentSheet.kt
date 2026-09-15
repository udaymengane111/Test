package app.worn.ui.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.worn.domain.engine.TimelineSegment
import app.worn.domain.model.ActivityType
import app.worn.domain.model.SessionKind
import app.worn.ui.components.PastOrTodayDatePicker
import app.worn.ui.theme.WornTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSegmentSheet(
    segment: TimelineSegment,
    activities: List<ActivityType>,
    zone: ZoneId,
    onDismiss: () -> Unit,
    onSave: (app.worn.domain.model.TrackingSession) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = WornTheme.colors
    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    val startLocal = Instant.ofEpochMilli(segment.session.startMillis).atZone(zone)
    val endLocal = segment.session.endMillis?.let { Instant.ofEpochMilli(it).atZone(zone) }
    var startText by remember { mutableStateOf(startLocal.toLocalTime().format(fmt)) }
    var endText by remember { mutableStateOf(endLocal?.toLocalTime()?.format(fmt) ?: "") }
    var activityId by remember { mutableStateOf(segment.session.activityTypeId) }
    var selectedDate by remember { mutableStateOf(startLocal.toLocalDate()) }
    var showDate by remember { mutableStateOf(false) }
    val today = LocalDate.now(zone)
    val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy")

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.background) {
        Column(Modifier.padding(24.dp).padding(bottom = 24.dp)) {
            Text("Edit interval", color = colors.text, fontSize = 20.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                selectedDate.format(dateFmt),
                color = colors.secondary,
                fontSize = 14.sp,
                modifier = Modifier.clickable { showDate = true }.padding(vertical = 8.dp),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = startText,
                onValueChange = { startText = it },
                label = { Text("Start (HH:mm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = endText,
                onValueChange = { endText = it },
                label = { Text("End (HH:mm, empty = still open)") },
                modifier = Modifier.fillMaxWidth(),
            )
            if (segment.session.kind == SessionKind.REMOVAL) {
                Spacer(Modifier.height(12.dp))
                Text("Activity", color = colors.secondary, fontSize = 13.sp)
                activities.forEach { activity ->
                    TextButton(onClick = { activityId = activity.id }) {
                        Text(
                            activity.name,
                            color = if (activity.id == activityId) colors.accent else colors.text,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onDelete) { Text("Delete", color = colors.warning) }
                TextButton(onClick = {
                    val start = parseTime(startText, selectedDate, zone) ?: return@TextButton
                    val end = endText.takeIf { it.isNotBlank() }?.let { parseTime(it, selectedDate, zone) }
                    onSave(
                        segment.session.copy(
                            startMillis = start,
                            endMillis = end,
                            activityTypeId = activityId,
                        ),
                    )
                }) { Text("Save") }
            }
        }
    }
    if (showDate) {
        PastOrTodayDatePicker(
            selected = selectedDate,
            today = today,
            onDismiss = { showDate = false },
            onConfirm = {
                selectedDate = it
                showDate = false
            },
        )
    }
}

private fun parseTime(text: String, date: java.time.LocalDate, zone: ZoneId): Long? {
    return runCatching {
        val time = LocalTime.parse(text, DateTimeFormatter.ofPattern("H:mm"))
        date.atTime(time).atZone(zone).toInstant().toEpochMilli()
    }.getOrNull()
}
