package com.pedidos360.pedidosdomain.envios.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import jakarta.validation.constraints.*;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "envios")
public class Envios {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotNull @Positive private Long pedidoId;

    @Column(nullable = false)
    @NotBlank private String direccionEntrega;

    @Column(nullable = false)
    @Pattern(regexp="EN_PREPARACION|DESPACHADO|ENTREGADO|CANCELADO") @NotBlank private String estadoEnvio = "EN_PREPARACION";

    @Column(nullable = false)
    @NotBlank private String numeroSeguimiento;
}