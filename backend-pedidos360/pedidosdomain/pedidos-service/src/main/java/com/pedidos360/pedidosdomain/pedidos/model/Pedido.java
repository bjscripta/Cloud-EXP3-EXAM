package com.pedidos360.pedidosdomain.pedidos.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import jakarta.validation.constraints.*;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    @NotNull @Positive private Long clienteId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado = Estado.PENDIENTE;

    @Column(nullable = false)
    private float total = 0;

    @Column(nullable = false)
    @NotBlank private String direccionEntrega;

    @jakarta.validation.Valid
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="pedido_lineas", joinColumns=@JoinColumn(name="pedido_id"))
    @OrderColumn(name="posicion")
    private java.util.List<LineaPedido> lineas = new java.util.ArrayList<>();

    @Column(unique=true)
    @Size(max=100)
    private String referenciaExterna;

    @PrePersist
    private void asignarFechaCreacion() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    public enum Estado {
        PENDIENTE,
        CONFIRMADO,
        ENVIADO,
        ENTREGADO,
        CANCELADO
    }
}