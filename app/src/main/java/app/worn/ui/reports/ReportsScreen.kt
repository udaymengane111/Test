package app.worn.ui.reports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.worn.domain.engine.DurationFormat
import app.worn.domain.engine.ReportGrain
import app.worn.ui.TodayUiState
import app.worn.ui.WornViewModel
import app.worn.ui.components.Hairline
import app.worn.ui.components.SectionLabel
import app.worn.ui.theme.WornTheme
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ReportsScreen(state: TodayUiState, vm: WornViewModel, onBack: () -> Unit) {
    val colors = WornTheme.colors
    val report = state.report ?: return
    val title = when (report.window.grain) {
        ReportGrain.DAY -> report.window.start.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
        ReportGrain.WEEK -> "Week of " + report.window.start.format(DateTimeFormatter.ofPattern("d MMM"))
        ReportGrain.MONTH -> report.window.start.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        ReportGrain.QUARTER -> "Q${(report.window.start.monthValue - 1) / 3 + 1} ${report.window.start.year}"
        ReportGrain.YEAR -> report.window.start.year.toString()
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.text)
            }
            Text("Reports", color = colors.text, fontSize = 28.sp, fontWeight = FontWeight.Light)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ReportGrain.entries.forEach { grain ->
                val selected = state.reportGrain == grain
                Text(
                    grain.name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) },
                    color = if (selected) colors.text else colors.tertiary,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .clickable { vm.setReportGrain(grain) }
                        .padding(vertical = 8.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.shiftReport(-1) }) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous period", tint = colors.secondary)
            }
            Text(title, color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Light, modifier = Modifier.weight(1f))
            IconButton(onClick = { vm.shiftReport(1) }) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = "Next period", tint = colors.secondary)
            }
        }
        Spacer(Modifier.height(24.dp))
        if (report.daysWithData.isEmpty()) {
            Text("No wear data", color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(8.dp))
            Text("Wear time only appears for days that were tracked.", color = colors.secondary, fontSize = 15.sp)
        } else {

        SectionLabel("Total worn")
        Spacer(Modifier.height(8.dp))
        Text(DurationFormat.hoursMinutes(report.totalWornMillis), color = colors.text, fontSize = 40.sp, fontWeight = FontWeight.Light)
        Text(
            DurationFormat.percentPrecise(report.achievement) + " of target",
            color = colors.secondary,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text("Target  ${DurationFormat.hoursMinutes(report.totalTargetMillis)}", color = colors.tertiary, fontSize = 14.sp)

        Spacer(Modifier.height(28.dp))
        Hairline()
        Spacer(Modifier.height(24.dp))
        SectionLabel("Missed")
        Spacer(Modifier.height(8.dp))
        Text(DurationFormat.hoursMinutes(report.missedMillis), color = colors.text, fontSize = 28.sp, fontWeight = FontWeight.Light)
        Text("Days below target, without extra wear compensating.", color = colors.tertiary, fontSize = 13.sp)

        report.averageDailyWearMillis?.let { avg ->
            Spacer(Modifier.height(28.dp))
            Hairline()
            Spacer(Modifier.height(24.dp))
            SectionLabel("Average daily wear")
            Spacer(Modifier.height(8.dp))
            Text(DurationFormat.hoursMinutesCompact(avg), color = colors.text, fontSize = 28.sp, fontWeight = FontWeight.Light)
        }

        if (report.completeDaysWithData > 0) {
            Spacer(Modifier.height(28.dp))
            Hairline()
            Spacer(Modifier.height(24.dp))
            SectionLabel("Days on target")
            Spacer(Modifier.height(8.dp))
            Text(
                "${report.daysOnTarget} / ${report.completeDaysWithData}",
                color = colors.text,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
            )
            if (report.todayInProgress) {
                Text("Today — in progress", color = colors.tertiary, fontSize = 13.sp)
            }
        }

        if (report.bestDay != null && report.lowestDay != null && report.bestDay.date != report.lowestDay.date) {
            Spacer(Modifier.height(28.dp))
            Hairline()
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Best day", color = colors.tertiary, fontSize = 12.sp)
                    Text(DurationFormat.hoursMinutesCompact(report.bestDay.wornMillis), color = colors.text, fontSize = 18.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text("Lowest day", color = colors.tertiary, fontSize = 12.sp)
                    Text(DurationFormat.hoursMinutesCompact(report.lowestDay.wornMillis), color = colors.text, fontSize = 18.sp)
                }
            }
        }

        if (report.removals.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            Hairline()
            Spacer(Modifier.height(24.dp))
            SectionLabel("Removal time")
            Spacer(Modifier.height(12.dp))
            report.removals.forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(row.name, color = colors.text, fontSize = 16.sp)
                        Text("${row.count} times · ${DurationFormat.hoursMinutesCompact(row.actualMillis)}", color = colors.tertiary, fontSize = 13.sp)
                        if (row.count > 0 && report.window.grain != ReportGrain.DAY) {
                            Text(
                                "Average ${DurationFormat.hoursMinutesCompact(row.averageMillis)}",
                                color = colors.tertiary,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    Text(DurationFormat.hoursMinutesCompact(row.actualMillis), color = colors.secondary, fontSize = 15.sp)
                }
            }
            Text("Total out  ${DurationFormat.hoursMinutesCompact(report.totalOutMillis)}", color = colors.tertiary, fontSize = 13.sp)
            report.averageOutMillis?.let {
                Spacer(Modifier.height(6.dp))
                Text("Average time out  ${DurationFormat.hoursMinutesCompact(it)}", color = colors.tertiary, fontSize = 13.sp)
            }
        }

        if (report.window.grain != ReportGrain.DAY) {
            Spacer(Modifier.height(28.dp))
            Hairline()
            Spacer(Modifier.height(24.dp))
            SectionLabel("Daily breakdown")
            Spacer(Modifier.height(8.dp))
            report.days.forEach { day ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val label = when (report.window.grain) {
                        ReportGrain.WEEK -> day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        else -> day.date.format(DateTimeFormatter.ofPattern("d MMM"))
                    }
                    Text(label, color = colors.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    if (!day.hasData) {
                        Text("No wear data", color = colors.tertiary, fontSize = 14.sp)
                    } else {
                        Text(
                            DurationFormat.hoursMinutesCompact(day.wornMillis),
                            color = colors.secondary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                        Text(
                            DurationFormat.percent((day.wornMillis.toFloat() / day.targetMillis.toFloat()).coerceAtLeast(0f)),
                            color = colors.tertiary,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
        }
        Spacer(Modifier.height(40.dp))
    }
}
