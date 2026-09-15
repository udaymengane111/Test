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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import app.worn.ui.TodayUiState
import app.worn.ui.WornViewModel
import app.worn.ui.components.PrimaryButton
import app.worn.ui.components.SectionLabel
import app.worn.ui.theme.WornTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TreatmentScreen(state: TodayUiState, vm: WornViewModel, onSettings: () -> Unit) {
    val colors = WornTheme.colors
    var showNew by remember { mutableStateOf(false) }
    val current = state.currentAligner
    val nextNumber = (current?.setNumber ?: state.alignerSets.maxOfOrNull { it.setNumber } ?: 0) + 1
    var number by remember(showNew, nextNumber, current == null) {
        mutableStateOf((if (current == null) 1 else nextNumber).toString())
    }

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
            Text("Set ${current.setNumber}", color = colors.text, fontSize = 44.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(8.dp))
            Text("Started", color = colors.tertiary, fontSize = 13.sp)
            Text(
                current.startDate.format(DateTimeFormatter.ofPattern("d MMM yyyy")),
                color = colors.secondary,
                fontSize = 16.sp,
            )
        } else {
            Text("No aligner recorded", color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(8.dp))
            Text("Add the set you’re wearing now.", color = colors.secondary, fontSize = 15.sp)
        }
        Spacer(Modifier.height(32.dp))
        if (!showNew) {
            PrimaryButton(if (current == null) "ADD CURRENT ALIGNER" else "START NEW ALIGNER") {
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
            Text("Today", color = colors.text, fontSize = 17.sp)
            Spacer(Modifier.height(24.dp))
            PrimaryButton("SAVE") {
                val n = number.toIntOrNull() ?: return@PrimaryButton
                vm.startNewSet(n, LocalDate.now(), "")
                showNew = false
            }
            TextButton(onClick = { showNew = false }) { Text("Cancel", color = colors.secondary) }
        }
        val history = state.alignerSets.filter { it.id != current?.id }.sortedByDescending { it.setNumber }
        if (history.isNotEmpty()) {
            Spacer(Modifier.height(40.dp))
            SectionLabel("Aligner history")
            Spacer(Modifier.height(12.dp))
            history.forEach { set ->
                val range = if (set.endDate == null) {
                    set.startDate.format(DateTimeFormatter.ofPattern("d MMM")) + " →"
                } else {
                    set.startDate.format(DateTimeFormatter.ofPattern("d MMM")) +
                        " → " + set.endDate.format(DateTimeFormatter.ofPattern("d MMM"))
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                ) {
                    Text("Set ${set.setNumber}", color = colors.text, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Text(range, color = colors.secondary, fontSize = 15.sp)
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}
