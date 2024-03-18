#!/bin/bash
set -e

echo "Configure media-topology"

oc apply -f apps/media-topology/media-topology-config.yaml
oc apply -f apps/media-topology/media-topology.yaml


echo "Media-topology configured"