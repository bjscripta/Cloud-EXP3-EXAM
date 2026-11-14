package com.pedidos360.pedidosdomain.facturacion.messaging.kafka;

import com.pedidos360.messaging.kafka.VentaRegistradaKafkaEvent;
import com.pedidos360.pedidosdomain.facturacion.model.Facturacion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VentaKafkaPublisher {
    private final KafkaOutboxWriter writer;
    private final String topic;

    public VentaKafkaPublisher(KafkaOutboxWriter writer,
            @Value("${app.kafka.topics.ventas}") String topic) {
        this.writer = writer;
        this.topic = topic;
    }

    public void publish(Facturacion factura) {
        var event = new VentaRegistradaKafkaEvent(UUID.randomUUID(), 1, Instant.now(),
                factura.getId(), factura.getPedidoId(), factura.getClienteId(),
                BigDecimal.valueOf(factura.getMontoTotal()), factura.getEstadoPago());
        writer.enqueue(event.eventId(), topic, Long.toString(event.pedidoId()), event);
    }
}
