package com.pedidos360.infraestructuredomain.bff.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
/**
 *
 * @author bjcan
 */
@Getter
@Setter
@NoArgsConstructor
public class PedidoDTO {

    private int id;
    private Long clienteId;
    private LocalDateTime fechaCreacion;
    private Estado estado = Estado.PENDIENTE;
    private float total;
    private String direccionEntrega;

    private java.util.List<LineaDTO> lineas = new java.util.ArrayList<>();
    private String referenciaExterna;
    @Getter @Setter @NoArgsConstructor
    public static class LineaDTO {
        private Long productoId;
        private int cantidad;
        private String nombreProducto;
        private Float precioUnitario;
    }

    public enum Estado {
        PENDIENTE,
        CONFIRMADO,
        ENVIADO,
        ENTREGADO,
        CANCELADO
    }
}