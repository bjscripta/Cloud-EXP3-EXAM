package com.pedidos360.infrastructure.kafkaadmin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kafka")
@Validated
public class KafkaManagementController {
    private final KafkaManagementService service;
    public KafkaManagementController(KafkaManagementService service) { this.service = service; }
    public record CreateTopic(
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9][a-zA-Z0-9._-]{0,248}") String name,
            @Min(1) @Max(1000) int partitions, @Min(1) @Max(10) short replicationFactor,
            @NotNull @Size(max = 4) Map<String, String> configs) {}
    public record TopicConfig(@NotNull @Size(min = 1, max = 4) Map<String, String> configs) {}
    public record Partitions(@Min(1) @Max(1000) int total) {}

    @GetMapping("/topics")
    public Collection<String> topics() throws Exception { return service.topics(); }
    @GetMapping("/topics/{name}")
    public Map<String, Object> topic(@PathVariable("name") @Pattern(regexp = "[a-zA-Z0-9][a-zA-Z0-9._-]{0,248}") String name) throws Exception {
        return service.topic(name);
    }
    @PostMapping("/topics") @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody CreateTopic request) throws Exception {
        service.create(request.name(), request.partitions(), request.replicationFactor(), request.configs());
    }
    @PatchMapping("/topics/{name}/configs") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void configure(@PathVariable("name") @Pattern(regexp = "[a-zA-Z0-9][a-zA-Z0-9._-]{0,248}") String name,
            @Valid @RequestBody TopicConfig request) throws Exception { service.configure(name, request.configs()); }
    @PatchMapping("/topics/{name}/partitions") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void partitions(@PathVariable("name") @Pattern(regexp = "[a-zA-Z0-9][a-zA-Z0-9._-]{0,248}") String name,
            @Valid @RequestBody Partitions request) throws Exception { service.partitions(name, request.total()); }
    @DeleteMapping("/topics/{name}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("name") @Pattern(regexp = "[a-zA-Z0-9][a-zA-Z0-9._-]{0,248}") String name) throws Exception {
        service.delete(name);
    }
    @GetMapping("/groups")
    public List<Map<String, String>> groups() throws Exception { return service.groups(); }
    @GetMapping("/groups/{id}")
    public Map<String, Object> group(@PathVariable("id") @NotBlank @Size(max = 255) String id) throws Exception { return service.group(id); }
}
