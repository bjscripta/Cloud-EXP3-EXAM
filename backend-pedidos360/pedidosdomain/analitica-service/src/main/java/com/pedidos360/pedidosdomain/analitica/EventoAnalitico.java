package com.pedidos360.pedidosdomain.analitica;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.domain.Persistable;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "eventos_analiticos", uniqueConstraints =
        @UniqueConstraint(name = "uk_evento_negocio", columnNames = {"tipo", "negocio_id"}))
@Getter
@NoArgsConstructor
public class EventoAnalitico implements Persistable<String> {
    @Id
    private String eventId;
    @Column(nullable = false)
    private String tipo;
    @Column(name = "negocio_id", nullable = false)
    private long negocioId;
    @Column(nullable = false)
    private long pedidoId;
    @Column(nullable = false)
    private long clienteId;
    @Column(nullable = false, precision = 38, scale = 8)
    private BigDecimal monto;
    @Column(nullable = false)
    private String estado;
    @Column(nullable = false)
    private Instant occurredAt;

    @Override
    public String getId() { return eventId; }

    @Override
    @Transient
    public boolean isNew() { return true; }

    public EventoAnalitico(String eventId, String tipo, long negocioId, long pedidoId,
            long clienteId, BigDecimal monto, String estado, Instant occurredAt) {
        this.eventId = eventId;
        this.tipo = tipo;
        this.negocioId = negocioId;
        this.pedidoId = pedidoId;
        this.clienteId = clienteId;
        this.monto = monto;
        this.estado = estado;
        this.occurredAt = occurredAt;
    }
}
