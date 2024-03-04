To run this PoC we need to run docker compose that can be found in docker directory of this project.

After docker compose successfully started we need to launch kafka connect connectors by running shell script that can be
found under docker/init-scripts path inside this project.

We also need to install image magick with version 7 or higher for sake of media topology (I used method 2 from this
guide: https://linuxopsys.com/topics/install-latest-imagemagick-on-ubuntu)
We also need to create property files for all topologies that we want to run

By default, postgres db is created on localhost, port 5432, with database name poc-db and login and password equal to
postgres.

To run this PoC we need to start Database transfer topology and topology that we want to run.

To send task to any topology we must add record to record_execution table of our postgres db.
Depends on topology there might be required task parameters that we need to fill.

If you got to common classes and look for ExecutionPropertyKeys then you will see constants that define those properties
in format:
-<TOPOLOGY_THAT_NEEDS_THAT_PROPERTY>_<PROPERTY_NAME>

In case your topology require any property (OAI, transformation, validation, indexing) then put those values in field
_execution_parameters_ of table mentioned above.

Record data or url to harvest should be put as value of _field record_data_.

Topology that we want to run is specified by execution_name field that needs to be set. Possible value can be found in
common class TopologyNames file.

Example oai task definition:
-INSERT INTO record_execution (dataset_id, execution_id, record_id, record_data, execution_name, execution_parameters)
VALUES ('1', '1', '1', '', 'oai_topology', '{"oaiEndpoint":"https://aggregator.ekt.gr/aggregator-oai/request", "
oaiMetadataPrefix":"edm", "oaiSet":"mantamado"}');

In the end result values will be automatically put inside database record_execution_result or record_execution_exception
table based on result of task, but for now I am trying to configure kafka connect to do so, so you can find result
records or exception inside appropriate kafka queues.

**To access kafka queues go to http://localhost:9000**

**To access kafka connect connectors go to http://localhost:8083/connectors**

**After trying to rerun topology there might be some delay before it gets active again since it needs to read some
metadata from queue**

**Worries**

**For now record data is stored in queues when passing between nodes what is concerning in production environment (for
PoC is fine I guess)**