#!/bin/bash
set -e

echo "Configure Postgres"

oc apply -f postgres/postgres-volume.yaml
oc apply -f postgres/postgres-service.yaml
oc apply -f postgres/postgres.yaml

echo "Postgres configured"