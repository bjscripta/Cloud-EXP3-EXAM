package com.pedidos360.pedidosdomain.pedidos.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
@Embeddable @Getter @Setter @NoArgsConstructor
public class LineaPedido {
    @NotNull @Positive private Long productoId;
    @Min(1) @Max(10000) private int cantidad = 1;
    private String nombreProducto;
    private Float precioUnitario;
}
