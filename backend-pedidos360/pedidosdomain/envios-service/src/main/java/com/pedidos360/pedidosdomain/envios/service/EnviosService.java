package com.pedidos360.pedidosdomain.envios.service;

import com.pedidos360.pedidosdomain.envios.model.Envios;
import com.pedidos360.pedidosdomain.envios.repository.EnviosRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class EnviosService {

    @Autowired
    private EnviosRepository enviosRepository;

    @Autowired
    private com.pedidos360.messaging.business.BusinessReferences references;

    public List<Envios> getEnvios() {
        return enviosRepository.findAll();
    }

    public Envios getEnvioById(Long id) {
        return enviosRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Envío no encontrado"));
    }

    public Envios getEnvioByPedidoId(Long pedidoId) {
        return enviosRepository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Envío del pedido no encontrado"));
    }

    public Envios saveEnvio(Envios envio) {
        references.get("pedidos", envio.getPedidoId());
        return enviosRepository.save(envio);
    }

    public void prepararPedido(com.pedidos360.messaging.PedidoCreadoEvent evento) {
        if (enviosRepository.findByPedidoId(evento.pedidoId()).isPresent()) return;
        Envios envio = new Envios();
        envio.setPedidoId(evento.pedidoId());
        envio.setDireccionEntrega(evento.direccionEntrega());
        envio.setEstadoEnvio("EN_PREPARACION");
        envio.setNumeroSeguimiento(java.util.UUID.randomUUID().toString());
        saveEnvio(envio);
    }

    public String deleteEnvio(Long id) {
        Envios envio = getEnvioById(id);
        enviosRepository.delete(envio);
        return "Envío eliminado";
    }
}