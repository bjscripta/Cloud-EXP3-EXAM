package com.pedidos360.pedidosdomain.analitica;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.messaging.kafka.PedidoCreadoKafkaEvent;
import com.pedidos360.messaging.kafka.VentaRegistradaKafkaEvent;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EventosKafkaConsumer {
    private static final Logger log = LoggerFactory.getLogger(EventosKafkaConsumer.class);
    private final ObjectMapper mapper;
    private final Validator validator;
    private final AnaliticaService service;
    public EventosKafkaConsumer(ObjectMapper mapper, Validator validator, AnaliticaService service) {
        this.mapper = mapper;
        this.validator = validator;
        this.service = service;
    }

    @KafkaListener(topics = "${app.kafka.topics.pedidos}")
    public void pedido(ConsumerRecord<String, String> record) {
        var event = decode(record.value(), PedidoCreadoKafkaEvent.class);
        validarClave(record.key(), event.pedidoId());
        persistir(new EventoAnalitico(event.eventId().toString(), "PEDIDO", event.pedidoId(),
                event.pedidoId(), event.clienteId(), event.total(), event.estado(), event.occurredAt()));
    }

    @KafkaListener(topics = "${app.kafka.topics.ventas}")
    public void venta(ConsumerRecord<String, String> record) {
        var event = decode(record.value(), VentaRegistradaKafkaEvent.class);
        validarClave(record.key(), event.pedidoId());
        persistir(new EventoAnalitico(event.eventId().toString(), "VENTA", event.facturaId(),
                event.pedidoId(), event.clienteId(), event.montoTotal(), event.estadoPago(), event.occurredAt()));
    }

    private <T> T decode(String payload, Class<T> type) {
        if (payload == null) { throw new IllegalArgumentException("Payload nulo"); }
        try {
            T event = mapper.readValue(payload, type);
            if (event == null) { throw new IllegalArgumentException("Evento nulo"); }
            var violations = validator.validate(event);
            if (!violations.isEmpty()) { throw new ConstraintViolationException(violations); }
            return event;
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("JSON inválido para " + type.getSimpleName(), ex);
        }
    }

    private void validarClave(String key, long pedidoId) {
        if (!Long.toString(pedidoId).equals(key)) {
            throw new IllegalArgumentException("La clave Kafka debe ser pedidoId");
        }
    }

    private void persistir(EventoAnalitico event) {
        try {
            service.registrar(event);
        } catch (DataIntegrityViolationException ex) {
            // La transacción fallida ya terminó; consultar fuera de ella.
            if (!service.duplicado(event)) { throw ex; }
        }
        log.info("Evento analítico procesado eventId={} tipo={}", event.getEventId(), event.getTipo());
    }
}
