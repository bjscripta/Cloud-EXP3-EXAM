package com.pedidos360.infrastructure.rabbitadmin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.pedidos360.infrastructure.rabbitadmin.service.RabbitResourceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/rabbit-admin")
public class RabbitResourceController {
    private final RabbitResourceService service;
    public RabbitResourceController(RabbitResourceService service){this.service=service;}
    public record QueueRequest(@NotBlank String name,boolean durable,String deadLetterExchange,@Size(max=255) String deadLetterRoutingKey){}
    public record ExchangeRequest(@NotBlank String name,@NotBlank String type,boolean durable){}
    public record BindingRequest(@NotBlank String source,@NotBlank String queue,@NotNull @Size(max=255) String routingKey){}
    public record RemoveBinding(@NotBlank String source,@NotBlank String queue,@NotBlank String propertiesKey){}
    public record PublishRequest(@NotBlank String exchange,@NotNull @Size(max=255) String routingKey,@NotNull @Size(max=10000) String payload){}
    @GetMapping("/resources") public Map<String,JsonNode> resources(){return service.resources();}
    @PostMapping("/queues") @ResponseStatus(HttpStatus.CREATED)
    public void queue(@Valid @RequestBody QueueRequest request){service.createQueue(request.name(),request.durable(),request.deadLetterExchange(),request.deadLetterRoutingKey());}
    @DeleteMapping("/queues/{name}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQueue(@PathVariable("name") String name){service.deleteQueue(name);}
    @PostMapping("/exchanges") @ResponseStatus(HttpStatus.CREATED)
    public void exchange(@Valid @RequestBody ExchangeRequest request){service.createExchange(request.name(),request.type(),request.durable());}
    @DeleteMapping("/exchanges/{name}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExchange(@PathVariable("name") String name){service.deleteExchange(name);}
    @PostMapping("/bindings") @ResponseStatus(HttpStatus.CREATED)
    public void bind(@Valid @RequestBody BindingRequest request){service.bind(request.source(),request.queue(),request.routingKey());}
    @DeleteMapping("/bindings") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unbind(@Valid @RequestBody RemoveBinding request){service.unbind(request.source(),request.queue(),request.propertiesKey());}
    @PostMapping("/messages") public JsonNode publish(@Valid @RequestBody PublishRequest request){return service.publish(request.exchange(),request.routingKey(),request.payload());}
    @DeleteMapping("/queues/{name}/messages") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void purge(@PathVariable("name") String name){service.purgeTestQueue(name);}
}
