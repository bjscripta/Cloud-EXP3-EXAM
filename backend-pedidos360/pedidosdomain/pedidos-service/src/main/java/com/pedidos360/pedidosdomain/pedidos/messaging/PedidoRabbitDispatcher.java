package com.pedidos360.pedidosdomain.pedidos.messaging;
import com.pedidos360.messaging.RabbitTopology;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import org.slf4j.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component
public class PedidoRabbitDispatcher {
    private static final Logger log=LoggerFactory.getLogger(PedidoRabbitDispatcher.class);
    private final PedidoRabbitOutboxRepository repository;
    private final RabbitTemplate rabbit;
    public PedidoRabbitDispatcher(PedidoRabbitOutboxRepository repository,RabbitTemplate rabbit) {this.repository=repository;this.rabbit=rabbit;}
    @Scheduled(fixedDelay=1000) @Transactional
    public void dispatch() {
        repository.findFirstBySentFalseOrderByIdAsc().ifPresent(row -> {
            if(row.getNextAttemptAt().isAfter(Instant.now())) return;
            try {
                var correlation=new CorrelationData(UUID.randomUUID().toString());
                rabbit.convertAndSend(RabbitTopology.EVENTS_EXCHANGE,RabbitTopology.PEDIDO_CREADO_ROUTING_KEY,row.getPayload(),correlation);
                var confirm=correlation.getFuture().get(10,TimeUnit.SECONDS);
                if(!confirm.isAck() || correlation.getReturned()!=null) throw new IllegalStateException("RabbitMQ no confirmó el mensaje");
                row.setSent(true);
            } catch(Exception ex) {
                if(ex instanceof InterruptedException) Thread.currentThread().interrupt();
                row.setAttempts(Math.min(row.getAttempts()+1,30));
                row.setNextAttemptAt(Instant.now().plusSeconds(Math.min(60,1L<<Math.min(row.getAttempts(),6))));
                log.warn("Pedido Rabbit pendiente outboxId={} intento={} error={}",row.getId(),row.getAttempts(),ex.getClass().getSimpleName());
            }
        });
    }
}
