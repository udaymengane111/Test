package app.worn.ui.treatment

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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import app.worn.ui.TodayUiState
import app.worn.ui.WornViewModel
import app.worn.ui.components.PrimaryButton
import app.worn.ui.theme.WornTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TreatmentScreen(state: TodayUiState, vm: WornViewModel, onSettings: () -> Unit) {
    val colors = WornTheme.colors
    var showNew by remember { mutableStateOf(false) }
    val current = state.currentAligner
    val nextNumber = (current?.setNumber ?: state.alignerSets.maxOfOrNull { it.setNumber } ?: 0) + 1
    var number by remember(nextNumber) { mutableStateOf(nextNumber.toString()) }
    var notes by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Treatment", color = colors.text, fontSize = 32.sp, fontWeight = FontWeight.Light, modifier = Modifier.weight(1f))
            IconButton(onClick = onSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = colors.secondary)
            }
        }
        Spacer(Modifier.height(28.dp))
        Text("CURRENT ALIGNER", color = colors.tertiary, fontSize = 11.sp, letterSpacing = 1.8.sp)
        Spacer(Modifier.height(10.dp))
        if (current != null) {
            Text("Set ${current.setNumber}", color = colors.text, fontSize = 40.sp, fontWeight = FontWeight.Light)
            Text(
                "Started ${current.startDate.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}",
                color = colors.secondary,
                fontSize = 15.sp,
            )
            if (current.notes.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(current.notes, color = colors.secondary, fontSize = 14.sp)
            }
        } else {
            Text("No set yet", color = colors.secondary, fontSize = 17.sp)
        }
        Spacer(Modifier.height(28.dp))
        if (!showNew) {
            PrimaryButton("START NEW ALIGNER", onClick = { showNew = true })
        } else {
            Text("New set", color = colors.text, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = number,
                onValueChange = { number = it.filter(Char::isDigit).take(3) },
                label = { Text("Set number") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text("Starts today", color = colors.secondary, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Save", onClick = {
                val n = number.toIntOrNull() ?: return@PrimaryButton
                vm.startNewSet(n, LocalDate.now(), notes)
                notes = ""
                showNew = false
            })
            TextButton(onClick = { showNew = false }) { Text("Cancel", color = colors.secondary) }
        }
        if (state.alignerSets.isNotEmpty()) {
            Spacer(Modifier.height(36.dp))
            Text("HISTORY", color = colors.tertiary, fontSize = 11.sp, letterSpacing = 1.8.sp)
            Spacer(Modifier.height(12.dp))
            state.alignerSets.sortedByDescending { it.setNumber }.forEach { set ->
                val range = if (set.endDate == null) {
                    set.startDate.format(DateTimeFormatter.ofPattern("d MMM")) + " →"
                } else {
                    set.startDate.format(DateTimeFormatter.ofPattern("d MMM")) +
                        " → " + set.endDate.format(DateTimeFormatter.ofPattern("d MMM"))
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                ) {
                    Text("Set ${set.setNumber}", color = colors.text, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Text(range, color = colors.secondary, fontSize = 14.sp)
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}
