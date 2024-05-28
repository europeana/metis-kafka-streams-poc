#!/bin/bash
set -e

echo "Configure validation-topology"

oc apply -f apps/validation-topology/validation-topology-config.yaml
oc apply -f apps/validation-topology/validation-topology.yaml


echo "Validation-topology configured"