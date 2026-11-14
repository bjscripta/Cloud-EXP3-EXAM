package com.pedidos360.pedidosdomain.analitica;

import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventoAnaliticoRepository extends JpaRepository<EventoAnalitico, String> {
    boolean existsByTipoAndNegocioId(String tipo, long negocioId);
    long countByTipo(String tipo);
    @Query("select coalesce(sum(e.monto), 0) from EventoAnalitico e where e.tipo = 'VENTA'")
    BigDecimal montoFacturado();
}
