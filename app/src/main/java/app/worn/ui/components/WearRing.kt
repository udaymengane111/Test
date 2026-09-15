package app.worn.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.worn.domain.engine.DurationFormat
import app.worn.ui.theme.WornTheme

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
    val animated by animateFloatAsState(progress, animationSpec = tween(700), label = "ring")
    val ringColor = when {
        targetReached -> colors.accent
        wearing -> colors.wearing
        else -> colors.removed
    }
    Box(modifier.size(280.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(280.dp)) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = colors.ringTrack,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = DurationFormat.hoursMinutes(wornMillis),
                color = colors.text,
                fontSize = 48.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-1).sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (wearing) "WORN" else "NOT WORN",
                color = colors.secondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 3.sp,
            )
            Text(
                text = DurationFormat.percent(progress),
                color = colors.tertiary,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}
