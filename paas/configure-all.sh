#!/bin/bash
set -e

if [ -z ${OPENSHIFT_PROJECT} ]; then echo Error: Environment variable OPENSHIFT_PROJECT not defined; exit 1; fi

oc project $OPENSHIFT_PROJECT

. repositories/configure-repositories.sh
. zookeeper/configure-zookeeper.sh
. kafka/configure-kafka.sh
. kafdrop/configure-kafdrop.sh
. postgres/configure-postgres.sh
. connect/configure-connect.sh
. connect-ui/configure-connect-ui.sh

. apps/database-transfer-topology/configure-database-transfer-topology.sh
sleep 75
. apps/validation-topology/configure-validation-topology.sh
. apps/transformation-topology/configure-transformation-topology.sh
. apps/oai-harvest-topology/configure-oai-harvest-topology.sh
. apps/normalization-topology/configure-normalization-topology.sh
. apps/media-topology/configure-media-topology.sh
. apps/indexing-topology/configure-indexing-topology.sh
. apps/enrichment-topology/configure-enrichment-topology.sh