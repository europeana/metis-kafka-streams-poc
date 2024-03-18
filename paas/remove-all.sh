if [ -z ${OPENSHIFT_PROJECT} ]; then echo Error: Environment variable OPENSHIFT_PROJECT not defined; exit 1; fi

oc project $OPENSHIFT_PROJECT

oc delete all --selector 'app in (connect,connect-ui,kafdrop,zookeeper,postgres,kafka)'
oc delete all --selector 'app in (database-transfer-topology,validation-topology,transformation-topology,oai-harvest-topology,normalization-topology,media-topology,indexing-topology,enrichment-topology)'
oc delete pvc --selector 'app in (kafka,postgres,zookeeper)'
oc delete configmap --selector 'app in (kafka,database-transfer-topology,validation-topology,transformation-topology,oai-harvest-topology,normalization-topology,media-topology,indexing-topology,enrichment-topology)'
oc delete secret --selector 'app in (docker-registry)'