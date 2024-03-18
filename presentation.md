# <p style="text-align: center;">Kafka streams for Metis</p>

<br/><br/><br/>
<p align="center">
  <img alt="PCSS logo" style="margin-left: 50px" src="https://igcz.poznan.pl/wp-content/uploads/2016/01/PCSS-logo-300x110.png" />
  <img alt="Europeana logo" style="margin-right: 100px"src="https://upload.wikimedia.org/wikipedia/commons/thumb/4/49/Europeana_logo_black.svg/361px-Europeana_logo_black.svg.png" />
</p>

<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>

<hr style="height: 40px"/>

# <p style="text-align: center;">Agenda</p>

1. How does Kafka works?
2. What is Kafka connect?
3. How it affects kafka streams and How kafka streams work?
4. What is kafka streams like?
5. What I didn't like about kafka streams?
6. Technical aspects of PoC
7. How does PoC work?
8. How does development process look like?
9. But how does it work in practice? Code, local and openshift deployments presentation
10. Which features I didn't use in PoC but are interesting?
11. Further development perspectives?
12. Any questions?

<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>

<hr style="height: 40px"/>

## <p style="text-align: center;">How does kafka works?</p>

<br/>

- **Kafka** as a message broker is back-bone of kafka streams and understanding of it is crucial for understanding kafka
  streams workflow;
- Messages are put into queues,
- Message contain key and value,
- Queues can be replicated - to assure fault tolerance and minimize time downs - and partitioned - based on kafka
  message keys,
- Offset as a tool of having information of current consumer state

<p align="center">
<img alt="Kafka architecture" src="https://daxg39y63pxwu.cloudfront.net/images/blog/apache-kafka-architecture-/image_589142173211625734253276.png"/>
</p>

<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>
<hr style="height: 40px"/>

## <p style="text-align: center;">What is Kafka connect?</p>

- Tool tested and used in process of creating PoC,
- Allow to create entities called connectors that are based on external plugins and acts like sort of connection between
  various
  databases and kafka queues.
- Those connectors are responsible for:
    - automatically putting messages to kafka queues from database based on change logs,
    - automatically putting messages from kafka queues to database based on offsets,
    - transformation, converting and serialization or deserialization of messages and records based on connector
      configuration,
- Didn't manage to put messages from queues to database as part of PoC - had issues with serialization most likely due
  to usage JSON serialization - for
  simplicity case - instead of avro

<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>
<hr style="height: 40px"/>

## <p style="text-align: center;">How it affects kafka streams and How kafka streams work?</p>

- Kafka streams works by interacting with kafka queues using consumer and producer interfaces,
- In case of source each partition of kafka queue is assigned to only one node of KS (kafka streams),
    - In case of KS node consumer (reader of message queue) not sending hearth beat in time, his partition will be
      reassigned to different KS node,
- One kafka stream node can have multiple source partitions assigned,
- We can also treat kafka queues as sinks to which result messages are put,
- All fault-tolerance is based on kafka mechanism such as:
    - in committing queue offset after processing message,
    - in case of reprocessing, when KS node is reassigned to partition, all messages from point of last committed
      offset (
      might be
      changed based on config) will be reprocessed.
- Interaction with database can be automated by using kafka connect.

<p align="center">
  <img alt="Kafka architecture" src="https://docs.confluent.io/platform/current/_images/streams-architecture-topology.jpg" width="400"/>
</p>
<br/><br/><br/><br/>
<hr style="height: 40px"/>

## <p style="text-align: center;">What is kafka streams like?</p>

- Allows processing of high volumes of data thanks to ease of horizontal scaling due to how well kafka handles
  partitioning,
- Gives two main API that allows to interact with this tool:
    - Processor API (Used in PoC) - low level API that require us to write most code ourselves but provide highest
      flexibility, least kafka load,
    - DSL - high level API that does a lot of things for us, require less code, but restrict our flexibility and provide
      highest kafka load,
    - We can also mix those two, but I didn't go into much of details in this use case. It seemed to be middle ground
      between two listed above.
- Thanks to flexibility is appropriate for both, small and big tasks,
- Provide Exactly-once semantics (Didn't test limits of that statement),
- Provide both stateless and stateful operations (map, reduce, flatmap (only in DSL) and state store),
- Doesn't require any sort of cluster, each node only needs to be connected to kafka.

<br/><br/><br/><br/><br/><br/><br/>
<hr style="height: 40px"/>

## <p style="text-align: center;">What I didn't like about kafka streams?</p>

- In general, it's great tool but require a lot of knowledge and self awareness about how we build and configure
  application,
- There is no built-in UI to inspect processing, but it is due to how this app treats scaling - no need for cluster nor
  any connection between nodes,
- Due to lack of cluster it might be difficult to maintain global state (partition wide state is fine, can be bypassed
  by good design of app),
- It is hard to manage complex operations that might require multiple partitions to work together (It require to design
  app workflow properly),
- Being handled by kafka itself comes with operational overhead,
- Messages are processed in same order that they were delivered to queue,
- There is limitation to what can be put in queue (Can be bypassed by directly querying dbs),
- It requires (same as all tools with any kind of task management) fine-tuning of all sort of timeouts in case our
  message processing might be slow.

<br/><br/><br/><br/><br/><br/><br/>
<hr style="height: 40px"/>

## <p style="text-align: center;">Technical aspects of PoC</p>

- Uses kafka with zookeeper (Couldn't make it work in Kraft mode),
- Uses postgresSQL database,
- Uses debezium kafka connect fork,
- All topology code is made to be as close to flink and spring batch version as possible,
- Uses DAO/DTO with minor changes and some extra tables such as task_status,
- It doesn't provide any web-ui to monitor processing,
- PoC additionally deploy two monitoring tools - Lafdrop - to access Kafka queue status and - connect-ui - to access
  defined connectors,
- There are two ways to deploy KS poc:
    - on local machine using docker compose that can be found in docker directory,
    - on openshift cluster using shell script that can be found in paas directory (This script is not very flexible and
      only support PSNC paas instance),
      <br/><br/><br/><br/><br/><br/><br/>

<hr style="height: 40px"/>

## <p style="text-align: center;">How does PoC work?</p>

- Task definition are manually put into database,

```agsl
INSERT INTO record_execution (dataset_id, execution_id, record_id, record_data, execution_name, execution_parameters)
        VALUES (1, 1, 2, '', 'oai_topology',
         '{ 
             "oaiEndpoint" : "https://aggregator.ekt.gr/aggregator-oai/request", 
             "oaiMetadataPrefix":"edm",
             "oaiSet":"mantamado"
         }');
```

- Kafka connect detects new record and then put it into kafka queue,
- Input topology (that also creates all topics) reads records and sends it to appropriate topology source queue,

Two steps above uses same message format, so I will put it only once for both:

```agsl
key:{
    "dataset_id":"1", 
    "execution_id":"1", 
    "record_id":"2"
},
value:{
    "record_data": "",
    "execution_name": "oai_topology",
    "execution_parameters": {
    "oaiEndpoint": "https://aggregator.ekt.gr/aggregator-oai/request",
    "oaiMetadataPrefix": "edm",
    "oaiSet": "mantamado"
    }
}
```

- Appropriate Topology process record,
- After process is finished result is directly put into database (I wanted to use kafka connect for that but didn't
  manage to). It might be exception or proper result.

<br/><br/><br/><br/><br/><br/><br/>
<hr style="height: 40px"/>

## <p style="text-align: center;">How does development process look like?</p>

- modify source code
- build application
- test locally
    - run docker compose and initiate connectors
    - run/test locally
- Copy jar into paas/apps folder and deploy docker image
- Depends on cluster options rescale apps or simply do nothing since trigger will do it in your stand

<br/><br/>
<hr style="height: 40px"/>

## <p style="text-align: center;">But how does it work in practice? Code, local and openshift deployments presentation</p>

I decided to highlight only most important aspects and to show how work with this tool feels like.

- Overview of code:
    - How does serialization work,
    - How to write processor,
    - How to structure topology
- Docker compose deployment,
    - How working on local machine with kafka streams looks like,
- PaaS
    - How deployment looks like, test of scaling up and down.

<br/><br/>
<hr style="height: 40px"/>

## <p style="text-align: center;">Which features I didn't use in PoC but are interesting?</p>

- Windowed processing - Possibility to aggregate records from window and process them at same time,
- State store - Possibility to have state that is kept and can be modified as needed
  <br/><br/><br/><br/><br/><br/><br/>

<hr style="height: 40px"/>

## <p style="text-align: center;">Further development perspectives?</p>

- Design throttling implementation - Only way that comes to my mind right now is managing parallelization by
  manipulating partitions,
- Design statistics aggregation,
- Do proper implementation of all topologies - without "shortcuts",
- Optimize docker images - separate image for media,
- Optimize project modules so jar contain only one topology - I thought it would overcomplicate showcasing PoC,
- Improve error handling - add default error handling, error handling mappers etp.
  <br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>

<hr style="height: 40px"/>

## <p style="text-align: center;">Any questions?</p>

<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>
<hr style="height: 40px"/>

---

## References

https://kafka.apache.org/documentation/streams/

https://debezium.io/documentation/reference/stable/architecture.html

