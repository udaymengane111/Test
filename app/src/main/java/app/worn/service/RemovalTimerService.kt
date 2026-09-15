package app.worn.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import app.worn.WornApp
import app.worn.domain.engine.ActivityTimerCalculator
import app.worn.domain.model.SessionKind
import app.worn.notifications.WornNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RemovalTimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ticker: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        WornNotifications.ensureChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as WornApp
        scope.launch {
            val open = app.repository.openSession()
            if (open == null || open.kind != SessionKind.REMOVAL) {
                stopSelf()
                return@launch
            }
            val activities = app.database.activities().getAll()
            val name = activities.firstOrNull { it.id == open.activityTypeId }?.name ?: "Activity"
            val notification = WornNotifications.ongoing(this@RemovalTimerService, name, open, System.currentTimeMillis())
            if (Build.VERSION.SDK_INT >= 34) {
                ServiceCompat.startForeground(
                    this@RemovalTimerService,
                    WornNotifications.ID_ONGOING,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(WornNotifications.ID_ONGOING, notification)
            }
            WornNotifications.scheduleExpiry(this@RemovalTimerService, open)
            ticker?.cancel()
            ticker = launch {
                while (isActive) {
                    val current = app.repository.openSession()
                    if (current == null || current.kind != SessionKind.REMOVAL) {
                        stopSelf()
                        break
                    }
                    val n = activities.firstOrNull { it.id == current.activityTypeId }?.name ?: "Activity"
                    val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                    manager.notify(
                        WornNotifications.ID_ONGOING,
                        WornNotifications.ongoing(this@RemovalTimerService, n, current, System.currentTimeMillis()),
                    )
                    val snap = ActivityTimerCalculator.snapshot(current, System.currentTimeMillis())
                    if (snap.overdue && !snap.paused) {
                        manager.notify(
                            WornNotifications.ID_TIMER_DONE,
                            WornNotifications.timerDone(this@RemovalTimerService, n),
                        )
                    }
                    delay(1_000)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        ticker?.cancel()
        super.onDestroy()
    }

    companion object {
        fun sync(context: Context) {
            val app = context.applicationContext as WornApp
            CoroutineScope(Dispatchers.IO).launch {
                val open = app.repository.openSession()
                if (open != null && open.kind == SessionKind.REMOVAL) {
                    WornNotifications.scheduleExpiry(context, open)
                    val intent = Intent(context, RemovalTimerService::class.java)
                    if (Build.VERSION.SDK_INT >= 26) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                } else {
                    WornNotifications.cancelExpiry(context)
                    context.stopService(Intent(context, RemovalTimerService::class.java))
                }
            }
        }
    }
}
