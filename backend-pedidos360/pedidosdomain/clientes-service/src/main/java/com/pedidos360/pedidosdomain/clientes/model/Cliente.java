package com.pedidos360.pedidosdomain.clientes.model;

import jakarta.persistence.*;
import lombok.Getter;
import jakarta.validation.constraints.*;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank @Size(max=150) private String nombre;

    @Column(nullable = false, unique = true)
    @NotBlank @Email @Size(max=200) private String email;

    private String telefono;

    private String direccion;
}