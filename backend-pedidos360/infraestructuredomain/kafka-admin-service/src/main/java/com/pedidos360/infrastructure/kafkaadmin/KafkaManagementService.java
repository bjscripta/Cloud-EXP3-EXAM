package com.pedidos360.infrastructure.kafkaadmin;

import java.util.*;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.config.ConfigResource;
import org.springframework.stereotype.Service;

@Service
public class KafkaManagementService {
    private final AdminClient admin;
    public KafkaManagementService(AdminClient admin) { this.admin = admin; }

    public Collection<String> topics() throws Exception {
        return admin.listTopics().names().get(10, TimeUnit.SECONDS).stream().sorted().toList();
    }

    public Map<String, Object> topic(String name) throws Exception {
        var description = admin.describeTopics(List.of(name)).allTopicNames().get(10, TimeUnit.SECONDS).get(name);
        var resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        var config = admin.describeConfigs(List.of(resource)).all().get(10, TimeUnit.SECONDS).get(resource);
        var partitions = description.partitions().stream().map(p -> Map.of(
                "partition", p.partition(), "leader", p.leader() == null ? -1 : p.leader().id(),
                "replicas", p.replicas().stream().map(n -> n.id()).toList(),
                "isr", p.isr().stream().map(n -> n.id()).toList())).toList();
        Map<String, String> configs = new TreeMap<>();
        config.entries().stream().filter(e -> !e.isSensitive() && e.value() != null)
                .forEach(e -> configs.put(e.name(), e.value()));
        return Map.of("name", name, "partitions", partitions, "configs", configs);
    }

    public void create(String name, int partitions, short replicas, Map<String, String> configs) throws Exception {
        validateConfigs(configs);
        admin.createTopics(List.of(new NewTopic(name, partitions, replicas).configs(configs)))
                .all().get(10, TimeUnit.SECONDS);
    }

    public void configure(String name, Map<String, String> configs) throws Exception {
        validateConfigs(configs);
        if (configs.isEmpty()) { throw new IllegalArgumentException("Indicar al menos una configuración"); }
        var operations = configs.entrySet().stream().map(e -> new AlterConfigOp(
                new ConfigEntry(e.getKey(), e.getValue()), AlterConfigOp.OpType.SET)).toList();
        admin.incrementalAlterConfigs(Map.of(new ConfigResource(ConfigResource.Type.TOPIC, name), operations))
                .all().get(10, TimeUnit.SECONDS);
    }

    public void partitions(String name, int total) throws Exception {
        admin.createPartitions(Map.of(name, NewPartitions.increaseTo(total))).all().get(10, TimeUnit.SECONDS);
    }

    public void delete(String name) throws Exception {
        admin.deleteTopics(List.of(name)).all().get(10, TimeUnit.SECONDS);
    }

    public List<Map<String, String>> groups() throws Exception {
        return admin.listConsumerGroups().all().get(10, TimeUnit.SECONDS).stream()
                .map(g -> Map.of("groupId", g.groupId(), "state", g.state().map(Object::toString).orElse("UNKNOWN")))
                .toList();
    }

    public Map<String, Object> group(String id) throws Exception {
        var description = admin.describeConsumerGroups(List.of(id)).all().get(10, TimeUnit.SECONDS).get(id);
        if (description.state() == ConsumerGroupState.DEAD) {
            throw new GroupIdNotFoundException("Grupo de consumidores inexistente");
        }
        var committed = admin.listConsumerGroupOffsets(id).partitionsToOffsetAndMetadata().get(10, TimeUnit.SECONDS);
        Map<TopicPartition, OffsetSpec> requests = new HashMap<>();
        committed.forEach((partition, offset) -> { if (offset != null) requests.put(partition, OffsetSpec.latest()); });
        var latest = admin.listOffsets(requests).all().get(10, TimeUnit.SECONDS);
        List<Map<String, Object>> offsets = new ArrayList<>();
        committed.forEach((partition, offset) -> {
            if (offset == null) { return; }
            long end = latest.get(partition).offset();
            offsets.add(Map.of("topic", partition.topic(), "partition", partition.partition(),
                    "committedOffset", offset.offset(), "endOffset", end, "lag", Math.max(0, end - offset.offset())));
        });
        var members = description.members().stream().map(m -> Map.of(
                "consumerId", m.consumerId(), "clientId", m.clientId(),
                "partitions", m.assignment().topicPartitions().stream().map(Object::toString).sorted().toList())).toList();
        return Map.of("groupId", id, "state", description.state().toString(), "members", members, "offsets", offsets);
    }

    private void validateConfigs(Map<String, String> configs) {
        Set<String> allowed = Set.of("retention.ms", "retention.bytes", "cleanup.policy", "min.insync.replicas");
        configs.forEach((key, value) -> {
            if (!allowed.contains(key) || value == null || value.isBlank()) {
                throw new IllegalArgumentException("Configuración no permitida o vacía: " + key);
            }
        });
    }
}
