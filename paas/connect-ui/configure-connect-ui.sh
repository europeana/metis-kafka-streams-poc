#!/bin/bash
set -e

echo "Configure connect-ui"

oc apply -f connect-ui/connect-ui-service.yaml
oc apply -f connect-ui/connect-ui.yaml
oc apply -f connect-ui/connect-ui-route.yaml


echo "Connect-ui configured"