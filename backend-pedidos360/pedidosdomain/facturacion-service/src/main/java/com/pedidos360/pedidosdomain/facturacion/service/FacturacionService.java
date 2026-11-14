package com.pedidos360.pedidosdomain.facturacion.service;

import com.pedidos360.pedidosdomain.facturacion.model.Facturacion;
import com.pedidos360.pedidosdomain.facturacion.repository.FacturacionRepository;
import jakarta.transaction.Transactional;
import com.pedidos360.messaging.PedidoCreadoEvent;
import com.pedidos360.pedidosdomain.facturacion.messaging.kafka.VentaKafkaPublisher;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class FacturacionService {

    @Autowired
    private FacturacionRepository facturacionRepository;

    @Autowired
    private VentaKafkaPublisher ventaKafkaPublisher;

    public List<Facturacion> getFacturaciones() {
        return facturacionRepository.findAll();
    }

    public Facturacion getFacturacionById(Long id) {
        return facturacionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Factura no encontrada"));
    }

    public Facturacion getFacturaByPedidoId(Long pedidoId) {
        return facturacionRepository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Factura del pedido no encontrada"));
    }

    public Facturacion saveFacturacion(Facturacion facturacion) {
        boolean nueva = facturacion.getId() == null;
        Facturacion guardada = facturacionRepository.save(facturacion);
        if (nueva) {
            ventaKafkaPublisher.publish(guardada);
        }
        return guardada;
    }

    // La llamada desde el listener atraviesa el proxy transaccional: ACK tras el commit.
    public void facturarPedido(PedidoCreadoEvent evento) {
        if (facturacionRepository.findByPedidoId(evento.pedidoId()).isPresent()) {
            return;
        }
        Facturacion factura = new Facturacion();
        factura.setPedidoId(evento.pedidoId());
        factura.setClienteId(evento.clienteId());
        factura.setMontoTotal(evento.total());
        factura.setEstadoPago("PENDIENTE");
        saveFacturacion(factura);
    }

    public String deleteFacturacion(Long id) {
        Facturacion facturacion = getFacturacionById(id);
        facturacionRepository.delete(facturacion);
        return "Factura eliminada";
    }
}