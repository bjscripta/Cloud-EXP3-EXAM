package com.pedidos360.pedidosdomain.pedidos.messaging.kafka;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface KafkaOutboxRepository extends JpaRepository<KafkaOutboxEvent, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<KafkaOutboxEvent> findFirstBySentFalseOrderByIdAsc();
}
