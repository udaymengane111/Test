package app.worn.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.worn.ui.components.DurationStepper
import app.worn.ui.components.PrimaryButton
import app.worn.ui.theme.WornTheme

@Composable
fun OnboardingScreen(onComplete: (targetHours: Int, tea: Int, lunch: Int, snack: Int, setNumber: Int?) -> Unit) {
    val colors = WornTheme.colors
    var step by remember { mutableIntStateOf(0) }
    var target by remember { mutableIntStateOf(22) }
    var customTarget by remember { mutableStateOf("") }
    var tea by remember { mutableIntStateOf(10) }
    var lunch by remember { mutableIntStateOf(30) }
    var snack by remember { mutableIntStateOf(15) }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(28.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("Worn", color = colors.text, fontSize = 15.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(36.dp))
            when (step) {
                0 -> {
                    Text("Your simple aligner wear-time tracker.", color = colors.text, fontSize = 32.sp, fontWeight = FontWeight.Light, lineHeight = 40.sp)
                }
                1 -> {
                    Text("Daily target", color = colors.secondary, fontSize = 15.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("How long should you wear your aligner each day?", color = colors.text, fontSize = 28.sp, fontWeight = FontWeight.Light, lineHeight = 36.sp)
                    Spacer(Modifier.height(28.dp))
                    listOf(20, 21, 22).forEach { hours ->
                        val selected = hours == target && customTarget.isBlank()
                        Text(
                            "${hours}h",
                            color = if (selected) colors.text else colors.secondary,
                            fontSize = if (selected) 28.sp else 22.sp,
                            fontWeight = FontWeight.Light,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable {
                                    target = hours
                                    customTarget = ""
                                }
                                .padding(vertical = 10.dp),
                        )
                    }
                    BasicTextField(
                        value = customTarget,
                        onValueChange = {
                            customTarget = it.filter(Char::isDigit).take(2)
                            customTarget.toIntOrNull()?.let { target = it }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = TextStyle(color = colors.text, fontSize = 17.sp),
                        decorationBox = { inner ->
                            if (customTarget.isEmpty()) Text("Custom hours", color = colors.tertiary, fontSize = 17.sp)
                            inner()
                        },
                    )
                }
                2 -> {
                    Text("Removal times", color = colors.secondary, fontSize = 15.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("How long are you usually out?", color = colors.text, fontSize = 28.sp, fontWeight = FontWeight.Light, lineHeight = 36.sp)
                    Spacer(Modifier.height(24.dp))
                    DurationStepper("Tea", tea) { tea = it }
                    DurationStepper("Lunch / Dinner", lunch) { lunch = it }
                    DurationStepper("Snacks", snack) { snack = it }
                }
                else -> {
                    Text("You’re ready.", color = colors.text, fontSize = 32.sp, fontWeight = FontWeight.Light, lineHeight = 40.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Wear tracking starts as soon as you begin.", color = colors.secondary, fontSize = 16.sp, lineHeight = 24.sp)
                }
            }
        }
        PrimaryButton(
            text = when (step) {
                0, 1, 2 -> "Continue"
                else -> "Start wearing"
            },
            onClick = {
                if (step < 3) {
                    step += 1
                } else {
                    onComplete(
                        target.coerceIn(1, 24),
                        tea,
                        lunch,
                        snack,
                        null,
                    )
                }
            },
        )
    }
}
