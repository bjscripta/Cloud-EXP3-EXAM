package com.pedidos360.pedidosdomain.pedidos.messaging.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class KafkaOutboxWriter {
    private final KafkaOutboxRepository repository;
    private final ObjectMapper mapper;
    private final Validator validator;

    public KafkaOutboxWriter(KafkaOutboxRepository repository, ObjectMapper mapper, Validator validator) {
        this.repository = repository;
        this.mapper = mapper;
        this.validator = validator;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(UUID eventId, String topic, String key, Object event) {
        var violations = validator.validate(event);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        KafkaOutboxEvent row = new KafkaOutboxEvent();
        row.setEventId(eventId.toString());
        row.setTopic(topic);
        row.setMessageKey(key);
        row.setNextAttemptAt(Instant.now());
        try {
            row.setPayload(mapper.writeValueAsString(event));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("No fue posible serializar el evento Kafka", ex);
        }
        repository.save(row);
    }
}
