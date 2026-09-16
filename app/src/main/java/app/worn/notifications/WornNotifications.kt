package app.worn.notifications

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import app.worn.MainActivity
import app.worn.R
import app.worn.domain.engine.ActivityTimerCalculator
import app.worn.domain.engine.DurationFormat
import app.worn.domain.model.TrackingSession

object WornNotifications {
    const val CHANNEL_TIMER = "timer_alert"
    const val CHANNEL_ONGOING = "ongoing"
    const val ID_TIMER_DONE = 41
    const val ID_ONGOING = 42

    const val ACTION_PUT_BACK = "app.worn.action.PUT_BACK"
    const val ACTION_SNOOZE = "app.worn.action.SNOOZE"
    const val ACTION_MUTE = "app.worn.action.MUTE_REMINDERS"
    const val ACTION_TIMER_ALARM = "app.worn.action.TIMER_ALARM"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TIMER,
                "Aligner put-back reminders",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Short audible reminder when a removal timer ends, repeating every 5 minutes"
                enableVibration(true)
                setSound(sound, attrs)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
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
                setSound(null, null)
            },
        )
    }

    fun timerDone(context: Context, activityName: String, muted: Boolean): Notification {
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
        val mute = PendingIntent.getBroadcast(
            context,
            5,
            Intent(context, TimerActionReceiver::class.java).setAction(ACTION_MUTE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to put your aligner back")
            .setContentText("$activityName timer finished.")
            .setContentIntent(open)
            .setAutoCancel(false)
            .setOnlyAlertOnce(false)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "PUT BACK", putBack)
            .addAction(0, "MUTE", mute)
        if (muted) {
            builder.setSilent(true)
        } else {
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
        }
        return builder.build()
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

    fun scheduleWakeup(context: Context, triggerAtMillis: Long, pending: PendingIntent) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val exactAllowed = Build.VERSION.SDK_INT < 31 || alarm.canScheduleExactAlarms()
        try {
            if (exactAllowed) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            } else {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            }
        } catch (_: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        }
    }

    fun scheduleExpiry(context: Context, session: TrackingSession) {
        RemovalAlerts.sync(context, session)
    }

    fun cancelExpiry(context: Context) {
        RemovalAlerts.cancel(context)
    }

    fun expiryPending(context: Context): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            10,
            Intent(context, TimerActionReceiver::class.java).setAction(ACTION_TIMER_ALARM),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
