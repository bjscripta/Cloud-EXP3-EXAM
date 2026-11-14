package com.pedidos360.infraestructuredomain.bff.controller;

import com.pedidos360.infraestructuredomain.bff.client.KafkaPlatformClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analitica")
public class AnaliticaBffController {
    private final KafkaPlatformClient client;
    public AnaliticaBffController(KafkaPlatformClient client) { this.client = client; }
    @GetMapping("/resumen")
    public ResponseEntity<String> resumen() { return client.resumen(); }
}
