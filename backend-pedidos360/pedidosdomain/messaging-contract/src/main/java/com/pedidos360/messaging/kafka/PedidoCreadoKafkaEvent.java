package com.pedidos360.messaging.kafka;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/** Evento analítico de creación de pedido. Clave Kafka: pedidoId como String. */
public record PedidoCreadoKafkaEvent(
        @NotNull UUID eventId,
        @Min(1) @Max(1) int schemaVersion,
        @NotNull Instant occurredAt,
        @Positive long pedidoId,
        @Positive long clienteId,
        @NotNull @DecimalMin("0.0") BigDecimal total,
        @NotBlank String estado,
        @NotNull LocalDateTime fechaCreacion) {
}
