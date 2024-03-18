#!/bin/bash
set -e

echo "Configure Kafka"

oc apply -f kafka/kafka-config.yaml
oc apply -f kafka/kafka-service.yaml
oc apply -f kafka/kafka.yaml


echo "Kafka configured"