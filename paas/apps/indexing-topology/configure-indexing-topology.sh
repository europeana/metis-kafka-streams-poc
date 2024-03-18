#!/bin/bash
set -e

echo "Configure indexing-topology"

oc apply -f apps/indexing-topology/indexing-topology-config.yaml
oc apply -f apps/indexing-topology/indexing-topology.yaml


echo "Indexing-topology configured"