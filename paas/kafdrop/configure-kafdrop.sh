#!/bin/bash
set -e

echo "Configure Kafdrop"

oc apply -f kafdrop/kafdrop-service.yaml
oc apply -f kafdrop/kafdrop.yaml
oc apply -f kafdrop/kafdrop-route.yaml


echo "Kafdrop configured"