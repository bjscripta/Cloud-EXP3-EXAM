package com.pedidos360.pedidosdomain.inventario.model;

import jakarta.persistence.*;
import lombok.Getter;
import jakarta.validation.constraints.*;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inventarios")
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotNull @Positive private Long productoId;

    @Column(nullable = false)
    @Min(0) private int cantidadDisponible = 0;

    @Column(nullable = false)
    @Min(0) private int stockMinimo = 0;
}