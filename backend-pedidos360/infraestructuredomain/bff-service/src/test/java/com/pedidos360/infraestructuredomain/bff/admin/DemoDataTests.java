package com.pedidos360.infraestructuredomain.bff.admin;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class DemoDataTests {
    @Test void capsEachServiceAtFivePreservesExistingRowsAndCanBeRepeated() throws Exception {
        var mapper=new ObjectMapper();
        Map<String,ArrayNode> rows=new LinkedHashMap<>();
        for(String resource:List.of("clientes","productos","inventarios","pedidos","facturas","envios")) rows.put(resource,mapper.createArrayNode());
        rows.get("clientes").add(mapper.readTree("{\"id\":1,\"nombre\":\"Cliente original\",\"email\":\"original@example.test\",\"direccion\":\"Dirección original\"}"));
        rows.get("productos").add(mapper.readTree("{\"id\":1,\"nombre\":\"Producto original\",\"precio\":100,\"activo\":true}"));
        rows.get("inventarios").add(mapper.readTree("{\"id\":1,\"productoId\":1,\"cantidadDisponible\":9}"));
        rows.get("pedidos").add(mapper.readTree("{\"id\":1,\"clienteId\":1,\"total\":100,\"direccionEntrega\":\"Original\",\"referenciaExterna\":\"LEGACY\"}"));
        rows.get("facturas").add(mapper.readTree("{\"id\":1,\"pedidoId\":1,\"clienteId\":1}"));
        rows.get("envios").add(mapper.readTree("{\"id\":1,\"pedidoId\":1}"));
        var writes=new AtomicInteger();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange -> {
            String resource=exchange.getRequestURI().getPath().substring(1);
            JsonNode response=rows.get(resource);
            if(exchange.getRequestMethod().equals("POST")) {
                writes.incrementAndGet();
                ObjectNode row=(ObjectNode)mapper.readTree(exchange.getRequestBody());
                row.put("id",rows.get(resource).size()+1);rows.get(resource).add(row);response=row;
            }
            byte[] bytes=mapper.writeValueAsBytes(response);
            exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });
        server.start();
        try {
            var env=new MockEnvironment().withProperty("app.demo.enabled","true");
            for(String resource:rows.keySet()) env.withProperty("app.services."+resource+"-url","http://127.0.0.1:"+server.getAddress().getPort());
            var service=new AdminDataService(env,mapper);
            service.seedDemo();
            rows.forEach((resource,list)->assertEquals(5,list.size(),resource));
            assertEquals("Cliente original",rows.get("clientes").get(0).path("nombre").asText());
            assertEquals("Producto original",rows.get("productos").get(0).path("nombre").asText());
            assertEquals(9,rows.get("inventarios").get(0).path("cantidadDisponible").asInt());
            int written=writes.get();service.seedDemo();assertEquals(written,writes.get());
            for(JsonNode order:rows.get("pedidos")) assertTrue(order.path("clienteId").asInt()>=1 && order.path("clienteId").asInt()<=5);
        } finally {server.stop(0);}
    }
}
