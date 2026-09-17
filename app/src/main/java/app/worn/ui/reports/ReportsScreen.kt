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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.worn.domain.engine.DurationFormat
import app.worn.domain.engine.PeriodReport
import app.worn.domain.engine.ReportGrain
import app.worn.ui.TodayUiState
import app.worn.ui.WornViewModel
import app.worn.ui.components.Hairline
import app.worn.ui.components.SectionLabel
import app.worn.ui.theme.WornTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ReportsScreen(state: TodayUiState, vm: WornViewModel, onBack: () -> Unit) {
    val colors = WornTheme.colors
    val report = state.report ?: return
    val title = when (report.window.grain) {
        ReportGrain.DAY -> report.window.start.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
        ReportGrain.WEEK -> {
            val a = report.window.start.format(DateTimeFormatter.ofPattern("d MMM"))
            val b = report.window.endInclusive.format(DateTimeFormatter.ofPattern("d MMM"))
            "Week of $a–$b"
        }
        ReportGrain.MONTH -> report.window.start.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        ReportGrain.QUARTER -> {
            val q = (report.window.start.monthValue - 1) / 3 + 1
            "Q$q ${report.window.start.year}"
        }
        ReportGrain.YEAR -> report.window.start.year.toString()
    }
    val today = LocalDate.now()

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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            ReportGrain.entries.forEach { grain ->
                val selected = state.reportGrain == grain
                Text(
                    grain.label(),
                    color = if (selected) colors.text else colors.secondary,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .semantics { role = Role.Button }
                        .clickable { vm.setReportGrain(grain) }
                        .padding(horizontal = 2.dp, vertical = 14.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.shiftReport(-1) }) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous period", tint = colors.secondary)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.Light)
                if (report.window.grain == ReportGrain.QUARTER) {
                    Text(
                        report.window.start.format(DateTimeFormatter.ofPattern("MMM")) +
                            " – " +
                            report.window.endInclusive.format(DateTimeFormatter.ofPattern("MMM")),
                        color = colors.secondary,
                        fontSize = 13.sp,
                    )
                }
            }
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
            ReportBody(report, today)
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun ReportBody(report: PeriodReport, today: LocalDate) {
    val colors = WornTheme.colors
    SectionLabel("Total worn")
    Spacer(Modifier.height(10.dp))
    Text(DurationFormat.hoursMinutes(report.totalWornMillis), color = colors.text, fontSize = 40.sp, fontWeight = FontWeight.Light)
    Spacer(Modifier.height(6.dp))
    Text(DurationFormat.percentPrecise(report.achievement) + " of target", color = colors.secondary, fontSize = 15.sp)
    Spacer(Modifier.height(4.dp))
    Text("Target  ${DurationFormat.hoursMinutes(report.totalTargetMillis)}", color = colors.secondary, fontSize = 15.sp)

    Spacer(Modifier.height(32.dp))
    Hairline()
    Spacer(Modifier.height(28.dp))
    SectionLabel("Missed")
    Spacer(Modifier.height(10.dp))
    Text(DurationFormat.hoursMinutes(report.missedMillis), color = colors.text, fontSize = 32.sp, fontWeight = FontWeight.Light)
    Spacer(Modifier.height(4.dp))
    Text("Time below target, counted per day.", color = colors.tertiary, fontSize = 13.sp)

    report.averageDailyWearMillis?.let { avg ->
        if (report.window.grain != ReportGrain.DAY) {
            Spacer(Modifier.height(32.dp))
            Hairline()
            Spacer(Modifier.height(28.dp))
            SectionLabel("Average daily wear")
            Spacer(Modifier.height(10.dp))
            Text(DurationFormat.hoursMinutes(avg), color = colors.text, fontSize = 32.sp, fontWeight = FontWeight.Light)
        }
    }

    Spacer(Modifier.height(32.dp))
    Hairline()
    Spacer(Modifier.height(28.dp))
    SectionLabel("Out")
    Spacer(Modifier.height(10.dp))
    Text(DurationFormat.hoursMinutes(report.totalOutMillis), color = colors.text, fontSize = 32.sp, fontWeight = FontWeight.Light)

    if (report.completeDaysWithData > 0 && report.window.grain != ReportGrain.DAY) {
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
        if (report.todayInProgress && report.days.any { it.todayInProgress && it.hasData && !it.targetReached }) {
            Text("Today — in progress", color = colors.tertiary, fontSize = 13.sp)
        }
    }

    Spacer(Modifier.height(28.dp))
    Hairline()
    Spacer(Modifier.height(24.dp))
    SectionLabel("Removal time")
    Spacer(Modifier.height(12.dp))
    report.removals.forEach { row ->
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(row.name, color = colors.text, fontSize = 16.sp)
                val count = if (row.count == 1) "1 time" else "${row.count} times"
                Text(
                    if (row.count == 0) DurationFormat.span(0) else "$count · ${DurationFormat.span(row.actualMillis)}",
                    color = colors.tertiary,
                    fontSize = 13.sp,
                )
            }
            Text(DurationFormat.span(row.actualMillis), color = colors.secondary, fontSize = 15.sp)
        }
    }

    if (report.window.grain == ReportGrain.QUARTER || report.window.grain == ReportGrain.YEAR) {
        Spacer(Modifier.height(28.dp))
        Hairline()
        Spacer(Modifier.height(24.dp))
        SectionLabel(if (report.window.grain == ReportGrain.YEAR) "Monthly breakdown" else "Months")
        Spacer(Modifier.height(8.dp))
        report.monthBuckets.forEach { bucket ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    bucket.yearMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    color = colors.text,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                )
                if (!bucket.hasData) {
                    Text("No data", color = colors.tertiary, fontSize = 14.sp)
                } else {
                    Text(DurationFormat.hoursMinutes(bucket.wornMillis), color = colors.secondary, fontSize = 15.sp)
                }
            }
        }
    } else if (report.window.grain != ReportGrain.DAY) {
        Spacer(Modifier.height(28.dp))
        Hairline()
        Spacer(Modifier.height(24.dp))
        SectionLabel("Daily breakdown")
        Spacer(Modifier.height(8.dp))
        report.days.filter { !it.date.isAfter(today) }.forEach { day ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val label = when (report.window.grain) {
                    ReportGrain.WEEK -> day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    else -> day.date.format(DateTimeFormatter.ofPattern("d MMM"))
                }
                Text(label, color = colors.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                if (!day.hasData) {
                    Text("No data", color = colors.tertiary, fontSize = 14.sp)
                } else {
                    Text(
                        DurationFormat.hoursMinutes(day.wornMillis),
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

private fun ReportGrain.label(): String = when (this) {
    ReportGrain.DAY -> "Day"
    ReportGrain.WEEK -> "Week"
    ReportGrain.MONTH -> "Month"
    ReportGrain.QUARTER -> "Quarter"
    ReportGrain.YEAR -> "Year"
}
