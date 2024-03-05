#bin/bash

curl -i -X POST \
-H "Accept:application/json" \
-H "Content-Type:application/json" \
http://localhost:8083/connectors/ \
-d '{
  "name": "topologies-record-execution-connector",
  "config": {
      "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
      "plugin.name": "pgoutput",
      "tasks.max": "1",
      "database.hostname": "postgres",
      "database.user": "postgres",
      "database.password": "postgres",
      "database.dbname": "poc-db",
      "slot.name": "execution_slot",
      "topic.prefix": "connect",
      "table.include.list": "public.record_execution",
      "schema.history.internal.kafka.bootstrap.servers": "kafka-0:9092,kafka-1:9092,kafka-2:9092",
      "schema.history.internal.kafka.topic": "record-execution-history",
      "transforms": "unwrap",
      "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
      "transforms.unwrap.drop.tombstones": false,
      "transforms.unwrap.delete.handling.mode": "rewrite"
  }
}'

#curl -i -X POST \
#-H "Accept:application/json" \
#-H "Content-Type:application/json" \
#http://localhost:8083/connectors/ \
#-d '{
# "name": "topologies-record-execution-result-connector",
# "config": {
#   "connector.class": "io.debezium.connector.kafka.KafkaSinkConnector",
#   "tasks.max": "1",
#   "topics": "connect.public.record_execution_result",
#   "tasks.max": "1",
#   "topics": "connect.public.record_execution_result",
#   "topics.rename.format": "${topic}.sink",
#   "key.converter": "org.apache.kafka.connect.json.JsonConverter",
#   "value.converter": "org.apache.kafka.connect.json.JsonConverter",
#   "value.converter.schemas.enable": "false",
#   "connection.url": "jdbc:postgresql://postgres:5432/poc-db",
#   "connection.user": "postgres",
#   "connection.password": "postgres",
#   "transforms": "unwrap",
#   "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
#   "transforms.unwrap.drop.tombstones": false,
#   "transforms.unwrap.delete.handling.mode": "rewrite",
#   "database.history.kafka.bootstrap.servers": "kafka-0:9092,kafka-1:9092,kafka-2:9092",
#   "database.history.kafka.topic": "record-execution-result-history"
#  }
#}'
#
#
#curl -i -X POST \
#-H "Accept:application/json" \
#-H "Content-Type:application/json" \
#http://localhost:8083/connectors/ \
#-d '{
#      "name": "topologies-record-execution-exception-connector",
#      "config": {
#        "connector.class": "io.debezium.connector.kafka.KafkaSinkConnector",
#        "tasks.max": "1",
#        "topics": "connect.public.record_execution_exception",
#        "topics.rename.format": "${topic}.sink",
#        "key.converter": "org.apache.kafka.connect.json.JsonConverter",
#        "value.converter": "org.apache.kafka.connect.json.JsonConverter",
#        "value.converter.schemas.enable": "false",
#        "connection.url": "jdbc:postgresql://postgres:5432/poc-db",
#        "connection.user": "postgres",
#        "connection.password": "postgres",
#        "transforms": "unwrap",
#        "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
#        "transforms.unwrap.drop.tombstones": false,
#        "transforms.unwrap.delete.handling.mode": "rewrite",
#        "database.history.kafka.bootstrap.servers": "kafka-0:9092,kafka-1:9092,kafka-2:9092",
#        "database.history.kafka.topic": "record-execution-exception-history"
#      }
#    }
#'