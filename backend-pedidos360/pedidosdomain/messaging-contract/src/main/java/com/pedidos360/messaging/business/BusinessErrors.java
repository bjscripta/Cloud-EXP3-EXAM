package com.pedidos360.messaging.business;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.ConstraintViolationException;
@RestControllerAdvice
public class BusinessErrors {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> reference(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", ex.getReason() == null ? "Operación rechazada" : ex.getReason()));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String,String>> invalid(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("message", "Datos inválidos: revisa campos obligatorios, importes y estados"));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String,String>> conflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(409).body(Map.of("message", "Ya existe un registro con esos datos o tiene relaciones activas"));
    }
}
