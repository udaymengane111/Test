package app.worn.domain.engine

object TimerNotificationCopy {
    const val TITLE = "Put your aligner back"

    fun body(activityName: String): String {
        val label = when {
            activityName.contains("lunch", ignoreCase = true) ||
                activityName.contains("dinner", ignoreCase = true) -> "lunch/dinner"
            else -> activityName.ifBlank { "removal" }
        }
        return "Your $label removal timer has ended. Put your aligner back in."
    }
}
