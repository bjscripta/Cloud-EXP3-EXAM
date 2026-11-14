package com.pedidos360.messaging.kafka;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento analítico de factura registrada; no implica que la venta esté pagada.
 * Clave Kafka: pedidoId como String, para ordenar los eventos del mismo pedido.
 */
public record VentaRegistradaKafkaEvent(
        @NotNull UUID eventId,
        @Min(1) @Max(1) int schemaVersion,
        @NotNull Instant occurredAt,
        @Positive long facturaId,
        @Positive long pedidoId,
        @Positive long clienteId,
        @NotNull @DecimalMin("0.0") BigDecimal montoTotal,
        @NotBlank String estadoPago) {
}
