package org.ikigaidigital.application.observability

class OperationTimer private constructor(
    private val startedAtNanos: Long
) {
    fun elapsedMs(): Long =
        (System.nanoTime() - startedAtNanos) / NANOS_PER_MILLISECOND

    companion object {
        private const val NANOS_PER_MILLISECOND = 1_000_000L

        fun start(): OperationTimer =
            OperationTimer(System.nanoTime())
    }
}
