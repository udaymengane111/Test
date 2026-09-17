package app.worn.domain.engine

fun interface InstantClock {
    fun nowMillis(): Long
}

object SystemClock : InstantClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
