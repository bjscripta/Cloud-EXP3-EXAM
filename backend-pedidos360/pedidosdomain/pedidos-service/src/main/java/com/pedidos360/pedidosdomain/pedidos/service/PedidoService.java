package com.pedidos360.pedidosdomain.pedidos.service;

import com.pedidos360.pedidosdomain.pedidos.model.Pedido;
import com.pedidos360.pedidosdomain.pedidos.messaging.PedidoEventPublisher;
import com.pedidos360.pedidosdomain.pedidos.repository.PedidoRepository;
import jakarta.transaction.Transactional;
import com.pedidos360.pedidosdomain.pedidos.messaging.kafka.PedidoKafkaPublisher;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private PedidoEventPublisher pedidoEventPublisher;

    @Autowired
    private PedidoKafkaPublisher pedidoKafkaPublisher;

    @Autowired
    private com.pedidos360.messaging.business.BusinessReferences references;

    public List<Pedido> getPedidos() {
        return pedidoRepository.findAll();
    }

    public Pedido getPedidoById(int id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Pedido no encontrado"));
    }

    public List<Pedido> getPedidosByClienteId(Long clienteId) {
        return pedidoRepository.findByClienteId(clienteId);
    }

    public Pedido savePedido(Pedido pedido) {
        boolean nuevo = pedido.getId() == 0;
        if (nuevo) {
            references.get("clientes", pedido.getClienteId());
            if (pedido.getLineas() == null || pedido.getLineas().isEmpty() || pedido.getLineas().size() > 100) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pedido requiere entre 1 y 100 líneas de productos");
            }
            java.math.BigDecimal total = java.math.BigDecimal.ZERO;
            for (var linea : pedido.getLineas()) {
                if (linea.getProductoId() == null || linea.getCantidad() < 1 || linea.getCantidad() > 10000) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Producto y cantidad inválidos");
                }
                var producto = references.get("productos", linea.getProductoId());
                if (!producto.path("activo").asBoolean()) throw new ResponseStatusException(HttpStatus.CONFLICT, "El producto está inactivo");
                linea.setNombreProducto(producto.path("nombre").asText());
                linea.setPrecioUnitario((float) producto.path("precio").asDouble());
                total = total.add(new java.math.BigDecimal(Float.toString(linea.getPrecioUnitario())).multiply(java.math.BigDecimal.valueOf(linea.getCantidad())));
            }
            pedido.setTotal(total.floatValue());
        }
        Pedido guardado = pedidoRepository.save(pedido);
        if (nuevo) {
            pedidoKafkaPublisher.publish(guardado);
            pedidoEventPublisher.publicarPedidoCreado(guardado);
        }
        return guardado;
    }

    public String deletePedido(int id) {
        Pedido pedido = getPedidoById(id);
        references.noReferences("facturas", "pedidoId", id);
        references.noReferences("envios", "pedidoId", id);
        pedidoRepository.delete(pedido);
        return "Pedido eliminado";
    }
}
