package com.pedidos360.infraestructuredomain.bff.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class KafkaPlatformClient {
    private final WebClient analytics;
    private final WebClient admin;
    private final WebClient rabbit;

    public KafkaPlatformClient(
            @Value("${app.services.analitica-url}") String analyticsUrl,
            @Value("${app.services.kafka-admin-url}") String adminUrl,
            @Value("${app.services.rabbit-admin-url}") String rabbitUrl) {
        // Direcciones explícitas: estos servicios internos no usan Eureka.
        this.analytics = WebClient.builder().baseUrl(analyticsUrl).build();
        this.admin = WebClient.builder().baseUrl(adminUrl).build();
        this.rabbit = WebClient.builder().baseUrl(rabbitUrl).build();
    }

    public ResponseEntity<String> resumen() {
        return exchange(analytics, HttpMethod.GET, null, "api", "analitica", "resumen");
    }

    public ResponseEntity<String> kafka(HttpMethod method, JsonNode body, String... segments) {
        return exchange(admin, method, body, segments);
    }

    public ResponseEntity<String> rabbit(HttpMethod method, JsonNode body, String... segments) {
        return exchange(rabbit, method, body, segments);
    }

    private ResponseEntity<String> exchange(WebClient client, HttpMethod method,
            JsonNode body, String... segments) {
        var request = client.method(method).uri(builder -> builder.pathSegment(segments).build());
        if (body != null) { request.contentType(MediaType.APPLICATION_JSON).bodyValue(body); }
        try {
            return request.exchangeToMono(response -> response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .map(payload -> ResponseEntity.status(response.statusCode())
                            .contentType(MediaType.APPLICATION_JSON).body(payload)))
                    .block(Duration.ofSeconds(35));
        } catch (WebClientRequestException | IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El servicio interno no está disponible", ex);
        }
    }
}
