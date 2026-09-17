package app.worn.notifications

import android.content.Context
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import app.worn.WornApp
import app.worn.domain.engine.ActivityTimerCalculator
import app.worn.domain.engine.RemovalReminderPlanner
import app.worn.domain.model.SessionKind
import app.worn.domain.model.TrackingSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Repeating audible reminders for an open removal session.
 * Mute stops sound only; timestamps and tracking continue.
 */
object RemovalAlerts {
    private const val PREFS = "worn_removal_alerts"
    private const val KEY_SESSION = "session_id"
    private const val KEY_MUTED = "muted"
    private const val KEY_INDEX = "last_index"

    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted

    fun isMuted(): Boolean = _muted.value

    fun bind(context: Context, session: TrackingSession, soundEnabledDefault: Boolean) {
        val prefs = prefs(context)
        val stored = prefs.getString(KEY_SESSION, null)
        if (stored != session.id) {
            prefs.edit()
                .putString(KEY_SESSION, session.id)
                .putBoolean(KEY_MUTED, false)
                .putLong(KEY_INDEX, -1L)
                .apply()
            _muted.value = false
        } else {
            _muted.value = prefs.getBoolean(KEY_MUTED, false)
        }
    }

    fun setMuted(context: Context, muted: Boolean) {
        val editor = prefs(context).edit().putBoolean(KEY_MUTED, muted)
        if (!muted) editor.putLong(KEY_INDEX, -1L)
        editor.apply()
        _muted.value = muted
        if (muted) {
            cancel(context)
            NotificationManagerCompat.from(context).cancel(WornNotifications.ID_TIMER_DONE)
        }
        CoroutineScope(Dispatchers.IO).launch { onAlarm(context) }
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
        _muted.value = false
        cancel(context)
        NotificationManagerCompat.from(context).cancel(WornNotifications.ID_TIMER_DONE)
    }

    fun cancel(context: Context) {
        val alarm = context.getSystemService(android.app.AlarmManager::class.java)
        alarm.cancel(WornNotifications.expiryPending(context))
    }

    suspend fun sync(context: Context, session: TrackingSession) {
        WornNotifications.ensureChannels(context)
        if (session.kind != SessionKind.REMOVAL || session.endMillis != null) {
            clear(context)
            return
        }
        val now = System.currentTimeMillis()
        val expiry = expiryOf(session)
        val settings = (context.applicationContext as WornApp).repository.settings()
        val muted = prefs(context).getBoolean(KEY_MUTED, false)
        _muted.value = muted
        if (expiry != null && now >= expiry && !session.isPaused) {
            maybePost(context, session, expiry, now, settings.notificationsEnabled, settings.removalReminderSoundEnabled)
        }
        val next = RemovalReminderPlanner.nextTriggerMillis(
            expiryMillis = expiry,
            nowMillis = now,
            muted = muted,
            paused = session.isPaused,
            sessionOpen = true,
            repeatsEnabled = settings.removalReminderSoundEnabled,
            firstAlertEnabled = settings.notificationsEnabled,
        )
        if (next == null) {
            cancel(context)
            return
        }
        val firstAlert = expiry != null && next == expiry
        WornNotifications.scheduleWakeup(
            context,
            next,
            WornNotifications.expiryPending(context),
            userVisibleAlarm = firstAlert,
        )
    }

    suspend fun onAlarm(context: Context) {
        val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "worn:timer_alarm")
        wakeLock.setReferenceCounted(false)
        wakeLock.acquire(10_000L)
        try {
            withContext(Dispatchers.IO) {
                val app = context.applicationContext as WornApp
                val open = app.repository.openSession()
                if (open == null || open.kind != SessionKind.REMOVAL) {
                    clear(context)
                    return@withContext
                }
                val now = System.currentTimeMillis()
                val snap = ActivityTimerCalculator.snapshot(open, now)
                if (snap.paused) {
                    cancel(context)
                    return@withContext
                }
                val expiry = expiryOf(open) ?: return@withContext
                if (now < expiry - 1_000L) {
                    sync(context, open)
                    return@withContext
                }
                maybePost(
                    context,
                    open,
                    expiry,
                    now,
                    app.repository.settings().notificationsEnabled,
                    app.repository.settings().removalReminderSoundEnabled,
                )
                sync(context, open)
            }
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    private suspend fun maybePost(
        context: Context,
        session: TrackingSession,
        expiry: Long,
        now: Long,
        firstAlertEnabled: Boolean,
        repeatsEnabled: Boolean,
    ) {
        val muted = prefs(context).getBoolean(KEY_MUTED, false)
        val last = prefs(context).getLong(KEY_INDEX, -1L)
        val should = RemovalReminderPlanner.shouldPostAlert(
            expiryMillis = expiry,
            nowMillis = now,
            muted = muted,
            paused = session.isPaused,
            sessionOpen = true,
            lastPostedIndex = last,
            repeatsEnabled = repeatsEnabled,
            firstAlertEnabled = firstAlertEnabled,
        )
        if (!should) return
        val index = RemovalReminderPlanner.alertIndex(expiry, now)
        prefs(context).edit().putLong(KEY_INDEX, index).apply()
        val app = context.applicationContext as WornApp
        val activityName = app.database.activities().get(session.activityTypeId ?: "")?.name ?: "removal"
        WornNotifications.postTimerFinished(context, activityName, muted)
    }

    private fun expiryOf(session: TrackingSession): Long? {
        return ActivityTimerCalculator.expiryEpochMillis(session)
            ?: if (session.isPaused) {
                null
            } else {
                RemovalReminderPlanner.expiryMillis(
                    session.startMillis,
                    session.configuredDurationMillis ?: 0L,
                    session.pauseAccumulatedMillis,
                )
            }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
