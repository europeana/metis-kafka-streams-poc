#!/bin/bash
set -e

echo "Configure database-transfer-topology"

oc apply -f apps/database-transfer-topology/database-transfer-topology-config.yaml
oc apply -f apps/database-transfer-topology/database-transfer-topology.yaml


echo "Database-transfer-topology configured"