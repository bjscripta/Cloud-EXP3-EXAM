package com.pedidos360.pedidosdomain.productos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.context.annotation.Import({com.pedidos360.messaging.business.BusinessReferencesConfig.class, com.pedidos360.messaging.business.BusinessErrors.class})
@SpringBootApplication
public class ProductosServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductosServiceApplication.class, args);
	}

}
