#!/bin/bash
set -e

echo "Configure connect"

oc apply -f connect/connect-service.yaml
oc apply -f connect/connect.yaml

echo "Connect configured"