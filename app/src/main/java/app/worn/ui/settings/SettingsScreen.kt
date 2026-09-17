package app.worn.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import android.Manifest
import android.os.Build
import app.worn.domain.model.ActivityType
import app.worn.notifications.ReplacementReminders
import app.worn.notifications.WornNotifications
import app.worn.ui.TodayUiState
import app.worn.ui.WornViewModel
import app.worn.ui.components.DurationStepper
import app.worn.ui.components.PresetRow
import app.worn.ui.components.SectionLabel
import app.worn.ui.components.SettingsNavRow
import app.worn.ui.components.SettingsToggle
import app.worn.ui.components.TreatmentPlanSection
import app.worn.ui.theme.WornTheme
import java.util.UUID

@Composable
fun SettingsScreen(state: TodayUiState, vm: WornViewModel, onBack: () -> Unit) {
    val colors = WornTheme.colors
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setNotifications(granted)
        if (granted) WornNotifications.postSoundTest(context)
    }
    val settings = state.settings ?: return
    var customHours by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newMinutes by remember { mutableStateOf(15) }
    var editing by remember { mutableStateOf<ActivityType?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.text)
            }
            Text("Settings", color = colors.text, fontSize = 28.sp, fontWeight = FontWeight.Light)
        }
        Spacer(Modifier.height(28.dp))
        SectionLabel("Daily target")
        Spacer(Modifier.height(12.dp))
        Text(
            "${settings.dailyWearTargetMinutes / 60}h",
            color = colors.text,
            fontSize = 36.sp,
            fontWeight = FontWeight.Light,
        )
        Spacer(Modifier.height(8.dp))
        PresetRow(
            options = listOf(20, 21, 22),
            selected = if (customHours.isBlank()) settings.dailyWearTargetMinutes / 60 else -1,
            formatter = { "${it}h" },
        ) { hours ->
            customHours = ""
            vm.updateTarget(hours * 60)
        }
        BasicTextField(
            value = customHours,
            onValueChange = {
                customHours = it.filter(Char::isDigit).take(2)
                customHours.toIntOrNull()?.let { h -> if (h in 1..24) vm.updateTarget(h * 60) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = colors.text, fontSize = 16.sp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            decorationBox = { inner ->
                if (customHours.isEmpty()) Text("Custom hours", color = colors.tertiary, fontSize = 16.sp)
                inner()
            },
        )

        Spacer(Modifier.height(32.dp))
        SectionLabel("Removal activities")
        Spacer(Modifier.height(8.dp))
        state.activities.forEach { activity ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .clickable { editing = activity }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(activity.name, color = colors.text, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text("${activity.defaultDurationMinutes} min", color = colors.secondary, fontSize = 15.sp)
            }
        }
        if (editing != null) {
            val current = editing!!
            var minutes by remember(current.id) { mutableStateOf(current.defaultDurationMinutes) }
            DurationStepper(current.name, minutes) { minutes = it }
            Row {
                TextButton(onClick = {
                    vm.saveActivity(current.copy(defaultDurationMinutes = minutes))
                    editing = null
                }) { Text("Save") }
                if (!current.isDefault) {
                    TextButton(onClick = {
                        vm.deleteActivity(current.id)
                        editing = null
                    }) { Text("Delete", color = colors.warning) }
                }
                TextButton(onClick = { editing = null }) { Text("Cancel", color = colors.secondary) }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (!adding) {
            Text(
                "Add activity",
                color = colors.secondary,
                modifier = Modifier
                    .defaultMinSize(minHeight = 44.dp)
                    .clickable { adding = true }
                    .padding(vertical = 12.dp),
            )
        } else {
            BasicTextField(
                value = newName,
                onValueChange = { newName = it },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = colors.text, fontSize = 17.sp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                decorationBox = { inner ->
                    if (newName.isEmpty()) Text("Name", color = colors.tertiary, fontSize = 17.sp)
                    inner()
                },
            )
            DurationStepper("Duration", newMinutes) { newMinutes = it }
            TextButton(onClick = {
                if (newName.isBlank()) return@TextButton
                vm.saveActivity(
                    ActivityType(
                        id = UUID.randomUUID().toString(),
                        name = newName.trim(),
                        defaultDurationMinutes = newMinutes,
                        iconKey = "more",
                        enabled = true,
                        isDefault = false,
                        sortOrder = (state.activities.maxOfOrNull { it.sortOrder } ?: 0) + 1,
                    ),
                )
                newName = ""
                newMinutes = 15
                adding = false
            }) { Text("Save activity") }
        }

        Spacer(Modifier.height(36.dp))
        TreatmentPlanSection(settings.replacementIntervalDays) { days ->
            vm.updateReplacementInterval(days)
            ReplacementReminders.sync(context)
        }

        Spacer(Modifier.height(36.dp))
        SectionLabel("Notifications")
        Spacer(Modifier.height(8.dp))
        SettingsToggle(
            title = "Timer reminder",
            description = "When a removal timer ends",
            checked = settings.notificationsEnabled,
            onCheckedChange = vm::setNotifications,
        )
        SettingsToggle(
            title = "Removal reminder sound",
            description = "Short alert every 5 minutes after timer ends",
            checked = settings.removalReminderSoundEnabled,
            onCheckedChange = vm::setRemovalReminderSound,
        )
        SettingsNavRow(
            title = "Test notification sound",
            description = "Test your device's notification alert. Sound depends on Android notification settings.",
            onClick = {
                WornNotifications.ensureChannels(context)
                if (Build.VERSION.SDK_INT >= 33 && !WornNotifications.notificationsAllowed(context)) {
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else if (!WornNotifications.timerChannelIsAudible(context)) {
                    WornNotifications.openTimerChannelSettings(context)
                } else {
                    WornNotifications.postSoundTest(context)
                }
            },
        )
        if (!WornNotifications.timerChannelIsAudible(context) || !WornNotifications.notificationsAllowed(context)) {
            Text(
                "Sound is off in Android notification settings. Worn cannot override that.",
                color = colors.warning,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            SettingsNavRow(
                title = "Android notification settings",
                description = "Open the timer reminder channel on this device",
                onClick = { WornNotifications.openTimerChannelSettings(context) },
            )
        }
        SettingsToggle(
            title = "Aligner replacement reminder",
            description = "When the next set is due",
            checked = settings.replacementRemindersEnabled,
            onCheckedChange = {
                vm.setReplacementReminders(it)
                ReplacementReminders.sync(context)
            },
        )
        val alarm = context.getSystemService(android.app.AlarmManager::class.java)
        if (android.os.Build.VERSION.SDK_INT >= 31 && !alarm.canScheduleExactAlarms()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Exact alarms are off. Reminders still work, but timing may be less precise.",
                color = colors.tertiary,
                fontSize = 13.sp,
            )
        }

        Spacer(Modifier.height(36.dp))
        SectionLabel("About")
        Spacer(Modifier.height(10.dp))
        Text("Worn stores everything on this device. No account, no ads, no analytics.", color = colors.tertiary, fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(40.dp))
    }
}
