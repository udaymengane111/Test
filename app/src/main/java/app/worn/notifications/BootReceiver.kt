package app.worn.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.worn.WornApp
import app.worn.domain.model.SessionKind
import app.worn.service.RemovalTimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        val pending = goAsync()
        val app = context.applicationContext as WornApp
        scope.launch {
            try {
                val open = app.repository.openSession()
                if (open != null && open.kind == SessionKind.REMOVAL) {
                    WornNotifications.scheduleExpiry(context, open)
                    RemovalTimerService.sync(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
