package com.pedidos360.pedidosdomain.pedidos.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.messaging.PedidoCreadoEvent;
import com.pedidos360.messaging.RabbitTopology;
import com.pedidos360.pedidosdomain.pedidos.model.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PedidoEventPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(PedidoEventPublisher.class);

    private final PedidoRabbitOutboxRepository outbox;
    private final ObjectMapper objectMapper;

    public PedidoEventPublisher(
            PedidoRabbitOutboxRepository outbox,
            ObjectMapper objectMapper) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
    }

    public void publicarPedidoCreado(Pedido pedido) {
        PedidoCreadoEvent evento = new PedidoCreadoEvent(
                pedido.getId(),
                pedido.getClienteId(),
                pedido.getTotal(),
                pedido.getDireccionEntrega(),
                pedido.getFechaCreacion());

        try {
            String payload = objectMapper.writeValueAsString(evento);
            PedidoRabbitOutbox pending = new PedidoRabbitOutbox();
            pending.setPayload(payload);
            pending.setNextAttemptAt(java.time.Instant.now());
            outbox.save(pending);
            log.info("Evento pedido.creado registrado en outbox para pedidoId={}",
                    pedido.getId());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "No fue posible serializar el evento pedido.creado", ex);
        }
    }
}
