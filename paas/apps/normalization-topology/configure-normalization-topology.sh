#!/bin/bash
set -e

echo "Configure normalization-topology"

oc apply -f apps/normalization-topology/normalization-topology-config.yaml
oc apply -f apps/normalization-topology/normalization-topology.yaml


echo "Normalization-topology configured"