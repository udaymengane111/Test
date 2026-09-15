package app.worn.ui.history

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
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.worn.domain.engine.DurationFormat
import app.worn.ui.HistoryRow
import app.worn.ui.TodayUiState
import app.worn.ui.components.SectionLabel
import app.worn.ui.theme.WornTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HistoryScreen(state: TodayUiState, onOpenDay: (LocalDate) -> Unit, onReports: () -> Unit) {
    val colors = WornTheme.colors
    if (!state.hasHistory) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 20.dp),
        ) {
            Text("History", color = colors.text, fontSize = 32.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(8.dp))
            Text(
                "Reports",
                color = colors.secondary,
                fontSize = 15.sp,
                modifier = Modifier.defaultMinSize(minHeight = 44.dp).clickable(onClick = onReports).padding(vertical = 8.dp),
            )
            Spacer(Modifier.height(40.dp))
            Text("No history yet", color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(8.dp))
            Text("Your daily wear history will appear here.", color = colors.secondary, fontSize = 15.sp)
        }
        return
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("History", color = colors.text, fontSize = 32.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(8.dp))
            Text(
                "Reports",
                color = colors.secondary,
                fontSize = 15.sp,
                modifier = Modifier.defaultMinSize(minHeight = 44.dp).clickable(onClick = onReports).padding(vertical = 8.dp),
            )
            Spacer(Modifier.height(20.dp))
            SectionLabel("This week")
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.thisWeek.forEach { row ->
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (row.totals.targetReached) colors.accent else colors.ringTrack)
                            .semantics {
                                contentDescription = if (row.totals.targetReached) "Target reached" else "Below target"
                            },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        items(state.thisWeek, key = { "w-${it.date}" }) { row ->
            HistoryItem(row, weekday = true) { onOpenDay(row.date) }
        }
        item {
            Spacer(Modifier.height(28.dp))
            Text("7-day average", color = colors.tertiary, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                DurationFormat.hoursMinutesCompact(state.sevenDayAverageMillis),
                color = colors.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Light,
            )
        }
        if (state.earlierDays.isNotEmpty()) {
            item {
                Spacer(Modifier.height(36.dp))
                SectionLabel("Earlier")
                Spacer(Modifier.height(8.dp))
            }
            items(state.earlierDays, key = { it.date }) { row ->
                HistoryItem(row, weekday = false) { onOpenDay(row.date) }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun HistoryItem(row: HistoryRow, weekday: Boolean, onClick: () -> Unit) {
    val colors = WornTheme.colors
    val label = if (weekday) {
        row.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    } else {
        row.date.format(DateTimeFormatter.ofPattern("MMM d"))
    }
    val pct = DurationFormat.percent(row.totals.progress.coerceAtMost(1f))
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label ${DurationFormat.hoursMinutesCompact(row.totals.wornMillis)} $pct"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = colors.text, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(
            DurationFormat.hoursMinutesCompact(row.totals.wornMillis),
            color = colors.secondary,
            fontSize = 15.sp,
            modifier = Modifier.padding(end = 16.dp),
        )
        Text(pct, color = colors.tertiary, fontSize = 15.sp)
    }
}
