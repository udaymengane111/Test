package app.worn.notifications

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.worn.MainActivity
import app.worn.R
import app.worn.WornApp
import app.worn.domain.engine.ReminderAction
import app.worn.domain.engine.TreatmentPlanCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

object ReplacementReminders {
    const val CHANNEL = "replacement"
    const val ID = 43
    const val ACTION_DUE = "app.worn.action.REPLACEMENT_DUE"
    const val ACTION_SNOOZE = "app.worn.action.SNOOZE_REPLACEMENT"
    private const val PREFS = "worn_replacement"
    private const val KEY_LAST = "last_notice_date"
    private const val KEY_SNOOZE = "snooze_until"

    fun ensureChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Aligner replacement", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Reminds you when a new aligner set is due"
            },
        )
    }

    fun notification(context: Context, setNumber: Int, overdue: Boolean): Notification {
        val open = PendingIntent.getActivity(
            context,
            21,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra("open_treatment", true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val start = PendingIntent.getActivity(
            context,
            22,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra("open_treatment", true)
                .putExtra("start_new_set", true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snooze = PendingIntent.getBroadcast(
            context,
            23,
            Intent(context, TimerActionReceiver::class.java).setAction(ACTION_SNOOZE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = if (overdue) {
            "Set $setNumber is due. Record it when you can."
        } else {
            "Set $setNumber is due today."
        }
        return NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to change your aligner")
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "START NEW SET", start)
            .addAction(0, "SNOOZE", snooze)
            .build()
    }

    fun sync(context: Context) {
        CoroutineScope(Dispatchers.IO).launch { syncNow(context) }
    }

    suspend fun syncNow(context: Context) {
        ensureChannel(context)
        val app = context.applicationContext as WornApp
        val settings = app.repository.settings()
        val sets = app.repository.alignerSets()
        val zone = ZoneId.of(settings.currentZoneId)
        val today = LocalDate.now(zone)
        val snoozeUntil = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SNOOZE, null)?.let(LocalDate::parse)
        val plan = TreatmentPlanCalculator.planReminder(
            sets = sets,
            intervalDays = settings.replacementIntervalDays,
            today = today,
            zone = zone,
            enabled = settings.replacementRemindersEnabled,
            snoozeUntil = snoozeUntil,
        )
        val alarm = context.getSystemService(AlarmManager::class.java)
        val pending = duePending(context)
        alarm.cancel(pending)
        when (plan.action) {
            ReminderAction.NONE -> return
            ReminderAction.NOTIFY_NOW -> {
                maybeNotify(context, plan.nextSetNumber ?: return, overdue = plan.overdue)
            }
            ReminderAction.SCHEDULE -> {
                val trigger = plan.triggerAtMillis ?: return
                WornNotifications.scheduleWakeup(context, trigger, pending)
            }
        }
    }

    fun snoozeOneDay(context: Context) {
        val zone = ZoneId.systemDefault()
        val until = LocalDate.now(zone).plusDays(1)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SNOOZE, until.toString())
            .apply()
        NotificationManagerCompat.from(context).cancel(ID)
        sync(context)
    }

    fun clearNotice(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_LAST)
            .remove(KEY_SNOOZE)
            .apply()
        NotificationManagerCompat.from(context).cancel(ID)
    }

    suspend fun onDue(context: Context) {
        syncNow(context)
    }

    private fun maybeNotify(context: Context, setNumber: Int, overdue: Boolean) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = LocalDate.now()
        val last = prefs.getString(KEY_LAST, null)?.let(LocalDate::parse)
        if (!TreatmentPlanCalculator.shouldPostNotice(last, today)) return
        NotificationManagerCompat.from(context).notify(ID, notification(context, setNumber, overdue))
        prefs.edit().putString(KEY_LAST, today.toString()).apply()
    }

    private fun duePending(context: Context): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            24,
            Intent(context, TimerActionReceiver::class.java).setAction(ACTION_DUE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
