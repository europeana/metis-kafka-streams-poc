#!/bin/bash
set -e

echo "Configure enrichment-topology"

oc apply -f apps/enrichment-topology/enrichment-topology-config.yaml
oc apply -f apps/enrichment-topology/enrichment-topology.yaml


echo "Enrichment-topology configured"