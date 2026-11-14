package com.pedidos360.infraestructuredomain.bff.admin;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/admin")
public class AdminDataController {
    private final AdminDataService service;
    public AdminDataController(AdminDataService service){this.service=service;}
    @GetMapping("/datos") public Map<String,Object> data(){return service.snapshot();}
    @GetMapping("/datos/{resource}") public JsonNode rows(@PathVariable("resource") String resource){return service.read(resource);}
    @PostMapping("/datos/{resource}") public JsonNode create(@PathVariable("resource") String resource,@RequestBody JsonNode body){return service.save(resource,null,body);}
    @PutMapping("/datos/{resource}/{id}") public JsonNode update(@PathVariable("resource") String resource,@PathVariable("id") long id,@RequestBody JsonNode body){return service.save(resource,id,body);}
    @DeleteMapping("/datos/{resource}/{id}") public void delete(@PathVariable("resource") String resource,@PathVariable("id") long id){service.delete(resource,id);}
    @PostMapping("/demo") public Map<String,Object> demo(){return service.seedDemo();}
}
