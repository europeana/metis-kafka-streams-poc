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

import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import static eu.europeana.cloud.commons.TopologyNodeNames.*;
import static eu.europeana.cloud.commons.TopologyTopicNames.*;
import static java.lang.Thread.sleep;

public class DatabaseQueueReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseQueueReader.class);


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
            NewTopic databaseQueueReaderTopic = new NewTopic(DATABASE_QUEUE_READER_TOPIC_NAME, 1, (short) 1);
            NewTopic oaiHarvestTopic = new NewTopic(OAI_HARVEST_SOURCE_TOPIC_NAME, 1, (short) 1);
            NewTopic validationTopic = new NewTopic(VALIDATION_SOURCE_TOPIC_NAME, 1, (short) 1);
            NewTopic mediaTopic = new NewTopic(MEDIA_SOURCE_TOPIC_NAME, 1, (short) 1);
            NewTopic transformationTopic = new NewTopic(TRANSFORMATION_SOURCE_TOPIC_NAME, 1, (short) 1);
            NewTopic enrichmentTopic = new NewTopic(ENRICHMENT_SOURCE_TOPIC_NAME, 1, (short) 1);
            NewTopic normalizationTopic = new NewTopic(NORMALIZATION_SOURCE_TOPIC_NAME, 1, (short) 1);
            CreateTopicsOptions topicsOptions = new CreateTopicsOptions()
                    .retryOnQuotaViolation(false);
            CreateTopicsResult createTopicsResult = admin.createTopics(List.of(
                    databaseQueueReaderTopic,
                    oaiHarvestTopic,
                    validationTopic,
                    mediaTopic,
                    transformationTopic,
                    enrichmentTopic,
                    normalizationTopic
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
            topology.addSource(DATABASE_QUEUE_READER_SOURCE_NAME, recordExecutionKeySerde.deserializer(), recordExecutionSerde.deserializer(), DATABASE_QUEUE_READER_TOPIC_NAME);
            topology.addProcessor(DATABASE_QUEUE_READER_PROCESSOR_NAME, DatabaseQueueReaderProcessor::new, DATABASE_QUEUE_READER_SOURCE_NAME);
            topology.addSink(DATABASE_QUEUE_READER_OAI_HARVEST_SINK_NAME, OAI_HARVEST_SOURCE_TOPIC_NAME, recordExecutionKeySerde.serializer(), recordExecutionSerde.serializer(), DATABASE_QUEUE_READER_PROCESSOR_NAME);
            topology.addSink(DATABASE_QUEUE_READER_VALIDATION_SINK_NAME, VALIDATION_SOURCE_TOPIC_NAME, recordExecutionKeySerde.serializer(), recordExecutionSerde.serializer(), DATABASE_QUEUE_READER_PROCESSOR_NAME);
            topology.addSink(DATABASE_QUEUE_READER_MEDIA_SINK_NAME, MEDIA_SOURCE_TOPIC_NAME, recordExecutionKeySerde.serializer(), recordExecutionSerde.serializer(), DATABASE_QUEUE_READER_PROCESSOR_NAME);
            topology.addSink(DATABASE_QUEUE_READER_ENRICHMENT_SINK_NAME, ENRICHMENT_SOURCE_TOPIC_NAME, recordExecutionKeySerde.serializer(), recordExecutionSerde.serializer(), DATABASE_QUEUE_READER_PROCESSOR_NAME);
            topology.addSink(DATABASE_QUEUE_READER_TRANSFORMATION_SINK_NAME, TRANSFORMATION_SOURCE_TOPIC_NAME, recordExecutionKeySerde.serializer(), recordExecutionSerde.serializer(), DATABASE_QUEUE_READER_PROCESSOR_NAME);
            topology.addSink(DATABASE_QUEUE_READER_NORMALIZATION_SINK_NAME, NORMALIZATION_SOURCE_TOPIC_NAME, recordExecutionKeySerde.serializer(), recordExecutionSerde.serializer(), DATABASE_QUEUE_READER_PROCESSOR_NAME);
            return topology;
        }
    }

    private static Properties readProperties() {
        Properties properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "database-queue-reader");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092,localhost:9093,localhost:9094");
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return properties;
    }
}