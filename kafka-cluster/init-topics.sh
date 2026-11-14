#!/usr/bin/env bash
set -euo pipefail

for topic in pedidos-eventos ventas-eventos pedidos-eventos.DLT ventas-eventos.DLT; do
  kafka-topics --bootstrap-server kafka-1:9092 --create --if-not-exists \
    --topic "$topic" --partitions 3 --replication-factor 2 \
    --config retention.ms=604800000 --config cleanup.policy=delete \
    --config min.insync.replicas=2
  kafka-topics --bootstrap-server kafka-1:9092 --describe --topic "$topic"
done
