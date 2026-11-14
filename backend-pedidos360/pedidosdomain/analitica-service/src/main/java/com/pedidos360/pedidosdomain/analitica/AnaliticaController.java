package com.pedidos360.pedidosdomain.analitica;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analitica")
public class AnaliticaController {
    private final AnaliticaService service;
    public AnaliticaController(AnaliticaService service) { this.service = service; }
    @GetMapping("/resumen")
    public AnaliticaService.Resumen resumen() { return service.resumen(); }
}
