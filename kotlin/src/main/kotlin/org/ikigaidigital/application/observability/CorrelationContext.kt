package org.ikigaidigital.application.observability

/**
 * Shared logging context keys used across adapters and application services.
 */
object CorrelationContext {
    const val MDC_KEY = "correlationId"
}
