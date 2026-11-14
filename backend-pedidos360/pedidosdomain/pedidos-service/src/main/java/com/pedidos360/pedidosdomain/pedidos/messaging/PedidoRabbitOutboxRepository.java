package com.pedidos360.pedidosdomain.pedidos.messaging;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
public interface PedidoRabbitOutboxRepository extends JpaRepository<PedidoRabbitOutbox,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PedidoRabbitOutbox> findFirstBySentFalseOrderByIdAsc();
}
