package app.worn.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.worn.domain.engine.DurationFormat
import app.worn.ui.HistoryRow
import app.worn.ui.TodayUiState
import app.worn.ui.components.Hairline
import app.worn.ui.components.SectionLabel
import app.worn.ui.theme.WornLayout
import app.worn.ui.theme.WornTheme
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HistoryScreen(state: TodayUiState, onOpenDay: (LocalDate) -> Unit, onReports: () -> Unit) {
    val colors = WornTheme.colors
    val zone = ZoneId.of(state.settings?.currentZoneId ?: ZoneId.systemDefault().id)
    val today = LocalDate.now(zone)

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = WornLayout.pagePadding),
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Text("History", color = colors.text, fontSize = WornLayout.titleSize, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(4.dp))
            ReportsLink(onReports)
            Spacer(Modifier.height(24.dp))
            SectionLabel("7-day average")
            Spacer(Modifier.height(8.dp))
            Text(
                DurationFormat.hoursMinutes(state.sevenDayAverageMillis),
                color = colors.text,
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
            )
            if (!state.hasHistory) {
                Spacer(Modifier.height(4.dp))
                Text("No recorded days yet", color = colors.tertiary, fontSize = 13.sp)
            }
            Spacer(Modifier.height(28.dp))
            SectionLabel("This week")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                state.thisWeek.forEach { row ->
                    val future = row.date.isAfter(today)
                    val fill = when {
                        future -> Color.Transparent
                        row.hasData && row.totals.targetReached -> colors.accent
                        row.hasData -> colors.removed.copy(alpha = 0.45f)
                        else -> colors.ringTrack
                    }
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(fill)
                            .then(
                                if (future) Modifier.border(1.dp, colors.ringTrack, CircleShape) else Modifier,
                            )
                            .semantics {
                                contentDescription = when {
                                    future -> "Upcoming"
                                    !row.hasData -> "No data"
                                    row.totals.targetReached -> "Target reached"
                                    else -> "Below target"
                                }
                            },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        items(state.thisWeek, key = { "w-${it.date}" }) { row ->
            HistoryItem(row, weekday = true, today = today) { onOpenDay(row.date) }
        }
        if (state.earlierDays.isNotEmpty()) {
            item {
                Spacer(Modifier.height(28.dp))
                Hairline()
                Spacer(Modifier.height(24.dp))
                SectionLabel("Earlier")
                Spacer(Modifier.height(8.dp))
            }
            items(state.earlierDays, key = { it.date }) { row ->
                HistoryItem(row, weekday = false, today = today) { onOpenDay(row.date) }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun HistoryItem(row: HistoryRow, weekday: Boolean, today: LocalDate, onClick: () -> Unit) {
    val colors = WornTheme.colors
    val future = row.date.isAfter(today)
    val label = if (weekday) {
        row.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    } else {
        row.date.format(DateTimeFormatter.ofPattern("EEE d MMM"))
    }
    val worn = DurationFormat.hoursMinutes(row.totals.wornMillis)
    val pct = DurationFormat.percent(row.totals.progress.coerceAtMost(1f))
    val value = when {
        future -> "—"
        !row.hasData -> "No data"
        else -> worn
    }
    val secondary = when {
        future || !row.hasData -> ""
        else -> pct
    }
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clickable(enabled = !future, onClick = onClick)
            .padding(vertical = 12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label $value $secondary"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = colors.text, fontSize = 16.sp)
            if (row.date == today) {
                Text("Today", color = colors.tertiary, fontSize = 12.sp)
            }
        }
        Text(
            value,
            color = if (future || !row.hasData) colors.tertiary else colors.text,
            fontSize = 16.sp,
            modifier = Modifier.padding(end = if (secondary.isNotEmpty()) 12.dp else 0.dp),
        )
        if (secondary.isNotEmpty()) {
            Text(secondary, color = colors.tertiary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ReportsLink(onReports: () -> Unit) {
    val colors = WornTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
            .clickable(onClick = onReports)
            .padding(vertical = 8.dp)
            .semantics { contentDescription = "Open reports" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Reports", color = colors.secondary, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = colors.tertiary, modifier = Modifier.size(20.dp))
    }
}
