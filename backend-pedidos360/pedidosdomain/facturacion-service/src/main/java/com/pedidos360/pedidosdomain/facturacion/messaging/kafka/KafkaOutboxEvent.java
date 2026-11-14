package com.pedidos360.pedidosdomain.facturacion.messaging.kafka;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "kafka_outbox", indexes = @Index(name = "idx_outbox_pending", columnList = "sent,id"))
@Getter
@Setter
@NoArgsConstructor
public class KafkaOutboxEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String eventId;
    @Column(nullable = false)
    private String topic;
    @Column(nullable = false)
    private String messageKey;
    @Lob @Column(nullable = false)
    private String payload;
    @Column(nullable = false)
    private boolean sent;
    private Instant sentAt;
    @Column(nullable = false)
    private int attempts;
    @Column(nullable = false)
    private Instant nextAttemptAt;
}
