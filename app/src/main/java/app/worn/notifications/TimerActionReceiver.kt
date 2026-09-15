package app.worn.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import app.worn.WornApp
import app.worn.domain.engine.ActivityTimerCalculator
import app.worn.domain.model.SessionKind
import app.worn.service.RemovalTimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TimerActionReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as WornApp
        val pending = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    WornNotifications.ACTION_PUT_BACK -> {
                        app.repository.putAlignerBack()
                        NotificationManagerCompat.from(context).cancel(WornNotifications.ID_TIMER_DONE)
                        RemovalTimerService.sync(context)
                    }
                    WornNotifications.ACTION_SNOOZE -> {
                        app.repository.snooze()
                        NotificationManagerCompat.from(context).cancel(WornNotifications.ID_TIMER_DONE)
                        RemovalTimerService.sync(context)
                    }
                    ReplacementReminders.ACTION_DUE -> ReplacementReminders.onDue(context)
                    ReplacementReminders.ACTION_SNOOZE -> ReplacementReminders.snoozeOneDay(context)
                    WornNotifications.ACTION_TIMER_ALARM -> {
                        val open = app.repository.openSession() ?: return@launch
                        if (open.kind != SessionKind.REMOVAL) return@launch
                        val now = System.currentTimeMillis()
                        val snap = ActivityTimerCalculator.snapshot(open, now)
                        if (snap.paused) return@launch
                        if (snap.remainingMillis > 1_000L) {
                            WornNotifications.scheduleExpiry(context, open)
                            return@launch
                        }
                        val activity = app.database.activities().get(open.activityTypeId ?: "")
                        NotificationManagerCompat.from(context).notify(
                            WornNotifications.ID_TIMER_DONE,
                            WornNotifications.timerDone(context, activity?.name ?: "Activity"),
                        )
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
