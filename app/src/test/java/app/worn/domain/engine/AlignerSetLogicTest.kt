package app.worn.domain.engine

import app.worn.domain.model.AlignerSet
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class AlignerSetLogicTest {

    @Test
    fun startingNewSetClosesPreviousOnTheDayBefore() {
        val previous = AlignerSet("1", 8, LocalDate.of(2026, 9, 12), null, "")
        val newStart = LocalDate.of(2026, 9, 19)
        val closedEnd = if (newStart.isAfter(previous.startDate)) newStart.minusDays(1) else newStart
        assertThat(closedEnd).isEqualTo(LocalDate.of(2026, 9, 18))
        val next = AlignerSet("2", 9, newStart, null, "")
        assertThat(next.setNumber).isEqualTo(previous.setNumber + 1)
        assertThat(next.endDate).isNull()
    }
}
