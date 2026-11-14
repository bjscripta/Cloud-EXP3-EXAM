package com.pedidos360.pedidosdomain.analitica;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnaliticaService {
    private final EventoAnaliticoRepository repository;
    public AnaliticaService(EventoAnaliticoRepository repository) { this.repository = repository; }

    @Transactional
    public void registrar(EventoAnalitico event) {
        BigDecimal monto = event.getMonto().stripTrailingZeros();
        if (monto.scale() > 8 || monto.precision() - monto.scale() > 30) {
            throw new IllegalArgumentException("Importe fuera de precisión soportada (30 enteros, 8 decimales)");
        }
        if (duplicado(event)) { return; }
        repository.saveAndFlush(event);
    }

    public boolean duplicado(EventoAnalitico event) {
        return repository.existsById(event.getEventId())
                || repository.existsByTipoAndNegocioId(event.getTipo(), event.getNegocioId());
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Resumen resumen() {
        return new Resumen(repository.countByTipo("PEDIDO"), repository.countByTipo("VENTA"),
                repository.montoFacturado());
    }

    public record Resumen(long pedidosCreados, long facturasRegistradas, BigDecimal montoFacturado) {}
}
