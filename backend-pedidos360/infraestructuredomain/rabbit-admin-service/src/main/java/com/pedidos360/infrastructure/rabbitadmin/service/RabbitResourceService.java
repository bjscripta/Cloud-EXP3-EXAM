package com.pedidos360.infrastructure.rabbitadmin.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.pedidos360.messaging.RabbitTopology;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RabbitResourceService {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(RabbitResourceService.class);
    private final RestClient client;
    private final ObjectMapper mapper;
    private final String vhost;
    private final Set<String> businessQueues=Set.of(RabbitTopology.FACTURACION_QUEUE,RabbitTopology.FACTURACION_DLQ,RabbitTopology.ENVIOS_QUEUE,RabbitTopology.ENVIOS_DLQ);
    private final Set<String> businessExchanges=Set.of(RabbitTopology.EVENTS_EXCHANGE,RabbitTopology.DEAD_LETTER_EXCHANGE);
    public RabbitResourceService(ObjectMapper mapper,
            @Value("${app.rabbit.management-url}") String url,
            @Value("${spring.rabbitmq.username}") String username,
            @Value("${spring.rabbitmq.password}") String password,
            @Value("${spring.rabbitmq.virtual-host:/}") String vhost) {
        this.mapper=mapper;this.vhost=vhost;
        var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.client=RestClient.builder().baseUrl(url).requestFactory(factory)
                .defaultHeaders(headers->headers.setBasicAuth(username,password)).build();
    }
    public boolean protectedQueue(String name){return businessQueues.contains(name) || name.startsWith("amq.");}
    public boolean protectedExchange(String name){return name.isBlank() || name.startsWith("amq.") || businessExchanges.contains(name);}
    private void queueName(String name){name(name);if(protectedQueue(name)) conflict();}
    private void exchangeName(String name){name(name);if(protectedExchange(name)) conflict();}
    private void name(String name){
        if(name==null || !name.matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,127}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Nombre inválido: usa letras, números, punto, guion o guion bajo, hasta 128 caracteres");
    }
    private void conflict(){throw new ResponseStatusException(HttpStatus.CONFLICT,"Los recursos del flujo de pedidos y los recursos internos están protegidos");}
    public Map<String,JsonNode> resources(){
        JsonNode queues=call(HttpMethod.GET,"/api/queues/{vhost}",null,vhost);
        JsonNode exchanges=call(HttpMethod.GET,"/api/exchanges/{vhost}",null,vhost);
        JsonNode allBindings=call(HttpMethod.GET,"/api/bindings/{vhost}",null,vhost);
        if(!queues.isArray() || !exchanges.isArray() || !allBindings.isArray()) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Respuesta inválida de RabbitMQ");
        ArrayNode bindings=mapper.createArrayNode();
        queues.forEach(q->((ObjectNode)q).put("protectedResource",protectedQueue(q.path("name").asText())));
        exchanges.forEach(e->((ObjectNode)e).put("protectedResource",protectedExchange(e.path("name").asText())));
        for(JsonNode binding:allBindings) if(binding.path("destination_type").asText().equals("q")) {
            ((ObjectNode)binding).put("protectedResource",protectedExchange(binding.path("source").asText()) || protectedQueue(binding.path("destination").asText()));
            bindings.add(binding);
        }
        return Map.of("queues",queues,"exchanges",exchanges,"bindings",bindings);
    }
    public void createQueue(String name,boolean durable,String dlx,String deadKey){
        queueName(name);
        Map<String,Object> arguments=new HashMap<>();
        if(dlx!=null && !dlx.isBlank()) {
            exchangeName(dlx);
            call(HttpMethod.GET,"/api/exchanges/{vhost}/{exchange}",null,vhost,dlx);
            arguments.put("x-dead-letter-exchange",dlx);
            arguments.put("x-dead-letter-routing-key",deadKey==null?"":deadKey);
        }
        call(HttpMethod.PUT,"/api/queues/{vhost}/{queue}",Map.of("durable",durable,"auto_delete",false,"arguments",arguments),vhost,name);
    }
    public void deleteQueue(String name){
        queueName(name);
        call(HttpMethod.DELETE,"/api/queues/{vhost}/{queue}?if-unused=true&if-empty=true",null,vhost,name);
    }
    public void createExchange(String name,String type,boolean durable){
        exchangeName(name);
        if(!Set.of("direct","topic","fanout").contains(type)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tipo de exchange no permitido");
        call(HttpMethod.PUT,"/api/exchanges/{vhost}/{exchange}",Map.of("type",type,"durable",durable,"auto_delete",false,"internal",false,"arguments",Map.of()),vhost,name);
    }
    public void deleteExchange(String name){
        exchangeName(name);
        call(HttpMethod.DELETE,"/api/exchanges/{vhost}/{exchange}?if-unused=true",null,vhost,name);
    }
    public void bind(String source,String queue,String routingKey){
        exchangeName(source);queueName(queue);
        call(HttpMethod.POST,"/api/bindings/{vhost}/e/{exchange}/q/{queue}",Map.of("routing_key",routingKey==null?"":routingKey,"arguments",Map.of()),vhost,source,queue);
    }
    public void unbind(String source,String queue,String propertiesKey){
        exchangeName(source);queueName(queue);
        if(propertiesKey==null || propertiesKey.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Falta la clave del binding seleccionado");
        call(HttpMethod.DELETE,"/api/bindings/{vhost}/e/{exchange}/q/{queue}/{key}",null,vhost,source,queue,propertiesKey);
    }
    public JsonNode publish(String exchange,String routingKey,String payload){
        exchangeName(exchange);
        if(payload==null || payload.length()>10000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El mensaje debe tener hasta 10000 caracteres");
        return call(HttpMethod.POST,"/api/exchanges/{vhost}/{exchange}/publish",Map.of("properties",Map.of("content_type","text/plain"),"routing_key",routingKey==null?"":routingKey,"payload",payload,"payload_encoding","string"),vhost,exchange);
    }
    public void purgeTestQueue(String name){
        queueName(name);
        call(HttpMethod.DELETE,"/api/queues/{vhost}/{queue}/contents",null,vhost,name);
    }
    private JsonNode call(HttpMethod method,String path,Object body,Object... values){
        try {
            var request=client.method(method).uri(path,values);
            if(body!=null) request.contentType(MediaType.APPLICATION_JSON).body(body);
            String result=request.retrieve().body(String.class);
            if(method!=HttpMethod.GET) log.info("Administración RabbitMQ completada method={} path={}",method,path);
            return result==null || result.isBlank()?mapper.nullNode():mapper.readTree(result);
        } catch(RestClientResponseException ex) {
            int code=ex.getStatusCode().value();
            if(code==401 || code==403) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"El administrador no pudo autenticarse en RabbitMQ");
            String reason="RabbitMQ rechazó la operación";
            try {reason=mapper.readTree(ex.getResponseBodyAsString()).path("reason").asText(reason);}catch(Exception ignored){}
            throw new ResponseStatusException(ex.getStatusCode(),reason);
        } catch(RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"La API de administración RabbitMQ no está disponible");
        } catch(com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Respuesta inválida del administrador RabbitMQ");
        }
    }
}
