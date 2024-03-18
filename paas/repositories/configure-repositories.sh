#!/bin/bash
set -e

echo "Configure repositories"

oc apply -f repositories/docker-registry-secret.yaml
oc secrets link default docker-registry-secret --for=pull

echo "Repositories configured"