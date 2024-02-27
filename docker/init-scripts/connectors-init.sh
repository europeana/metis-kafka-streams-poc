#bin/bash

curl -i -X POST \
-H "Accept:application/json" \
-H "Content-Type:application/json" \
http://localhost:8083/connectors/ \
-d '{
  "name": "topologies-connector",
  "config": {
      "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
      "plugin.name": "pgoutput",
      "tasks.max": "1",
      "database.hostname": "postgres",
      "database.user": "postgres",
      "database.password": "postgres",
      "database.dbname": "poc-db",
      "topic.prefix": "connect",
      "table.include.list": "public.record_execution",
      "schema.history.internal.kafka.bootstrap.servers": "kafka-0:9092,kafka-1:9092,kafka-2:9092",
      "schema.history.internal.kafka.topic": "schema-history.topologies",
      "transforms": "unwrap",
      "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
      "transforms.unwrap.drop.tombstones": false,
      "transforms.unwrap.delete.handling.mode": "rewrite"
  }
}'