package app.worn.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.worn.ui.components.PrimaryButton
import app.worn.ui.theme.WornTheme

@Composable
fun OnboardingScreen(onComplete: (targetHours: Int, tea: Int, lunch: Int, snack: Int, setNumber: Int?) -> Unit) {
    val colors = WornTheme.colors
    var step by remember { mutableIntStateOf(0) }
    var target by remember { mutableIntStateOf(22) }
    var customTarget by remember { mutableStateOf("") }
    var tea by remember { mutableStateOf("10") }
    var lunch by remember { mutableStateOf("30") }
    var snack by remember { mutableStateOf("15") }
    var setNumber by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(28.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("Worn", color = colors.text, fontSize = 18.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(28.dp))
            when (step) {
                0 -> {
                    Text("How long do you aim to wear them each day?", color = colors.text, fontSize = 28.sp, fontWeight = FontWeight.Light, lineHeight = 36.sp)
                    Spacer(Modifier.height(28.dp))
                    listOf(20, 21, 22).forEach { hours ->
                        Choice(hours.toString() + " hours", hours == target && customTarget.isBlank()) {
                            target = hours
                            customTarget = ""
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customTarget,
                        onValueChange = {
                            customTarget = it.filter(Char::isDigit).take(2)
                            customTarget.toIntOrNull()?.let { target = it }
                        },
                        label = { Text("Custom hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                1 -> {
                    Text("How long are you usually out?", color = colors.text, fontSize = 28.sp, fontWeight = FontWeight.Light, lineHeight = 36.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("You can change these later.", color = colors.secondary, fontSize = 15.sp)
                    Spacer(Modifier.height(24.dp))
                    DurationField("Tea", tea) { tea = it }
                    DurationField("Lunch / Dinner", lunch) { lunch = it }
                    DurationField("Snack", snack) { snack = it }
                }
                else -> {
                    Text("Which aligner set are you on?", color = colors.text, fontSize = 28.sp, fontWeight = FontWeight.Light, lineHeight = 36.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Optional. You can add this later.", color = colors.secondary, fontSize = 15.sp)
                    Spacer(Modifier.height(24.dp))
                    OutlinedTextField(
                        value = setNumber,
                        onValueChange = { setNumber = it.filter(Char::isDigit).take(3) },
                        label = { Text("Set number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        PrimaryButton(
            text = if (step < 2) "Continue" else "Start wearing",
            onClick = {
                if (step < 2) {
                    step += 1
                } else {
                    onComplete(
                        target.coerceIn(1, 24),
                        tea.toIntOrNull() ?: 10,
                        lunch.toIntOrNull() ?: 30,
                        snack.toIntOrNull() ?: 15,
                        setNumber.toIntOrNull(),
                    )
                }
            },
        )
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = WornTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) colors.text else colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(label, color = if (selected) colors.background else colors.text, fontSize = 17.sp)
    }
}

@Composable
private fun DurationField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(3)) },
        label = { Text(label) },
        suffix = { Text("min") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    )
}
