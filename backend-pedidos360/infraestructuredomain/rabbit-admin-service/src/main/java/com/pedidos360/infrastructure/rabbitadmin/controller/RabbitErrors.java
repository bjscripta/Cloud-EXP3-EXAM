package com.pedidos360.infrastructure.rabbitadmin.controller;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.amqp.AmqpException;
@RestControllerAdvice(assignableTypes={RabbitResourceController.class,RabbitAdminController.class})
public class RabbitErrors {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> status(ResponseStatusException ex){return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message",ex.getReason()==null?"Operación rechazada":ex.getReason()));}
    @ExceptionHandler({MethodArgumentNotValidException.class,IllegalArgumentException.class})
    public ResponseEntity<Map<String,String>> invalid(Exception ex){return ResponseEntity.badRequest().body(Map.of("message","Revisa los nombres, el tipo de exchange y los campos requeridos"));}
    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<Map<String,String>> unavailable(AmqpException ex){return ResponseEntity.status(503).body(Map.of("message","RabbitMQ no está disponible"));}
}
