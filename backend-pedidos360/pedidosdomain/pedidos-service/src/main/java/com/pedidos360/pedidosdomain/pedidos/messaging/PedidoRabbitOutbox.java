package com.pedidos360.pedidosdomain.pedidos.messaging;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
@Entity @Table(name="rabbit_outbox") @Getter @Setter @NoArgsConstructor
public class PedidoRabbitOutbox {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Lob @Column(nullable=false) private String payload;
    private boolean sent;
    private int attempts;
    private Instant nextAttemptAt;
}
