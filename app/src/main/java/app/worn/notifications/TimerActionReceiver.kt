package app.worn.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import app.worn.WornApp
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
                        RemovalAlerts.clear(context)
                        RemovalTimerService.sync(context)
                    }
                    WornNotifications.ACTION_MUTE -> {
                        RemovalAlerts.setMuted(context, true)
                    }
                    WornNotifications.ACTION_SNOOZE -> {
                        app.repository.snooze()
                        NotificationManagerCompat.from(context).cancel(WornNotifications.ID_TIMER_DONE)
                        RemovalTimerService.sync(context)
                    }
                    ReplacementReminders.ACTION_DUE -> ReplacementReminders.onDue(context)
                    ReplacementReminders.ACTION_SNOOZE -> ReplacementReminders.snoozeOneDay(context)
                    WornNotifications.ACTION_TIMER_ALARM -> RemovalAlerts.onAlarm(context)
                    else -> {
                        val open = app.repository.openSession()
                        if (open != null && open.kind == SessionKind.REMOVAL) {
                            RemovalAlerts.sync(context, open)
                        }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
