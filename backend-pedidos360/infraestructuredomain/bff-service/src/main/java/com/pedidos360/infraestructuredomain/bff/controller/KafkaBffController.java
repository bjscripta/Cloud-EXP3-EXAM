package com.pedidos360.infraestructuredomain.bff.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.pedidos360.infraestructuredomain.bff.client.KafkaPlatformClient;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kafka")
public class KafkaBffController {
    private final KafkaPlatformClient client;
    public KafkaBffController(KafkaPlatformClient client) { this.client = client; }
    @GetMapping("/topics")
    public ResponseEntity<String> topics() { return client.kafka(HttpMethod.GET, null, "api", "kafka", "topics"); }
    @GetMapping("/topics/{name}")
    public ResponseEntity<String> topic(@PathVariable("name") String name) {
        return client.kafka(HttpMethod.GET, null, "api", "kafka", "topics", name);
    }
    @PostMapping("/topics")
    public ResponseEntity<String> create(@RequestBody JsonNode body) {
        return client.kafka(HttpMethod.POST, body, "api", "kafka", "topics");
    }
    @PatchMapping("/topics/{name}/configs")
    public ResponseEntity<String> configs(@PathVariable("name") String name, @RequestBody JsonNode body) {
        return client.kafka(HttpMethod.PATCH, body, "api", "kafka", "topics", name, "configs");
    }
    @PatchMapping("/topics/{name}/partitions")
    public ResponseEntity<String> partitions(@PathVariable("name") String name, @RequestBody JsonNode body) {
        return client.kafka(HttpMethod.PATCH, body, "api", "kafka", "topics", name, "partitions");
    }
    @DeleteMapping("/topics/{name}")
    public ResponseEntity<String> delete(@PathVariable("name") String name) {
        return client.kafka(HttpMethod.DELETE, null, "api", "kafka", "topics", name);
    }
    @GetMapping("/groups")
    public ResponseEntity<String> groups() { return client.kafka(HttpMethod.GET, null, "api", "kafka", "groups"); }
    @GetMapping("/groups/{id}")
    public ResponseEntity<String> group(@PathVariable("id") String id) {
        return client.kafka(HttpMethod.GET, null, "api", "kafka", "groups", id);
    }
}
