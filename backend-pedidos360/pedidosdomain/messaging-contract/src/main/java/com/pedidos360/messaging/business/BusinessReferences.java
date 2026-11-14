package com.pedidos360.messaging.business;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

public class BusinessReferences {
    private final Environment env;
    private final RestClient client;
    private final Map<String, Integer> ports = Map.of("clientes",8081,"productos",8082,
            "inventarios",8083,"pedidos",8084,"facturas",8085,"envios",8086);
    public BusinessReferences(Environment env) {
        this.env = env;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.client = RestClient.builder().requestFactory(factory).build();
    }
    public JsonNode get(String resource, long id) { return read(resource, "/" + id); }
    public JsonNode list(String resource) { return read(resource, ""); }
    private JsonNode read(String resource, String suffix) {
        String base = env.getProperty("app.services." + resource + "-url", "http://127.0.0.1:" + ports.get(resource));
        try {
            JsonNode result = client.get().uri(base + "/" + resource + suffix).retrieve().body(JsonNode.class);
            if (result == null || result.isNull()) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Respuesta vacía de " + resource);
            return result;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "No existe el registro relacionado de " + resource);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo validar la relación con " + resource);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Servicio de " + resource + " no disponible");
        }
    }
    public void noReferences(String resource, String field, long id) {
        for (JsonNode row : list(resource)) {
            if (row.path(field).asLong(-1) == id) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar: existen registros relacionados en " + resource);
        }
    }
    public void noProductOrders(long id) {
        for (JsonNode order : list("pedidos")) for (JsonNode line : order.path("lineas")) {
            if (line.path("productoId").asLong() == id) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El producto tiene pedidos; desactívalo para conservar el historial");
        }
    }
}
