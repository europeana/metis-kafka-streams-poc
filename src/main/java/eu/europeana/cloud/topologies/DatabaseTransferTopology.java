package eu.europeana.cloud.topologies;

import eu.europeana.cloud.processors.databaseQueueReaderProcessors.DatabaseQueueReaderProcessor;
import eu.europeana.cloud.serdes.RecordExecutionKeySerde;
import eu.europeana.cloud.serdes.RecordExecutionSerde;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import static eu.europeana.cloud.commons.TopologyConstants.DEFAULT_TOPIC_PARTITION_COUNT;
import static eu.europeana.cloud.commons.TopologyConstants.DEFAULT_TOPIC_REPLICATION_FACTOR;
import static eu.europeana.cloud.commons.TopologyNodeNames.*;
import static eu.europeana.cloud.commons.TopologyPropertyKeys.KAFKA_HOSTS;
import static eu.europeana.cloud.commons.TopologyTopicNames.*;
import static java.lang.Thread.sleep;

public class DatabaseTransferTopology {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseTransferTopology.class);


    public static void main(String[] args) {
        Properties properties = readProperties();

        createTopics(properties);

        final Topology topology = buildTopology();
        try (KafkaStreams streams = new KafkaStreams(topology, properties)) {
            final CountDownLatch latch = new CountDownLatch(1);

            // attach shutdown handler to catch control-c
            Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
                @Override
                public void run() {
                    streams.close();
                    latch.countDown();
                }
            });

            try {
                streams.start();
                latch.await();
            } catch (Throwable e) {
                System.exit(1);
            }
            System.exit(0);
        }
    }

    private static void createTopics(Properties properties) {
        try (Admin admin = Admin.create(properties)) {
            NewTopic databaseTransferRecordExecutionTopic = new NewTopic(DATABASE_TRANSFER_RECORD_EXECUTION_TOPIC_NAME, DEFAULT_TOPIC_PARTITION_COUNT, (short) DEFAULT_TOPIC_REPLICATION_FACTOR);
            NewTopic databaseTransferRecordExecutionResultTopic = new NewTopic(DATABASE_TRANSFER_RECORD_EXECUTION_RESULT_TOPIC_NAME, DEFAULT_TOPIC_PARTITION_COUNT, (short) DEFAULT_TOPIC_REPLICATION_FACTOR);
            NewTopic databaseTransferRecordExecutionExceptionTopic = new NewTopic(DATABASE_TRANSFER_RECORD_EXECUTION_EXCEPTION_TOPIC_NAME, DEFAULT_TOPIC_PARTITION_COUNT, (short) DEFAULT_TOPIC_REPLICATION_FACTOR);
            NewTopic oaiHarvestTopic = new NewTopic(OAI_HARVEST_SOURCE_TOPIC_NAME, DEFAULT_TOPIC_PARTITION_COUNT, (short) DEFAULT_TOPIC_REPLICATION_FACTOR);
            NewTopic validationTopic = new NewTopic(VALIDATION_SOURCE_TOPIC_NAME, DEFAULT_TOPIC_PARTITION_COUNT, (short) DEFAULT_TOPIC_REPLICATION_FACTOR);
            NewTopic mediaTopic = new NewTopic(MEDIA_SOURCE_TOPIC_NAME, DEFAULT_TOPIC_PARTITION_COUNT, (short) DEFAULT_TOPIC_REPLICATION_FACTOR);
            NewTopic transformationTopic = new NewTopic(TRANSFORMATION_SOURCE_TOPIC_NAME, DEFAULT_TOPIC_PARTITION_COUNT, (short) DEFAULT_TOPIC_REPLICATION_FACTOR);
            NewTopic enrichmentTopic = new NewTopic(ENRICHMENT_SOURCE_TOPIC_NAME, DEFAULT_TOPIC_PARTITION_COUNT, (short) DEFAULT_TOPIC_REPLICATION_FACTOR);
            NewTopic normalizationTopic = new NewTopic(NORMALIZATION_SOURCE_TOPIC_NAME, DEFAULT_TOPIC_PARTITION_COUNT, (short) DEFAULT_TOPIC_REPLICATION_FACTOR);
            NewTopic indexingTopic = new NewTopic(INDEXING_SOURCE_TOPIC_NAME, DEFAULT_TOPIC_PARTITION_COUNT, (short) DEFAULT_TOPIC_REPLICATION_FACTOR);
            CreateTopicsOptions topicsOptions = new CreateTopicsOptions()
                    .retryOnQuotaViolation(false);
            CreateTopicsResult createTopicsResult = admin.createTopics(List.of(
                    databaseTransferRecordExecutionTopic,
                    databaseTransferRecordExecutionResultTopic,
                    databaseTransferRecordExecutionExceptionTopic,
                    oaiHarvestTopic,
                    validationTopic,
                    mediaTopic,
                    transformationTopic,
                    enrichmentTopic,
                    normalizationTopic,
                    indexingTopic
            ), topicsOptions);
            while (!createTopicsResult.all().isDone()) {
                try {
                    LOGGER.info("waiting for topic creation...");
                    sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    private static Topology buildTopology() {
        Topology topology = new Topology();
        try (RecordExecutionSerde recordExecutionSerde = new RecordExecutionSerde();
             RecordExecutionKeySerde recordExecutionKeySerde = new RecordExecutionKeySerde()) {
            topology.addSource(DATABASE_TRANSFER_SOURCE_NAME, recordExecutionKeySerde.deserializer(), recordExecutionSerde.deserializer(), DATABASE_TRANSFER_RECORD_EXECUTION_TOPIC_NAME);
            topology.addProcessor(DATABASE_TRANSFER_PROCESSOR_NAME, DatabaseQueueReaderProcessor::new, DATABASE_TRANSFER_SOURCE_NAME);
            topology.addSink(DATABASE_TRANSFER_OAI_HARVEST_SINK_NAME, OAI_HARVEST_SOURCE_TOPIC_NAME, recordExecutionKeySerde.serializer(), recordExecutionSerde.serializer(), DATABASE_TRANSFER_PROCESSOR_NAME);
            topology.addSink(DATABASE_TRANSFER_VALIDATION_SINK_NAME, VALIDATION_SOURCE_TOPIC_NAME, recordExecutionKeySerde.serializer(), recordExecutionSerde.serializer(), DATABASE_TRANSFER_PROCESSOR_NAME);
            topology.addSink(DATABASE_TRANSFER_MEDIA_SINK_NAME, MEDIA_SOURCE_TOPIC_NAME, recordExecutionKeySerde.serializer(), recordExecutionSerde.serializer(), DATABASE_TRANSFER_PROCESSOR_NAME);
            topology.addSink(DATABASE_TRANSFER_ENRICHMENT_SINK_NAME, ENRICHMENT_SOURCE_TOPIC_NAME, recordExecutionKeySerde.serializer(), recordExecutionSerde.serializer(), DATABASE_TRANSFER_PROCESSOR_NAME);
            topology.addSink(DATABASE_TRANSFER_TRANSFORMATION_SINK_NAME, TRANSFORMATION_SOURCE_TOPIC_NAME, recordExecutionKeySerde.serializer(), recordExecutionSerde.serializer(), DATABASE_TRANSFER_PROCESSOR_NAME);
            topology.addSink(DATABASE_TRANSFER_NORMALIZATION_SINK_NAME, NORMALIZATION_SOURCE_TOPIC_NAME, recordExecutionKeySerde.serializer(), recordExecutionSerde.serializer(), DATABASE_TRANSFER_PROCESSOR_NAME);
            return topology;
        }
    }

    private static Properties readProperties() {
        Properties properties = new Properties();

        try (FileInputStream fis = new FileInputStream(Thread.currentThread().getContextClassLoader()
                .getResource("databaseQueueReader.properties").getPath())) {
            properties.load(fis);
            properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getProperty(KAFKA_HOSTS));
            properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "database-queue-reader");
            properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
            properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
            properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        } catch (FileNotFoundException e) {
            LOGGER.error("Property file not found", e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            LOGGER.error("Error when attempting to read property file", e);
            throw new RuntimeException(e);
        }
        return properties;
    }
}