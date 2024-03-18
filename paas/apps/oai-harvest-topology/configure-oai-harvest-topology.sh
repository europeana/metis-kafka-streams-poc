#!/bin/bash
set -e

echo "Configure oai-harvest-topology"

oc apply -f apps/oai-harvest-topology/oai-harvest-topology-config.yaml
oc apply -f apps/oai-harvest-topology/oai-harvest-topology.yaml


echo "Oai-harvest-topology configured"