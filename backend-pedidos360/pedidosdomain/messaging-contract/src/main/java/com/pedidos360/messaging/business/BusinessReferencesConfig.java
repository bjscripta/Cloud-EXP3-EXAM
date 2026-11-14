package com.pedidos360.messaging.business;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
@Configuration
public class BusinessReferencesConfig {
    @Bean public BusinessReferences businessReferences(Environment env) { return new BusinessReferences(env); }
}
