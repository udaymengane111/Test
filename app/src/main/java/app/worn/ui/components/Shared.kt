package app.worn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import app.worn.domain.model.ActivityType
import app.worn.domain.model.SessionKind
import app.worn.ui.theme.WornTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

private val TimeFmt = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun DayMetrics(
    wornMillis: Long,
    remainingMillis: Long,
    notWornMillis: Long,
    targetReached: Boolean,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Metric("Worn", DurationFormat.hoursMinutesCompact(wornMillis))
        Metric(
            "Remaining",
            if (targetReached) "Target reached" else DurationFormat.hoursMinutesCompact(remainingMillis),
            emphasize = targetReached,
        )
        Metric("Out today", DurationFormat.hoursMinutesCompact(notWornMillis))
    }
}

@Composable
private fun Metric(label: String, value: String, emphasize: Boolean = false) {
    val colors = WornTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(label, color = colors.tertiary, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            color = if (emphasize) colors.accent else colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
        )
    }
}

@Composable
fun WearingStatus(wearing: Boolean) {
    val colors = WornTheme.colors
    val label = if (wearing) "Wearing aligner" else "Aligner removed"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = label },
    ) {
        androidx.compose.foundation.Canvas(Modifier.size(9.dp)) {
            if (wearing) {
                drawCircle(colors.wearing)
            } else {
                drawCircle(colors.removed, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6.dp.toPx()))
            }
        }
        Text(label, color = colors.secondary, fontSize = 15.sp)
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = WornTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(colors.text)
            .semantics { role = Role.Button; contentDescription = text }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = colors.background,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.1.sp,
        )
    }
}

@Composable
fun TextAction(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = WornTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
            .semantics { role = Role.Button; contentDescription = text }
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = colors.secondary, fontSize = 15.sp)
    }
}

@Composable
fun TimelineList(
    segments: List<TimelineSegment>,
    activities: List<ActivityType>,
    zone: ZoneId,
    nowMillis: Long,
    onEdit: (TimelineSegment) -> Unit,
) {
    val colors = WornTheme.colors
    val longest = segments.maxOfOrNull { max(1L, it.endMillis - it.startMillis) } ?: 1L
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        segments.forEach { segment ->
            val start = Instant.ofEpochMilli(segment.startMillis).atZone(zone).toLocalTime().format(TimeFmt)
            val endLabel = if (segment.session.endMillis == null && segment.endMillis >= nowMillis - 2_000) {
                ""
            } else {
                Instant.ofEpochMilli(segment.endMillis).atZone(zone).toLocalTime().format(TimeFmt)
            }
            val activity = activities.firstOrNull { it.id == segment.session.activityTypeId }
            val title = if (segment.session.kind == SessionKind.WEAR) {
                "WORN"
            } else {
                (activity?.name ?: "Removed").uppercase()
            }
            val fraction = ((segment.endMillis - segment.startMillis).toFloat() / longest.toFloat()).coerceIn(0.14f, 1f)
            val worn = segment.session.kind == SessionKind.WEAR
            Column(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clickable { onEdit(segment) }
                    .semantics(mergeDescendants = true) {
                        contentDescription = "$title $start ${endLabel.ifBlank { "now" }}"
                    },
            ) {
                Text(
                    if (endLabel.isBlank()) "$start  ━" else "$start  $endLabel",
                    color = colors.secondary,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(if (worn) 3.dp else 2.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (worn) colors.text else colors.removed.copy(alpha = 0.55f)),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    title,
                    color = colors.tertiary,
                    fontSize = 11.sp,
                    letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
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
        Text(
            "−",
            color = colors.secondary,
            fontSize = 22.sp,
            modifier = Modifier
                .size(44.dp)
                .clickable { onChange((minutes - 5).coerceAtLeast(5)) }
                .semantics { role = Role.Button; contentDescription = "Decrease $label" },
            textAlign = TextAlign.Center,
        )
        Text("${minutes} min", color = colors.text, fontSize = 16.sp, modifier = Modifier.width(72.dp), textAlign = TextAlign.Center)
        Text(
            "+",
            color = colors.secondary,
            fontSize = 22.sp,
            modifier = Modifier
                .size(44.dp)
                .clickable { onChange((minutes + 5).coerceAtMost(180)) }
                .semantics { role = Role.Button; contentDescription = "Increase $label" },
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun DaysStepper(
    label: String,
    days: Int,
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
        Text(
            "−",
            color = colors.secondary,
            fontSize = 22.sp,
            modifier = Modifier
                .size(44.dp)
                .clickable { onChange((days - 1).coerceAtLeast(1)) }
                .semantics { role = Role.Button; contentDescription = "Decrease $label" },
            textAlign = TextAlign.Center,
        )
        Text("$days days", color = colors.text, fontSize = 16.sp, modifier = Modifier.width(80.dp), textAlign = TextAlign.Center)
        Text(
            "+",
            color = colors.secondary,
            fontSize = 22.sp,
            modifier = Modifier
                .size(44.dp)
                .clickable { onChange((days + 1).coerceAtMost(90)) }
                .semantics { role = Role.Button; contentDescription = "Increase $label" },
            textAlign = TextAlign.Center,
        )
    }
}
