#!/bin/bash
set -e

echo "Configure transformation-topology"

oc apply -f apps/transformation-topology/transformation-topology-config.yaml
oc apply -f apps/transformation-topology/transformation-topology.yaml


echo "Transformation-topology configured"