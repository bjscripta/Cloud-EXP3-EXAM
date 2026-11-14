package com.pedidos360.pedidosdomain.facturacion.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@EnableScheduling
public class KafkaOutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(KafkaOutboxDispatcher.class);
    private final KafkaOutboxRepository repository;
    private final ObjectMapper mapper;
    private final KafkaTemplate<String, Object> kafka;

    public KafkaOutboxDispatcher(KafkaOutboxRepository repository, ObjectMapper mapper,
            KafkaTemplate<String, Object> kafka) {
        this.repository = repository;
        this.mapper = mapper;
        this.kafka = kafka;
    }

    // El lock y la transacción impiden que dos instancias despachen la misma fila a la vez.
    // Se conserva FIFO: un envío fallido bloquea los siguientes hasta recuperarse.
    @Scheduled(fixedDelayString = "${app.kafka.outbox.poll-delay-ms:1000}")
    @Transactional
    public void dispatchNext() {
        repository.findFirstBySentFalseOrderByIdAsc().ifPresent(row -> {
            if (row.getNextAttemptAt().isAfter(Instant.now())) {
                return;
            }
            try {
                var result = kafka.send(row.getTopic(), row.getMessageKey(),
                        mapper.readTree(row.getPayload())).get(130, TimeUnit.SECONDS);
                row.setSent(true);
                row.setSentAt(Instant.now());
                log.info("Kafka confirmado eventId={} topic={} partition={} offset={}",
                        row.getEventId(), row.getTopic(), result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                row.setAttempts(Math.min(row.getAttempts() + 1, 30));
                long delay = Math.min(300, 1L << Math.min(row.getAttempts(), 8));
                row.setNextAttemptAt(Instant.now().plusSeconds(delay));
                Throwable cause = ex;
                while (cause.getCause() != null && cause.getCause() != cause) {
                    cause = cause.getCause();
                }
                log.warn("Kafka pendiente eventId={} topic={} attempt={} retryInSeconds={} error={}",
                        row.getEventId(), row.getTopic(), row.getAttempts(), delay,
                        cause.getClass().getSimpleName());
            }
        });
    }
}
