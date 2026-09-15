package app.worn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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

private val TimeFmt = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun DayMetrics(
    wornMillis: Long,
    remainingMillis: Long,
    notWornMillis: Long,
    targetReached: Boolean,
) {
    val colors = WornTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Metric("Worn", DurationFormat.hoursMinutesCompact(wornMillis))
        Metric(
            "Remaining",
            if (targetReached) "Target reached" else DurationFormat.hoursMinutesCompact(remainingMillis),
            emphasize = targetReached,
        )
        Metric("Not worn", DurationFormat.hoursMinutesCompact(notWornMillis))
    }
}

@Composable
private fun Metric(label: String, value: String, emphasize: Boolean = false) {
    val colors = WornTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = colors.tertiary, fontSize = 12.sp, letterSpacing = 0.4.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            color = if (emphasize) colors.accent else colors.text,
            fontSize = if (emphasize) 13.sp else 15.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun StatusDot(wearing: Boolean, label: String) {
    val colors = WornTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (wearing) colors.wearing else colors.removed),
        )
        Text(label, color = colors.secondary, fontSize = 15.sp)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, subtle: Boolean = false) {
    val colors = WornTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (subtle) colors.surface else colors.text)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (subtle) colors.text else colors.background,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.2.sp,
        )
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
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        segments.forEach { segment ->
            val start = Instant.ofEpochMilli(segment.startMillis).atZone(zone).toLocalTime().format(TimeFmt)
            val endLabel = if (segment.session.endMillis == null && segment.endMillis >= nowMillis - 2_000) {
                "now"
            } else {
                Instant.ofEpochMilli(segment.endMillis).atZone(zone).toLocalTime().format(TimeFmt)
            }
            val activity = activities.firstOrNull { it.id == segment.session.activityTypeId }
            val title = if (segment.session.kind == SessionKind.WEAR) {
                "Worn"
            } else {
                activity?.name?.uppercase() ?: "REMOVED"
            }
            Column(Modifier.clickable { onEdit(segment) }) {
                Text(
                    "$start  ·  $endLabel",
                    color = colors.secondary,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (segment.session.kind == SessionKind.WEAR) colors.text.copy(alpha = 0.85f)
                            else colors.removed.copy(alpha = 0.7f),
                        ),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    title,
                    color = colors.tertiary,
                    fontSize = 11.sp,
                    letterSpacing = 1.6.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
