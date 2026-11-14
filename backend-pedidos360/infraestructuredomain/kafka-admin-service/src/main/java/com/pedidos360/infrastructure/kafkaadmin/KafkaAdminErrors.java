package com.pedidos360.infrastructure.kafkaadmin;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import jakarta.validation.ConstraintViolationException;
import org.apache.kafka.common.errors.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice
public class KafkaAdminErrors {
    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<Map<String, String>> invalid(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "Solicitud inválida", "type", ex.getClass().getSimpleName()));
    }
    @ExceptionHandler({ExecutionException.class, TimeoutException.class, org.apache.kafka.common.KafkaException.class})
    public ResponseEntity<Map<String, String>> kafka(Exception ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) { cause = cause.getCause(); }
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        if (cause instanceof UnknownTopicOrPartitionException || cause instanceof GroupIdNotFoundException) {
            status = HttpStatus.NOT_FOUND;
        } else if (cause instanceof TopicExistsException) {
            status = HttpStatus.CONFLICT;
        } else if (cause instanceof InvalidConfigurationException || cause instanceof InvalidPartitionsException
                || cause instanceof InvalidReplicationFactorException || cause instanceof InvalidTopicException) {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(Map.of("error", "Operación Kafka no completada",
                "type", cause.getClass().getSimpleName()));
    }
}
