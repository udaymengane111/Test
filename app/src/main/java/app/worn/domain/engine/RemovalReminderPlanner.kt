package app.worn.domain.engine

/**
 * Schedules short audible removal reminders from timestamps.
 * First alert at timer expiry, then every [INTERVAL_MS] until put-back or mute.
 */
object RemovalReminderPlanner {
    const val INTERVAL_MS = 5 * 60_000L

    fun expiryMillis(sessionStart: Long, configuredMillis: Long, pauseAccumulatedMillis: Long): Long {
        return sessionStart + configuredMillis + pauseAccumulatedMillis
    }

    fun alertIndex(expiryMillis: Long, nowMillis: Long): Long {
        if (nowMillis < expiryMillis) return -1L
        return (nowMillis - expiryMillis) / INTERVAL_MS
    }

    fun shouldPostAlert(
        expiryMillis: Long?,
        nowMillis: Long,
        muted: Boolean,
        paused: Boolean,
        sessionOpen: Boolean,
        lastPostedIndex: Long?,
        repeatsEnabled: Boolean = true,
        firstAlertEnabled: Boolean = true,
    ): Boolean {
        if (!sessionOpen || muted || paused || expiryMillis == null) return false
        val index = alertIndex(expiryMillis, nowMillis)
        if (index < 0L) return false
        if (index == 0L && !firstAlertEnabled) return false
        if (index > 0L && !repeatsEnabled) return false
        return lastPostedIndex != index
    }

    fun nextTriggerMillis(
        expiryMillis: Long?,
        nowMillis: Long,
        muted: Boolean,
        paused: Boolean,
        sessionOpen: Boolean,
        repeatsEnabled: Boolean = true,
        firstAlertEnabled: Boolean = true,
    ): Long? {
        if (!sessionOpen || muted || paused || expiryMillis == null) return null
        if (nowMillis < expiryMillis) {
            return if (firstAlertEnabled) expiryMillis else null
        }
        if (!repeatsEnabled) return null
        val index = alertIndex(expiryMillis, nowMillis)
        return expiryMillis + (index + 1L) * INTERVAL_MS
    }

    fun stopsAfterPutBack(sessionOpen: Boolean): Boolean = !sessionOpen

    fun muteStopsSoundOnly(muted: Boolean, sessionStillOpen: Boolean): Boolean = muted && sessionStillOpen
}
