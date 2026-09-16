package app.worn.notifications

import android.content.Context
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

/**
 * Repeating 5-minute audible reminders for an open removal session.
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
                .putBoolean(KEY_MUTED, !soundEnabledDefault)
                .putLong(KEY_INDEX, -1L)
                .apply()
            _muted.value = !soundEnabledDefault
        } else {
            _muted.value = prefs.getBoolean(KEY_MUTED, false)
        }
    }

    fun setMuted(context: Context, muted: Boolean) {
        val editor = prefs(context).edit().putBoolean(KEY_MUTED, muted)
        if (!muted) editor.putLong(KEY_INDEX, -1L)
        editor.apply()
        _muted.value = muted
        val app = context.applicationContext
        if (muted) {
            cancel(context)
            NotificationManagerCompat.from(context).cancel(WornNotifications.ID_TIMER_DONE)
        }
        if (app is WornApp) {
            CoroutineScope(Dispatchers.IO).launch {
                if (!muted) {
                    onAlarm(context)
                } else {
                    val open = app.repository.openSession() ?: return@launch
                    sync(context, open)
                }
            }
        }
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

    fun sync(context: Context, session: TrackingSession) {
        WornNotifications.ensureChannels(context)
        if (session.kind != SessionKind.REMOVAL || session.endMillis != null) {
            clear(context)
            return
        }
        val now = System.currentTimeMillis()
        val expiry = ActivityTimerCalculator.expiryEpochMillis(session)
            ?: if (session.isPaused) {
                null
            } else {
                RemovalReminderPlanner.expiryMillis(
                    session.startMillis,
                    session.configuredDurationMillis ?: 0L,
                    session.pauseAccumulatedMillis,
                )
            }
        val muted = prefs(context).getBoolean(KEY_MUTED, false)
        _muted.value = muted
        val next = RemovalReminderPlanner.nextTriggerMillis(
            expiryMillis = expiry,
            nowMillis = now,
            muted = muted,
            paused = session.isPaused,
            sessionOpen = true,
        )
        if (next == null) {
            cancel(context)
            return
        }
        WornNotifications.scheduleWakeup(context, next, WornNotifications.expiryPending(context))
    }

    suspend fun onAlarm(context: Context) {
        val app = context.applicationContext as WornApp
        val open = app.repository.openSession()
        if (open == null || open.kind != SessionKind.REMOVAL) {
            clear(context)
            return
        }
        val now = System.currentTimeMillis()
        val snap = ActivityTimerCalculator.snapshot(open, now)
        if (snap.paused) {
            cancel(context)
            return
        }
        val expiry = ActivityTimerCalculator.expiryEpochMillis(open)
            ?: RemovalReminderPlanner.expiryMillis(
                open.startMillis,
                open.configuredDurationMillis ?: 0L,
                open.pauseAccumulatedMillis,
            )
        if (now < expiry - 1_000L) {
            sync(context, open)
            return
        }
        val muted = prefs(context).getBoolean(KEY_MUTED, false)
        val last = prefs(context).getLong(KEY_INDEX, -1L)
        val should = RemovalReminderPlanner.shouldPostAlert(
            expiryMillis = expiry,
            nowMillis = now,
            muted = muted,
            paused = false,
            sessionOpen = true,
            lastPostedIndex = last,
        )
        if (should && !muted) {
            val index = RemovalReminderPlanner.alertIndex(expiry, now)
            prefs(context).edit().putLong(KEY_INDEX, index).apply()
            val activity = app.database.activities().get(open.activityTypeId ?: "")
            NotificationManagerCompat.from(context).notify(
                WornNotifications.ID_TIMER_DONE,
                WornNotifications.timerDone(context, activity?.name ?: "Activity", muted = false),
            )
        }
        sync(context, open)
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
