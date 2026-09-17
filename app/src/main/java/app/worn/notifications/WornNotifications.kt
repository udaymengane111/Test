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
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.worn.MainActivity
import app.worn.R
import app.worn.domain.engine.ActivityTimerCalculator
import app.worn.domain.engine.DurationFormat
import app.worn.domain.engine.TimerNotificationCopy
import app.worn.domain.model.TrackingSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WornNotifications {
    /** Versioned so a previously silent channel cannot stick. */
    const val CHANNEL_TIMER = "timer_done_v3"
    const val CHANNEL_ONGOING = "ongoing"
    const val ID_TIMER_DONE = 41
    const val ID_ONGOING = 42
    const val ID_SOUND_TEST = 44

    const val ACTION_PUT_BACK = "app.worn.action.PUT_BACK"
    const val ACTION_SNOOZE = "app.worn.action.SNOOZE"
    const val ACTION_MUTE = "app.worn.action.MUTE_REMINDERS"
    const val ACTION_TIMER_ALARM = "app.worn.action.TIMER_ALARM"

    private val LEGACY_CHANNELS = listOf("timer", "timer_alert", "timer_done_v2")

    fun defaultSound(): Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    fun soundAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        LEGACY_CHANNELS.forEach { id ->
            if (manager.getNotificationChannel(id) != null) {
                manager.deleteNotificationChannel(id)
            }
        }
        val existing = manager.getNotificationChannel(CHANNEL_TIMER)
        if (existing == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_TIMER,
                    "Timer reminders",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Audible reminder when a removal timer ends"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 280, 160, 280)
                    enableLights(true)
                    setSound(defaultSound(), soundAttributes())
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setBypassDnd(false)
                },
            )
        }
        if (manager.getNotificationChannel(CHANNEL_ONGOING) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ONGOING,
                    "Current timer",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Shows remaining time while aligners are out"
                    setShowBadge(false)
                    setSound(null, null)
                    enableVibration(false)
                },
            )
        }
    }

    fun notificationsAllowed(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun timerChannelIsAudible(context: Context): Boolean {
        ensureChannels(context)
        val channel = context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(CHANNEL_TIMER) ?: return false
        if (channel.importance < NotificationManager.IMPORTANCE_DEFAULT) return false
        return channel.sound != null
    }

    fun openTimerChannelSettings(context: Context) {
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_TIMER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun postTimerFinished(context: Context, activityName: String, muted: Boolean) {
        if (muted || !notificationsAllowed(context)) return
        ensureChannels(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(ID_TIMER_DONE, timerDone(context, activityName, muted = false))
    }

    fun postSoundTest(context: Context): Boolean {
        ensureChannels(context)
        if (!notificationsAllowed(context)) return false
        val open = openAppIntent(context, 31)
        val notification = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Test notification sound")
            .setContentText("This uses the same channel as timer reminders.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setSound(defaultSound())
            .setVibrate(longArrayOf(0, 280, 160, 280))
            .build()
        context.getSystemService(NotificationManager::class.java).notify(ID_SOUND_TEST, notification)
        return true
    }

    fun timerDone(context: Context, activityName: String, muted: Boolean): Notification {
        val open = openAppIntent(context, 1)
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
            .setContentTitle(TimerNotificationCopy.TITLE)
            .setContentText(TimerNotificationCopy.body(activityName))
            .setStyle(NotificationCompat.BigTextStyle().bigText(TimerNotificationCopy.body(activityName)))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setTicker(TimerNotificationCopy.TITLE)
            .addAction(0, "PUT BACK", putBack)
            .addAction(0, "MUTE", mute)
        if (muted) {
            builder.setSilent(true)
        } else {
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            builder.setSound(defaultSound())
            builder.setVibrate(longArrayOf(0, 280, 160, 280))
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
        return NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Aligner removed")
            .setContentText(body)
            .setContentIntent(openAppIntent(context, 4))
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun scheduleWakeup(context: Context, triggerAtMillis: Long, pending: PendingIntent, userVisibleAlarm: Boolean = false) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val whenMs = maxOf(triggerAtMillis, System.currentTimeMillis() + 150L)
        val exactAllowed = Build.VERSION.SDK_INT < 31 || alarm.canScheduleExactAlarms()
        try {
            if (userVisibleAlarm && exactAllowed) {
                val show = openAppIntent(context, 11)
                alarm.setAlarmClock(AlarmManager.AlarmClockInfo(whenMs, show), pending)
            } else if (exactAllowed) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pending)
            } else {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pending)
            }
        } catch (_: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pending)
        }
    }

    fun scheduleExpiry(context: Context, session: TrackingSession) {
        CoroutineScope(Dispatchers.IO).launch { RemovalAlerts.sync(context, session) }
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

    private fun openAppIntent(context: Context, requestCode: Int): PendingIntent {
        return PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra("open_timer", true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
