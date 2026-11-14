package com.pedidos360.pedidosdomain.envios.messaging;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.messaging.PedidoCreadoEvent;
import com.pedidos360.messaging.RabbitTopology;
import com.pedidos360.pedidosdomain.envios.service.EnviosService;
import com.rabbitmq.client.Channel;

@Component
public class PedidoCreadoConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PedidoCreadoConsumer.class);

    private final EnviosService enviosService;
    private final ObjectMapper objectMapper;

    public PedidoCreadoConsumer(EnviosService enviosService, ObjectMapper objectMapper) {
        this.enviosService = enviosService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitTopology.ENVIOS_QUEUE)
    public void recibir(String contenido, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        for (int intento = 1; intento <= 3; intento++) {
            try {
                PedidoCreadoEvent evento = procesar(contenido);
                channel.basicAck(deliveryTag, false);
                LOGGER.info("Pedido {} preparado para envio y confirmado con ACK", evento.pedidoId());
                return;
            } catch (Exception exception) {
                LOGGER.error("Intento {}/3 fallido procesando envio", intento, exception);
                if (intento == 3) {
                    channel.basicNack(deliveryTag, false, false);
                    LOGGER.error("Mensaje rechazado con NACK y enviado a la DLQ");
                }
            }
        }
    }

    private PedidoCreadoEvent procesar(String contenido) throws IOException {
        PedidoCreadoEvent evento = objectMapper.readValue(contenido, PedidoCreadoEvent.class);

        enviosService.prepararPedido(evento);
        return evento;
    }
}
