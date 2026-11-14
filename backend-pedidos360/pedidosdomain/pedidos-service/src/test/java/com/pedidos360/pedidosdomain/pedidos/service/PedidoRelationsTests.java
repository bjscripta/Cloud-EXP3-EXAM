package com.pedidos360.pedidosdomain.pedidos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.messaging.business.BusinessReferences;
import com.pedidos360.pedidosdomain.pedidos.messaging.PedidoEventPublisher;
import com.pedidos360.pedidosdomain.pedidos.messaging.kafka.PedidoKafkaPublisher;
import com.pedidos360.pedidosdomain.pedidos.model.*;
import com.pedidos360.pedidosdomain.pedidos.repository.PedidoRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PedidoRelationsTests {
    @Test void computesTotalFromCatalogInsteadOfClientPrice() throws Exception {
        var references=mock(BusinessReferences.class);
        var repository=mock(PedidoRepository.class);
        var rabbit=mock(PedidoEventPublisher.class);
        var kafka=mock(PedidoKafkaPublisher.class);
        var service=new PedidoService();
        ReflectionTestUtils.setField(service,"references",references);
        ReflectionTestUtils.setField(service,"pedidoRepository",repository);
        ReflectionTestUtils.setField(service,"pedidoEventPublisher",rabbit);
        ReflectionTestUtils.setField(service,"pedidoKafkaPublisher",kafka);
        when(references.get("productos",7)).thenReturn(new ObjectMapper().readTree("{\"nombre\":\"Catálogo\",\"precio\":1200,\"activo\":true}"));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Pedido pedido=new Pedido();pedido.setClienteId(2L);pedido.setTotal(1);
        LineaPedido linea=new LineaPedido();linea.setProductoId(7L);linea.setCantidad(3);linea.setPrecioUnitario(1f);
        pedido.setLineas(new ArrayList<>(List.of(linea)));
        var saved=service.savePedido(pedido);
        assertEquals(3600f,saved.getTotal());
        assertEquals(1200f,saved.getLineas().get(0).getPrecioUnitario());
        verify(references).get("clientes",2);
        verify(rabbit).publicarPedidoCreado(saved);verify(kafka).publish(saved);
    }
    @Test void missingClientPreventsSavingOrPublishing() {
        var references=mock(BusinessReferences.class);var repository=mock(PedidoRepository.class);
        var service=new PedidoService();
        ReflectionTestUtils.setField(service,"references",references);ReflectionTestUtils.setField(service,"pedidoRepository",repository);
        when(references.get("clientes",99)).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,"Cliente inexistente"));
        Pedido pedido=new Pedido();pedido.setClienteId(99L);
        assertThrows(ResponseStatusException.class,()->service.savePedido(pedido));
        verifyNoInteractions(repository);
    }
}
