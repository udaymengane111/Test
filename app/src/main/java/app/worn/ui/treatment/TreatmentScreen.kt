package app.worn.ui.treatment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import app.worn.notifications.ReplacementReminders
import app.worn.ui.TodayUiState
import app.worn.ui.WornViewModel
import app.worn.ui.components.PastOrTodayDatePicker
import app.worn.ui.components.PrimaryButton
import app.worn.ui.components.SectionLabel
import app.worn.ui.theme.WornTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TreatmentScreen(
    state: TodayUiState,
    vm: WornViewModel,
    onSettings: () -> Unit,
    startNewRequested: Boolean = false,
    onStartNewConsumed: () -> Unit = {},
) {
    val colors = WornTheme.colors
    val context = LocalContext.current
    val today = LocalDate.now()
    val schedule = state.treatment
    val current = schedule?.current
    val nextNumber = schedule?.expectedNextSetNumber ?: 1
    var showNew by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(startNewRequested) {
        if (startNewRequested) {
            showNew = true
            onStartNewConsumed()
        }
    }
    var number by remember(showNew, nextNumber, current == null) {
        mutableStateOf((if (current == null) 1 else nextNumber).toString())
    }
    var startDate by remember(showNew) { mutableStateOf(today) }
    var showPicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val fmt = DateTimeFormatter.ofPattern("d MMM yyyy")
    val shortFmt = DateTimeFormatter.ofPattern("d MMM")

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Treatment", color = colors.text, fontSize = 32.sp, fontWeight = FontWeight.Light, modifier = Modifier.weight(1f))
            IconButton(onClick = onSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = colors.secondary)
            }
        }
        Spacer(Modifier.height(36.dp))
        SectionLabel("Current aligner")
        Spacer(Modifier.height(12.dp))
        if (current != null) {
            Text("Set ${current.set.setNumber}", color = colors.text, fontSize = 44.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(8.dp))
            Text("Started", color = colors.tertiary, fontSize = 13.sp)
            Text(current.start.format(fmt), color = colors.secondary, fontSize = 16.sp)
            Spacer(Modifier.height(16.dp))
            if (schedule?.isOverdue == true) {
                Text("OVERDUE", color = colors.warning, fontSize = 11.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Text("Set ${schedule.expectedNextSetNumber}", color = colors.text, fontSize = 22.sp, fontWeight = FontWeight.Light)
                Text(
                    "Due ${schedule.expectedNextDate?.format(shortFmt)}",
                    color = colors.secondary,
                    fontSize = 15.sp,
                )
                Text(
                    "${schedule.overdueDays} day${if (schedule.overdueDays == 1L) "" else "s"} overdue",
                    color = colors.warning,
                    fontSize = 14.sp,
                )
            } else {
                Text("Next replacement", color = colors.tertiary, fontSize = 13.sp)
                Text(
                    schedule?.expectedNextDate?.format(fmt) ?: "—",
                    color = colors.secondary,
                    fontSize = 16.sp,
                )
            }
        } else {
            Text("No aligner recorded", color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(8.dp))
            Text("Add the set you’re wearing now. You can use the real start date, even if it was in the past.", color = colors.secondary, fontSize = 15.sp)
        }
        Spacer(Modifier.height(32.dp))
        if (!showNew) {
            PrimaryButton(if (current == null) "ADD CURRENT ALIGNER" else "START NEW ALIGNER") {
                error = null
                showNew = true
            }
        } else {
            Text(if (current == null) "Add current aligner" else "Start new aligner", color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(20.dp))
            Text("Set number", color = colors.tertiary, fontSize = 13.sp)
            BasicTextField(
                value = number,
                onValueChange = { number = it.filter(Char::isDigit).take(3) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = colors.text, fontSize = 32.sp, fontWeight = FontWeight.Light),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
            Text("Start date", color = colors.tertiary, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                startDate.format(fmt),
                color = colors.text,
                fontSize = 17.sp,
                modifier = Modifier
                    .defaultMinSize(minHeight = 44.dp)
                    .clickable { showPicker = true }
                    .padding(vertical = 10.dp),
            )
            Text("Change date", color = colors.secondary, fontSize = 13.sp, modifier = Modifier.clickable { showPicker = true })
            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = colors.warning, fontSize = 14.sp)
            }
            Spacer(Modifier.height(24.dp))
            PrimaryButton("SAVE") {
                val n = number.toIntOrNull() ?: return@PrimaryButton
                vm.startNewSet(n, startDate, "") { message ->
                    if (message == null) {
                        ReplacementReminders.clearNotice(context)
                        ReplacementReminders.sync(context)
                        showNew = false
                        error = null
                    } else {
                        error = message
                    }
                }
            }
            TextButton(onClick = { showNew = false; error = null }) { Text("Cancel", color = colors.secondary) }
        }
        val history = schedule?.periods.orEmpty().sortedByDescending { it.set.setNumber }
        if (history.isNotEmpty()) {
            Spacer(Modifier.height(40.dp))
            SectionLabel("Aligner history")
            Spacer(Modifier.height(12.dp))
            history.forEach { period ->
                val range = if (period.endInclusive == null) {
                    period.start.format(shortFmt) + " → Current"
                } else {
                    period.start.format(shortFmt) + " → " + period.endInclusive.format(shortFmt)
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                ) {
                    Text("Set ${period.set.setNumber}", color = colors.text, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Text(range, color = colors.secondary, fontSize = 15.sp)
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
    if (showPicker) {
        PastOrTodayDatePicker(
            selected = startDate,
            today = today,
            onDismiss = { showPicker = false },
            onConfirm = {
                startDate = it
                showPicker = false
            },
        )
    }
}
