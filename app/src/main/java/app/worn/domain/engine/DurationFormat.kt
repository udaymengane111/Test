package app.worn.domain.engine

import java.util.concurrent.TimeUnit
import kotlin.math.abs

object DurationFormat {

    fun hoursMinutes(millis: Long): String {
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(abs(millis))
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "${hours}h ${minutes.toString().padStart(2, '0')}m"
    }

    fun hoursMinutesCompact(millis: Long): String {
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(abs(millis))
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "${hours}h ${minutes}m"
    }

    fun timer(millis: Long): String {
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(abs(millis)).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    fun timerVerbose(millis: Long): String {
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(abs(millis)).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "${minutes.toString().padStart(2, '0')} min ${seconds.toString().padStart(2, '0')} sec"
    }

    fun percent(progress: Float): String {
        val clamped = progress.coerceAtLeast(0f)
        if (clamped < 0.01f) return "<1%"
        val pct = (clamped * 100f).toInt()
        return "$pct%"
    }

    fun percentPrecise(progress: Float): String {
        val clamped = progress.coerceAtLeast(0f) * 100f
        return "%.1f%%".format(clamped)
    }
}
