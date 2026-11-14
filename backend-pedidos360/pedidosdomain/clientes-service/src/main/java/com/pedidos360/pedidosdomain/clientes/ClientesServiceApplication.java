package com.pedidos360.pedidosdomain.clientes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.context.annotation.Import({com.pedidos360.messaging.business.BusinessReferencesConfig.class, com.pedidos360.messaging.business.BusinessErrors.class})
@SpringBootApplication
public class ClientesServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClientesServiceApplication.class, args);
	}

}
