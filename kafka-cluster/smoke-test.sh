#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

# Uses the caller's Docker context. Set DOCKER_HOST when the environment requires it.
compose=(docker compose)
run_id="smoke-$(date +%s)-$$"
for topic in pedidos-eventos ventas-eventos; do
  payload="{\"eventId\":\"${run_id}-${topic}\",\"tipo\":\"infraestructura.smoke\"}"
  printf '%s\n' "$payload" | "${compose[@]}" exec -T kafka-1 kafka-console-producer \
    --bootstrap-server kafka-1:9092 --topic "$topic" \
    --producer-property acks=all
  output=$("${compose[@]}" exec -T kafka-1 kafka-console-consumer \
    --bootstrap-server kafka-1:9092 --topic "$topic" --from-beginning \
    --group "${run_id}-${topic}" --timeout-ms 10000 2>&1) || {
      # The console consumer exits nonzero on its expected end-of-window timeout.
      if [[ "$output" != *"TimeoutException"* ]]; then
        printf '%s\n' "$output" >&2
        exit 1
      fi
    }
  if [[ "$output" != *"$payload"* ]]; then
    printf 'FAIL: evento no recibido en %s\n%s\n' "$topic" "$output" >&2
    exit 1
  fi
  "${compose[@]}" exec -T kafka-1 kafka-consumer-groups \
    --bootstrap-server kafka-1:9092 --delete --group "${run_id}-${topic}"
  printf 'PASS: publicar y consumir en %s\n' "$topic"
done
