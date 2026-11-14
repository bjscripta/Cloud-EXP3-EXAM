package com.pedidos360.pedidosdomain.clientes.service;

import com.pedidos360.pedidosdomain.clientes.model.Cliente;
import com.pedidos360.pedidosdomain.clientes.repository.ClienteRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private com.pedidos360.messaging.business.BusinessReferences references;

    public List<Cliente> getClientes() {
        return clienteRepository.findAll();
    }

    public Cliente getClienteById(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Cliente no encontrado"));
    }

    public Cliente saveCliente(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    public String deleteCliente(Long id) {
        Cliente cliente = getClienteById(id);
        references.noReferences("pedidos", "clienteId", id);
        references.noReferences("facturas", "clienteId", id);
        clienteRepository.delete(cliente);
        return "Cliente eliminado";
    }
}