package org.ikigaidigital.application.observability

/**
 * Measures the elapsed time of an operation.
 */
class OperationTimer private constructor(
    private val startedAtNanos: Long
) {
    /**
     * Returns the elapsed time in milliseconds since the timer was started.
     */
    fun elapsedMs(): Long =
        (System.nanoTime() - startedAtNanos) / NANOS_PER_MILLISECOND

    companion object {
        private const val NANOS_PER_MILLISECOND = 1_000_000L

        fun start(): OperationTimer =
            OperationTimer(System.nanoTime())
    }
}
