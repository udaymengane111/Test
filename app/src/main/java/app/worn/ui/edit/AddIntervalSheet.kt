package app.worn.ui.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.worn.domain.model.ActivityType
import app.worn.domain.model.DefaultActivities
import app.worn.domain.model.SessionKind
import app.worn.ui.components.PastOrTodayDatePicker
import app.worn.ui.theme.WornTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIntervalSheet(
    date: LocalDate,
    zone: ZoneId,
    activities: List<ActivityType>,
    onDismiss: () -> Unit,
    onAdd: (SessionKind, Long, Long, String?) -> Unit,
) {
    val colors = WornTheme.colors
    val today = LocalDate.now(zone)
    var selectedDate by remember { mutableStateOf(date) }
    var showDate by remember { mutableStateOf(false) }
    var kindWear by remember { mutableStateOf(true) }
    var startText by remember { mutableStateOf("12:00") }
    var endText by remember { mutableStateOf("12:30") }
    var activityId by remember { mutableStateOf(DefaultActivities.OTHER) }
    val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy")

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.background) {
        Column(Modifier.padding(24.dp).padding(bottom = 24.dp)) {
            Text("Add missing interval", color = colors.text, fontSize = 20.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                selectedDate.format(dateFmt),
                color = colors.secondary,
                fontSize = 14.sp,
                modifier = Modifier
                    .defaultMinSize(minHeight = 44.dp)
                    .clickable { showDate = true }
                    .padding(vertical = 8.dp),
            )
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { kindWear = true }) {
                Text("Worn", color = if (kindWear) colors.accent else colors.secondary)
            }
            TextButton(onClick = { kindWear = false }) {
                Text("Removed", color = if (!kindWear) colors.accent else colors.secondary)
            }
            OutlinedTextField(startText, { startText = it }, label = { Text("Start HH:mm") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(endText, { endText = it }, label = { Text("End HH:mm") }, modifier = Modifier.fillMaxWidth())
            if (!kindWear) {
                activities.forEach { activity ->
                    TextButton(onClick = { activityId = activity.id }) {
                        Text(activity.name, color = if (activity.id == activityId) colors.accent else colors.text)
                    }
                }
            }
            TextButton(onClick = {
                val start = parse(startText, selectedDate, zone) ?: return@TextButton
                val end = parse(endText, selectedDate, zone) ?: return@TextButton
                if (end <= start) return@TextButton
                onAdd(if (kindWear) SessionKind.WEAR else SessionKind.REMOVAL, start, end, if (kindWear) null else activityId)
            }) { Text("Add") }
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

private fun parse(text: String, date: LocalDate, zone: ZoneId): Long? = runCatching {
    val time = LocalTime.parse(text, DateTimeFormatter.ofPattern("H:mm"))
    date.atTime(time).atZone(zone).toInstant().toEpochMilli()
}.getOrNull()
