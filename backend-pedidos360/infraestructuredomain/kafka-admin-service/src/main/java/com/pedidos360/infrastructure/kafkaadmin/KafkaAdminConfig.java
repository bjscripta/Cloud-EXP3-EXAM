package com.pedidos360.infrastructure.kafkaadmin;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaAdminConfig {
    @Bean(destroyMethod = "close")
    public AdminClient kafkaAdminClient(KafkaProperties properties) {
        return AdminClient.create(properties.buildAdminProperties(null));
    }
}
