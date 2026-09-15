package app.worn.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.worn.domain.engine.DurationFormat
import app.worn.ui.HistoryRow
import app.worn.ui.TodayUiState
import app.worn.ui.theme.WornTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(state: TodayUiState, onOpenDay: (LocalDate) -> Unit) {
    val colors = WornTheme.colors
    val today = state.historyDays.firstOrNull()?.date
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("History", color = colors.text, fontSize = 32.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(8.dp))
            Text(
                "7-day average  ${DurationFormat.hoursMinutesCompact(state.sevenDayAverageMillis)}",
                color = colors.secondary,
                fontSize = 15.sp,
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                state.weekHits.forEach { hit ->
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (hit) colors.accent else colors.ringTrack),
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }
        items(state.historyDays, key = { it.date }) { row ->
            HistoryItem(row, today) { onOpenDay(row.date) }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun HistoryItem(row: HistoryRow, today: LocalDate?, onClick: () -> Unit) {
    val colors = WornTheme.colors
    val label = when (row.date) {
        today -> "Today"
        today?.minusDays(1) -> "Yesterday"
        else -> row.date.format(DateTimeFormatter.ofPattern("MMM d"))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = colors.text, fontSize = 17.sp)
            Text(
                DurationFormat.hoursMinutesCompact(row.totals.wornMillis),
                color = colors.secondary,
                fontSize = 14.sp,
            )
        }
        MiniProgress(row.totals.progress)
        Spacer(Modifier.width(12.dp))
        Text(
            DurationFormat.percent(row.totals.progress.coerceAtMost(1f)),
            color = colors.secondary,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun MiniProgress(progress: Float) {
    val colors = WornTheme.colors
    Box(
        Modifier
            .width(44.dp)
            .height(4.dp)
            .clip(CircleShape)
            .background(colors.ringTrack),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(4.dp)
                .clip(CircleShape)
                .background(colors.accent),
        )
    }
}
