package app.worn.domain.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TimerNotificationCopyTest {

    @Test
    fun teaUsesActivityName() {
        assertThat(TimerNotificationCopy.TITLE).isEqualTo("Put your aligner back")
        assertThat(TimerNotificationCopy.body("Tea"))
            .isEqualTo("Your Tea removal timer has ended. Put your aligner back in.")
    }

    @Test
    fun lunchDinnerUsesFriendlyLabel() {
        assertThat(TimerNotificationCopy.body("Lunch / Dinner"))
            .isEqualTo("Your lunch/dinner removal timer has ended. Put your aligner back in.")
    }

    @Test
    fun snackUsesActivityName() {
        assertThat(TimerNotificationCopy.body("Snack"))
            .isEqualTo("Your Snack removal timer has ended. Put your aligner back in.")
    }
}
