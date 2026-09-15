package app.worn.notifications

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import app.worn.MainActivity
import app.worn.R
import app.worn.domain.engine.ActivityTimerCalculator
import app.worn.domain.engine.DurationFormat
import app.worn.domain.model.TrackingSession

object WornNotifications {
    const val CHANNEL_TIMER = "timer"
    const val CHANNEL_ONGOING = "ongoing"
    const val ID_TIMER_DONE = 41
    const val ID_ONGOING = 42

    const val ACTION_PUT_BACK = "app.worn.action.PUT_BACK"
    const val ACTION_SNOOZE = "app.worn.action.SNOOZE"
    const val ACTION_TIMER_ALARM = "app.worn.action.TIMER_ALARM"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TIMER,
                "Aligner reminders",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Reminds you when an activity timer ends"
                enableVibration(true)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ONGOING,
                "Current timer",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows remaining time while aligners are out"
                setShowBadge(false)
            },
        )
    }

    fun timerDone(context: Context, activityName: String): Notification {
        val open = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val putBack = PendingIntent.getBroadcast(
            context,
            2,
            Intent(context, TimerActionReceiver::class.java).setAction(ACTION_PUT_BACK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snooze = PendingIntent.getBroadcast(
            context,
            3,
            Intent(context, TimerActionReceiver::class.java).setAction(ACTION_SNOOZE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to put your aligner back in")
            .setContentText(activityName)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "PUT BACK", putBack)
            .addAction(0, "SNOOZE 5 MIN", snooze)
            .build()
    }

    fun ongoing(context: Context, activityName: String, session: TrackingSession, nowMillis: Long): Notification {
        val snapshot = ActivityTimerCalculator.snapshot(session, nowMillis)
        val body = when {
            snapshot.paused -> "$activityName · paused"
            snapshot.overdue -> "$activityName · ${DurationFormat.timer(snapshot.overdueMillis)} over"
            else -> "$activityName · ${DurationFormat.timer(snapshot.remainingMillis)} remaining"
        }
        val open = PendingIntent.getActivity(
            context,
            4,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Aligner removed")
            .setContentText(body)
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    fun scheduleExpiry(context: Context, session: TrackingSession) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val trigger = ActivityTimerCalculator.expiryEpochMillis(session)
        val pending = expiryPending(context)
        if (trigger == null) {
            alarm.cancel(pending)
            return
        }
        if (trigger <= System.currentTimeMillis()) {
            context.sendBroadcast(Intent(context, TimerActionReceiver::class.java).setAction(ACTION_TIMER_ALARM))
            return
        }
        if (Build.VERSION.SDK_INT >= 31 && !alarm.canScheduleExactAlarms()) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        } else {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        }
    }

    fun cancelExpiry(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        alarm.cancel(expiryPending(context))
    }

    private fun expiryPending(context: Context): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            10,
            Intent(context, TimerActionReceiver::class.java).setAction(ACTION_TIMER_ALARM),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
