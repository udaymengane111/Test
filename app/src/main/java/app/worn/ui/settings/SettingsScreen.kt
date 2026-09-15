package app.worn.ui.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.worn.domain.model.ActivityType
import app.worn.ui.TodayUiState
import app.worn.ui.WornViewModel
import app.worn.ui.theme.WornTheme
import java.util.UUID

@Composable
fun SettingsScreen(state: TodayUiState, vm: WornViewModel, onBack: () -> Unit) {
    val colors = WornTheme.colors
    val settings = state.settings ?: return
    var hours by remember(settings.dailyWearTargetMinutes) {
        mutableStateOf((settings.dailyWearTargetMinutes / 60).toString())
    }
    var minutes by remember(settings.dailyWearTargetMinutes) {
        mutableStateOf((settings.dailyWearTargetMinutes % 60).toString())
    }
    var adding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newMinutes by remember { mutableStateOf("15") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.text)
            }
            Text("Settings", color = colors.text, fontSize = 28.sp, fontWeight = FontWeight.Light)
        }
        Spacer(Modifier.height(24.dp))
        Text("DAILY WEAR TARGET", color = colors.tertiary, fontSize = 11.sp, letterSpacing = 1.8.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "${settings.dailyWearTargetMinutes / 60}h ${settings.dailyWearTargetMinutes % 60}m",
            color = colors.text,
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
        )
        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedTextField(hours, { hours = it.filter(Char::isDigit).take(2) }, label = { Text("Hours") }, modifier = Modifier.weight(1f).padding(end = 8.dp))
            OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(2) }, label = { Text("Minutes") }, modifier = Modifier.weight(1f))
        }
        TextButton(onClick = {
            val h = hours.toIntOrNull() ?: return@TextButton
            val m = (minutes.toIntOrNull() ?: 0).coerceIn(0, 59)
            vm.updateTarget(h * 60 + m)
        }) { Text("Save target") }
        Text("Changing the target applies from today onward. Past days keep their original target.", color = colors.tertiary, fontSize = 13.sp)

        Spacer(Modifier.height(32.dp))
        Text("REMINDERS", color = colors.tertiary, fontSize = 11.sp, letterSpacing = 1.8.sp)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Notifications", color = colors.text, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Switch(checked = settings.notificationsEnabled, onCheckedChange = vm::setNotifications)
        }

        Spacer(Modifier.height(28.dp))
        Text("REMOVAL ACTIVITIES", color = colors.tertiary, fontSize = 11.sp, letterSpacing = 1.8.sp)
        Spacer(Modifier.height(8.dp))
        state.activities.forEach { activity ->
            ActivityRow(activity, vm)
        }
        Spacer(Modifier.height(12.dp))
        if (!adding) {
            Text(
                "Add activity",
                color = colors.accent,
                modifier = Modifier.clickable { adding = true }.padding(vertical = 12.dp),
            )
        } else {
            OutlinedTextField(newName, { newName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(newMinutes, { newMinutes = it.filter(Char::isDigit).take(3) }, label = { Text("Minutes") }, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = {
                val mins = newMinutes.toIntOrNull() ?: return@TextButton
                if (newName.isBlank()) return@TextButton
                vm.saveActivity(
                    ActivityType(
                        id = UUID.randomUUID().toString(),
                        name = newName.trim(),
                        defaultDurationMinutes = mins,
                        iconKey = "more",
                        enabled = true,
                        isDefault = false,
                        sortOrder = (state.activities.maxOfOrNull { it.sortOrder } ?: 0) + 1,
                    ),
                )
                newName = ""
                newMinutes = "15"
                adding = false
            }) { Text("Save activity") }
        }
        Spacer(Modifier.height(40.dp))
        Text("Worn stores everything on this device. No account, no ads, no analytics.", color = colors.tertiary, fontSize = 13.sp)
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun ActivityRow(activity: ActivityType, vm: WornViewModel) {
    val colors = WornTheme.colors
    var minutes by remember(activity.id, activity.defaultDurationMinutes) {
        mutableStateOf(activity.defaultDurationMinutes.toString())
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(activity.name, color = colors.text, fontSize = 16.sp, modifier = Modifier.weight(1f))
            if (!activity.isDefault) {
                TextButton(onClick = { vm.deleteActivity(activity.id) }) {
                    Text("Delete", color = colors.warning)
                }
            }
        }
        OutlinedTextField(
            value = minutes,
            onValueChange = { minutes = it.filter(Char::isDigit).take(3) },
            label = { Text("Duration (min)") },
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = {
            minutes.toIntOrNull()?.let { value ->
                vm.saveActivity(activity.copy(defaultDurationMinutes = value.coerceAtLeast(1)))
            }
        }) { Text("Save") }
    }
}
