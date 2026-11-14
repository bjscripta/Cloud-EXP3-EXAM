package com.pedidos360.pedidosdomain.productos.model;

import jakarta.persistence.*;
import lombok.Getter;
import jakarta.validation.constraints.*;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank @Size(max=150) private String nombre;

    @Column(length = 1000)
    private String descripcion;

    @Column(nullable = false)
    @NotNull @DecimalMin("0.0") private Float precio;

    @Column(nullable = false)
    @NotNull private Boolean activo = true;
}