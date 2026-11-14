package com.pedidos360.infraestructuredomain.bff.admin;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminDataService {
    private final Environment env;
    private final ObjectMapper mapper;
    private final RestClient client;
    private final Map<String,Integer> ports=Map.of("clientes",8081,"productos",8082,"inventarios",8083,"pedidos",8084,"facturas",8085,"envios",8086);
    public AdminDataService(Environment env,ObjectMapper mapper) {
        this.env=env;this.mapper=mapper;
        var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.client=RestClient.builder().requestFactory(factory).build();
    }
    public JsonNode read(String resource) {
        JsonNode rows=call(resource,"",HttpMethod.GET,null);
        if(!rows.isArray()) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Respuesta inválida del servicio de "+resource);
        return rows;
    }
    public JsonNode save(String resource,Long id,JsonNode body) {
        if(id==null && Set.of("facturas","envios").contains(resource)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Las facturas y envíos se generan desde un pedido");
        return call(resource,id==null?"":"/"+id,id==null?HttpMethod.POST:HttpMethod.PUT,body);
    }
    public void delete(String resource,long id) {call(resource,"/"+id,HttpMethod.DELETE,null);}
    private JsonNode call(String resource,String suffix,HttpMethod method,JsonNode body) {
        if(!ports.containsKey(resource)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Servicio no encontrado");
        String base=env.getProperty("app.services."+resource+"-url","http://127.0.0.1:"+ports.get(resource));
        try {
            var request=client.method(method).uri(base+"/"+resource+suffix);
            if(body!=null) request.contentType(MediaType.APPLICATION_JSON).body(body);
            String text=request.retrieve().body(String.class);
            if(text==null || text.isBlank()) return mapper.nullNode();
            try {return mapper.readTree(text);} catch(Exception ex) {return mapper.getNodeFactory().textNode(text);}
        } catch(RestClientResponseException ex) {
            String message="La operación fue rechazada por el servicio de "+resource;
            try {JsonNode response=mapper.readTree(ex.getResponseBodyAsString()); if(response.hasNonNull("message")) message=response.get("message").asText();} catch(Exception ignored) {}
            throw new ResponseStatusException(ex.getStatusCode(),message);
        } catch(RestClientException ex) {throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Servicio de "+resource+" no disponible");}
    }
    public Map<String,Object> snapshot() {
        Map<String,java.util.concurrent.CompletableFuture<Map<String,Object>>> tasks=new LinkedHashMap<>();
        for(String resource:List.of("clientes","productos","inventarios","pedidos","facturas","envios")) {
            tasks.put(resource,java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {return Map.<String,Object>of("available",true,"rows",read(resource));}
                catch(ResponseStatusException ex) {return Map.<String,Object>of("available",false,"rows",mapper.createArrayNode(),"message",ex.getReason());}
            }));
        }
        Map<String,Object> data=new LinkedHashMap<>();
        tasks.forEach((resource,task)->data.put(resource,task.join()));
        return data;
    }
    private ObjectNode object(Object... pairs) {
        ObjectNode node=mapper.createObjectNode();
        for(int i=0;i<pairs.length;i+=2) node.set(pairs[i].toString(),mapper.valueToTree(pairs[i+1]));
        return node;
    }
    private JsonNode find(JsonNode rows,String key,String value) {
        for(JsonNode row:rows) if(row.path(key).asText().equals(value)) return row;
        return null;
    }
    // No se reescriben datos existentes. Solo cinco conjuntos enlazados y reconocibles.
    public synchronized Map<String,Object> seedDemo() {
        if(!env.getProperty("app.demo.enabled",Boolean.class,true)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"La carga de ejemplos está deshabilitada");
        Map<String,Integer> created=new LinkedHashMap<>();
        created.put("clientes",0);created.put("productos",0);created.put("inventarios",0);created.put("pedidos",0);
        // Comprobar disponibilidad de todos antes de empezar; la carga se puede reanudar.
        Map<String,JsonNode> current=new HashMap<>();
        for(String resource:ports.keySet()) current.put(resource,read(resource));
        String[] names={"Ana Pérez","Diego Soto","Camila Rojas","Tomás Vega","Sofía Muñoz"};
        String[] products={"Teclado de prueba","Mouse de prueba","Audífonos de prueba","Webcam de prueba","Soporte de prueba"};
        int[] prices={19990,12990,24990,34990,15990};
        for(int i=0;i<5;i++) {
            String email="demo"+(i+1)+"@example.test";
            JsonNode customer=find(current.get("clientes"),"email",email);
            if(customer==null && current.get("clientes").size()+created.get("clientes")<5) {customer=save("clientes",null,object("nombre",names[i],"email",email,"telefono","+5690000000"+i,"direccion","Dirección de prueba "+(i+1)+", Santiago"));created.merge("clientes",1,Integer::sum);}
            JsonNode product=find(current.get("productos"),"nombre",products[i]);
            if(product==null && current.get("productos").size()+created.get("productos")<5) {product=save("productos",null,object("nombre",products[i],"descripcion","Producto ficticio para demostración","precio",prices[i],"activo",true));created.merge("productos",1,Integer::sum);}
            if(customer==null) customer=current.get("clientes").get(i % current.get("clientes").size());
            if(product==null) product=current.get("productos").get(i % current.get("productos").size());
            long productId=product.path("id").asLong();
            if(current.get("inventarios").size()+created.get("inventarios")<5 && find(current.get("inventarios"),"productoId",Long.toString(productId))==null) {
                save("inventarios",null,object("productoId",productId,"cantidadDisponible",20+i*5,"stockMinimo",5));created.merge("inventarios",1,Integer::sum);
            }
            String ref="DEMO-PED-"+(i+1);
            if(current.get("pedidos").size()+created.get("pedidos")<5 && find(current.get("pedidos"),"referenciaExterna",ref)==null) {
                var line=object("productoId",productId,"cantidad",i%2+1);
                save("pedidos",null,object("clienteId",customer.path("id").asLong(),"direccionEntrega",customer.path("direccion").asText(),"referenciaExterna",ref,"lineas",List.of(line)));
                created.merge("pedidos",1,Integer::sum);
            }
        }
        // Completar derivados que falten en pedidos de pruebas anteriores o consumidores detenidos.
        // Sus importes y relaciones salen siempre del pedido, sin modificar el original.
        JsonNode orders=read("pedidos");
        JsonNode invoices=read("facturas"); JsonNode shipments=read("envios");
        int repairedInvoices=0,repairedShipments=0;
        for(JsonNode order:orders) {
            if(repairedInvoices+invoices.size()<5 && find(invoices,"pedidoId",order.path("id").asText())==null) {
                try {call("facturas","",HttpMethod.POST,object("pedidoId",order.path("id").asLong(),"clienteId",order.path("clienteId").asLong(),"estadoPago","PENDIENTE"));repairedInvoices++;}
                catch(ResponseStatusException ex){if(ex.getStatusCode().value()!=409) throw ex;}
            }
            if(repairedShipments+shipments.size()<5 && find(shipments,"pedidoId",order.path("id").asText())==null) {
                try {call("envios","",HttpMethod.POST,object("pedidoId",order.path("id").asLong(),"direccionEntrega",order.path("direccionEntrega").asText(),"estadoEnvio","EN_PREPARACION","numeroSeguimiento","DEMO-"+order.path("id").asText()));repairedShipments++;}
                catch(ResponseStatusException ex){if(ex.getStatusCode().value()!=409) throw ex;}
            }
        }
        created.put("facturas",repairedInvoices);created.put("envios",repairedShipments);
        return Map.of("created",created,"message","Datos de prueba preparados hasta un máximo de cinco registros por servicio. Se conservaron los existentes; actualiza para consultar los derivados.");
    }
}
