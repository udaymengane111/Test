package app.worn.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.worn.domain.engine.DurationFormat
import app.worn.domain.engine.TimelineSegment
import app.worn.domain.engine.TimerSnapshot
import app.worn.domain.model.ActivityType
import app.worn.domain.model.SessionKind
import app.worn.ui.theme.WornTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TimeFmt = DateTimeFormatter.ofPattern("HH:mm")

enum class ButtonTone { Positive, Removal }

@Composable
fun DayMetrics(
    wornMillis: Long,
    remainingMillis: Long,
    notWornMillis: Long,
    targetReached: Boolean,
) {
    Row(Modifier.fillMaxWidth()) {
        Metric("Worn", DurationFormat.hoursMinutes(wornMillis), Modifier.weight(1f))
        Metric(
            "Remaining",
            if (targetReached) "Target reached" else DurationFormat.hoursMinutes(remainingMillis),
            Modifier.weight(1f),
            emphasize = targetReached,
        )
        Metric("Out today", DurationFormat.hoursMinutes(notWornMillis), Modifier.weight(1f))
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier, emphasize: Boolean = false) {
    val colors = WornTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.padding(horizontal = 4.dp)) {
        Text(label, color = colors.tertiary, fontSize = 12.sp, letterSpacing = 0.2.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            value,
            color = if (emphasize) colors.accent else colors.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun WearingStatus(wearing: Boolean) {
    val colors = WornTheme.colors
    val label = if (wearing) "Aligner in" else "Aligner removed"
    val accent = if (wearing) colors.wearing else colors.removed
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = label },
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Text(label, color = accent, fontSize = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    tone: ButtonTone = ButtonTone.Positive,
    onClick: () -> Unit,
) {
    val colors = WornTheme.colors
    val background = when (tone) {
        ButtonTone.Positive -> colors.accent
        ButtonTone.Removal -> colors.removed
    }
    val foreground = when (tone) {
        ButtonTone.Positive -> Color.White
        ButtonTone.Removal -> colors.text
    }
    Box(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 54.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .semantics { role = Role.Button; contentDescription = text }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = foreground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.2.sp,
        )
    }
}

@Composable
fun TextAction(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = WornTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .semantics { role = Role.Button; contentDescription = text }
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = colors.secondary, fontSize = 16.sp)
    }
}

@Composable
fun TimelineList(
    segments: List<TimelineSegment>,
    activities: List<ActivityType>,
    zone: ZoneId,
    nowMillis: Long,
    timer: TimerSnapshot? = null,
    wornMillis: Long? = null,
    outMillis: Long? = null,
    onEdit: (TimelineSegment) -> Unit,
) {
    val colors = WornTheme.colors
    Column {
        if (wornMillis != null && outMillis != null) {
            Text(
                "Worn · ${DurationFormat.span(wornMillis)}  ·  Out · ${DurationFormat.span(outMillis)}",
                color = colors.tertiary,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
        }
        segments.forEachIndexed { index, segment ->
            val start = Instant.ofEpochMilli(segment.startMillis).atZone(zone).toLocalTime().format(TimeFmt)
            val current = segment.session.endMillis == null && segment.endMillis >= nowMillis - 2_000
            val endLabel = if (current) "now" else Instant.ofEpochMilli(segment.endMillis).atZone(zone).toLocalTime().format(TimeFmt)
            val activity = activities.firstOrNull { it.id == segment.session.activityTypeId }
            val worn = segment.session.kind == SessionKind.WEAR
            val title = if (worn) "Worn" else (activity?.name ?: "Removed")
            val duration = DurationFormat.span(segment.endMillis - segment.startMillis)
            val accent = if (worn) colors.wearing else colors.removed
            val titleColor = if (current) accent else colors.text
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .defaultMinSize(minHeight = 56.dp)
                    .clickable { onEdit(segment) }
                    .semantics(mergeDescendants = true) {
                        contentDescription = "$title $start $endLabel $duration"
                    },
            ) {
                TimelineRail(
                    color = accent,
                    filled = current || worn,
                    isFirst = index == 0,
                    isLast = index == segments.lastIndex,
                )
                Column(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 14.dp).weight(1f)) {
                    Text("$start — $endLabel", color = colors.secondary, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(title, color = titleColor, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    if (current && !worn && timer != null) {
                        Text("${DurationFormat.span(timer.elapsedMillis)} elapsed", color = colors.secondary, fontSize = 14.sp)
                        if (timer.overdue) {
                            Text("${DurationFormat.span(timer.overdueMillis)} over", color = colors.warning, fontSize = 14.sp)
                        }
                    } else {
                        Text(duration, color = colors.tertiary, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineRail(
    color: Color,
    filled: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
) {
    val colors = WornTheme.colors
    Box(
        Modifier
            .width(16.dp)
            .fillMaxHeight()
            .heightIn(min = 56.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(Modifier.fillMaxWidth().fillMaxHeight()) {
            val x = size.width / 2f
            val dotY = 18.dp.toPx()
            val lineColor = colors.hairline
            if (!isFirst) {
                drawLine(
                    color = lineColor,
                    start = Offset(x, 0f),
                    end = Offset(x, dotY - 6.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            if (!isLast) {
                drawLine(
                    color = lineColor,
                    start = Offset(x, dotY + 6.dp.toPx()),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }
        Box(
            Modifier
                .padding(top = 14.dp)
                .size(9.dp)
                .clip(CircleShape)
                .background(if (filled) color else color.copy(alpha = 0.35f)),
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    val colors = WornTheme.colors
    Text(
        text.uppercase(),
        color = colors.tertiary,
        fontSize = 11.sp,
        letterSpacing = 1.6.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
fun Hairline() {
    val colors = WornTheme.colors
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.hairline))
}

@Composable
fun DurationStepper(
    label: String,
    minutes: Int,
    onChange: (Int) -> Unit,
) {
    val colors = WornTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = colors.text, fontSize = 17.sp, modifier = Modifier.weight(1f))
        StepperButton("−", "Decrease $label") { onChange((minutes - 5).coerceAtLeast(5)) }
        Text("${minutes} min", color = colors.text, fontSize = 16.sp, modifier = Modifier.width(80.dp), textAlign = TextAlign.Center)
        StepperButton("+", "Increase $label") { onChange((minutes + 5).coerceAtMost(180)) }
    }
}

@Composable
fun DaysStepper(
    label: String,
    days: Int,
    onChange: (Int) -> Unit,
) {
    val colors = WornTheme.colors
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, color = colors.secondary, fontSize = 14.sp)
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            StepperButton("−", "Decrease $label") { onChange((days - 1).coerceAtLeast(1)) }
            Text(
                "$days days",
                color = colors.text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.width(120.dp),
                textAlign = TextAlign.Center,
            )
            StepperButton("+", "Increase $label") { onChange((days + 1).coerceAtMost(90)) }
        }
    }
}

@Composable
private fun StepperButton(symbol: String, description: String, onClick: () -> Unit) {
    val colors = WornTheme.colors
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button; contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = colors.secondary, fontSize = 24.sp, fontWeight = FontWeight.Light)
    }
}

@Composable
fun PresetRow(
    options: List<Int>,
    selected: Int,
    formatter: (Int) -> String = { it.toString() },
    onSelect: (Int) -> Unit,
) {
    val colors = WornTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        options.forEach { value ->
            val on = selected == value
            Text(
                formatter(value),
                color = if (on) colors.text else colors.secondary,
                fontSize = 18.sp,
                fontWeight = if (on) FontWeight.Medium else FontWeight.Light,
                modifier = Modifier
                    .defaultMinSize(minWidth = 56.dp, minHeight = 48.dp)
                    .semantics { role = Role.Button }
                    .clickable { onSelect(value) }
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun TreatmentPlanSection(
    intervalDays: Int,
    onChange: (Int) -> Unit,
) {
    val colors = WornTheme.colors
    Column(Modifier.fillMaxWidth()) {
        SectionLabel("Treatment plan")
        Spacer(Modifier.height(16.dp))
        Text("Replace aligner every", color = colors.secondary, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "$intervalDays days",
            color = colors.text,
            fontSize = 36.sp,
            fontWeight = FontWeight.Light,
        )
        Spacer(Modifier.height(8.dp))
        PresetRow(listOf(7, 10, 14), intervalDays, onSelect = onChange)
        Spacer(Modifier.height(4.dp))
        DaysStepper("Custom days", intervalDays.coerceIn(1, 90), onChange)
    }
}

@Composable
fun SettingsToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = WornTheme.colors
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, color = colors.text, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(description, color = colors.tertiary, fontSize = 13.sp, lineHeight = 18.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = colors.ringTrack,
            ),
        )
    }
}

@Composable
fun SettingsNavRow(
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    val colors = WornTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
            .semantics { role = Role.Button; contentDescription = title },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = colors.text, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(description, color = colors.tertiary, fontSize = 13.sp, lineHeight = 18.sp)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = colors.tertiary)
    }
}
