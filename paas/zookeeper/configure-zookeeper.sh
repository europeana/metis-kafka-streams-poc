#!/bin/bash
set -e

echo "Configure Zookeeper"

oc apply -f zookeeper/zookeeper-volume.yaml
oc apply -f zookeeper/zookeeper-service.yaml
oc apply -f zookeeper/zookeeper.yaml

echo "Zookeeper configured"