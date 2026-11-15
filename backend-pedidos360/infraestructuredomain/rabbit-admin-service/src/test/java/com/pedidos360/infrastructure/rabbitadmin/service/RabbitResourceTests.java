package com.pedidos360.infrastructure.rabbitadmin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.messaging.RabbitTopology;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class RabbitResourceTests {
    @Test void businessResourcesCannotBeDeletedOrRebound() {
        var service=new RabbitResourceService(new ObjectMapper(),"http://127.0.0.1:1","admin","test","/");
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.deleteQueue(RabbitTopology.FACTURACION_QUEUE)).getStatusCode().value());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.deleteExchange(RabbitTopology.EVENTS_EXCHANGE)).getStatusCode().value());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.bind("demo.exchange",RabbitTopology.ENVIOS_QUEUE,"demo")).getStatusCode().value());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.publish(RabbitTopology.EVENTS_EXCHANGE,"pedido.creado","invalid")).getStatusCode().value());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->service.deleteQueue("../otra-cola")).getStatusCode().value());
    }
    @Test void vhostAndBindingKeyAreEncodedWithoutChangingTheirMeaning() throws Exception {
        List<String> paths=new ArrayList<>();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{paths.add(exchange.getRequestURI().toASCIIString());exchange.sendResponseHeaders(204,-1);exchange.close();});
        server.start();
        try {
            var service=new RabbitResourceService(new ObjectMapper(),"http://127.0.0.1:"+server.getAddress().getPort(),"admin","test","/");
            service.unbind("demo.exchange","demo.queue","orders/a#b");
            assertEquals(1,paths.size());
            assertTrue(paths.get(0).contains("/bindings/%2F/"),paths.toString());
            assertTrue(paths.get(0).endsWith("orders%2Fa%23b"),paths.toString());
        } finally {server.stop(0);}
    }
    @Test void brokerCredentialsAreNotExposedToTheBrowser() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{exchange.sendResponseHeaders(401,-1);exchange.close();});server.start();
        try {
            var service=new RabbitResourceService(new ObjectMapper(),"http://127.0.0.1:"+server.getAddress().getPort(),"admin","private-test","/");
            var error=assertThrows(ResponseStatusException.class,service::resources);
            assertEquals(503,error.getStatusCode().value());
            assertFalse(error.getReason().contains("private-test"));
        } finally {server.stop(0);}
    }
}
