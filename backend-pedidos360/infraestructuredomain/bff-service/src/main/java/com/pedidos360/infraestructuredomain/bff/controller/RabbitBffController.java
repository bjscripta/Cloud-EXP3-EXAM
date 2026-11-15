package com.pedidos360.infraestructuredomain.bff.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.pedidos360.infraestructuredomain.bff.client.KafkaPlatformClient;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rabbit")
public class RabbitBffController {
    private final KafkaPlatformClient client;
    public RabbitBffController(KafkaPlatformClient client){this.client=client;}
    @GetMapping("/resources") public ResponseEntity<String> resources(){return client.rabbit(HttpMethod.GET,null,"rabbit-admin","resources");}
    @PostMapping("/queues") public ResponseEntity<String> queue(@RequestBody JsonNode body){return client.rabbit(HttpMethod.POST,body,"rabbit-admin","queues");}
    @DeleteMapping("/queues/{name}") public ResponseEntity<String> deleteQueue(@PathVariable("name") String name){return client.rabbit(HttpMethod.DELETE,null,"rabbit-admin","queues",name);}
    @PostMapping("/exchanges") public ResponseEntity<String> exchange(@RequestBody JsonNode body){return client.rabbit(HttpMethod.POST,body,"rabbit-admin","exchanges");}
    @DeleteMapping("/exchanges/{name}") public ResponseEntity<String> deleteExchange(@PathVariable("name") String name){return client.rabbit(HttpMethod.DELETE,null,"rabbit-admin","exchanges",name);}
    @PostMapping("/bindings") public ResponseEntity<String> bind(@RequestBody JsonNode body){return client.rabbit(HttpMethod.POST,body,"rabbit-admin","bindings");}
    @DeleteMapping("/bindings") public ResponseEntity<String> unbind(@RequestBody JsonNode body){return client.rabbit(HttpMethod.DELETE,body,"rabbit-admin","bindings");}
    @PostMapping("/messages") public ResponseEntity<String> publish(@RequestBody JsonNode body){return client.rabbit(HttpMethod.POST,body,"rabbit-admin","messages");}
    @DeleteMapping("/queues/{name}/messages") public ResponseEntity<String> purge(@PathVariable("name") String name){return client.rabbit(HttpMethod.DELETE,null,"rabbit-admin","queues",name,"messages");}
    @DeleteMapping("/dlq/{name}/messages") public ResponseEntity<String> purgeDlq(@PathVariable("name") String name){return client.rabbit(HttpMethod.DELETE,null,"rabbit-admin","dlq",name,"messages");}
}
