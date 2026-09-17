package app.worn.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.worn.domain.engine.DurationFormat
import app.worn.ui.theme.WornTheme
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private const val MinVisibleSweepDegrees = 16f

@Composable
fun WearRing(
    wornMillis: Long,
    targetMillis: Long,
    wearing: Boolean,
    targetReached: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = WornTheme.colors
    val progress = if (targetMillis <= 0L) 0f else (wornMillis.toFloat() / targetMillis.toFloat()).coerceIn(0f, 1f)
    val animated by animateFloatAsState(progress, animationSpec = tween(900), label = "ring")
    val ringColor = when {
        targetReached -> colors.accent
        else -> colors.wearing
    }
    val wornLabel = DurationFormat.hoursMinutes(wornMillis)
    val description = "$wornLabel worn today, ${DurationFormat.percent(progress)} of ${DurationFormat.hoursMinutes(targetMillis)} target"
    Box(
        modifier
            .size(228.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(228.dp)) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2 + 2.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val origin = Offset(inset, inset)
            val radius = arcSize.width / 2f
            val center = Offset(origin.x + radius, origin.y + radius)
            drawArc(
                color = colors.ringTrack,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = origin,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (wornMillis > 0L) {
                val sweep = max(360f * animated, MinVisibleSweepDegrees)
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = origin,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                val angle = Math.toRadians((-90f + sweep).toDouble())
                drawCircle(
                    color = ringColor,
                    radius = stroke / 2f + 1.dp.toPx(),
                    center = Offset(
                        center.x + radius * cos(angle).toFloat(),
                        center.y + radius * sin(angle).toFloat(),
                    ),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 28.dp)) {
            Text(
                text = wornLabel,
                color = colors.text,
                fontSize = 40.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-0.8).sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (targetReached) "Target reached" else "worn today",
                color = if (targetReached) colors.accent else colors.secondary,
                fontSize = 13.sp,
            )
        }
    }
}
