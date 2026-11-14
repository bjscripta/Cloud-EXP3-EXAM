package com.pedidos360.pedidosdomain.pedidos.messaging.kafka;

import com.pedidos360.messaging.kafka.PedidoCreadoKafkaEvent;
import com.pedidos360.pedidosdomain.pedidos.model.Pedido;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PedidoKafkaPublisher {
    private final KafkaOutboxWriter writer;
    private final String topic;

    public PedidoKafkaPublisher(KafkaOutboxWriter writer,
            @Value("${app.kafka.topics.pedidos}") String topic) {
        this.writer = writer;
        this.topic = topic;
    }

    public void publish(Pedido pedido) {
        var event = new PedidoCreadoKafkaEvent(UUID.randomUUID(), 1, Instant.now(),
                pedido.getId(), pedido.getClienteId(),
                new BigDecimal(Float.toString(pedido.getTotal())),
                pedido.getEstado().name(), pedido.getFechaCreacion());
        writer.enqueue(event.eventId(), topic, Long.toString(event.pedidoId()), event);
    }
}
