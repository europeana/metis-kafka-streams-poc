package eu.europeana.cloud.topologies;

import eu.europeana.cloud.processors.validationProcessors.ValidationProcessor;
import eu.europeana.cloud.serdes.RecordExecutionExceptionSerde;
import eu.europeana.cloud.serdes.RecordExecutionKeySerde;
import eu.europeana.cloud.serdes.RecordExecutionResultSerde;
import eu.europeana.cloud.serdes.RecordExecutionSerde;
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
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import static eu.europeana.cloud.commons.TopologyNodeNames.VALIDATION_PROCESSOR_NAME;
import static eu.europeana.cloud.commons.TopologyNodeNames.VALIDATION_TOPOLOGY_SOURCE_NAME;
import static eu.europeana.cloud.commons.TopologyPropertyKeys.KAFKA_HOSTS;
import static eu.europeana.cloud.commons.TopologyTopicNames.VALIDATION_SOURCE_TOPIC_NAME;

public class ValidationTopology {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationTopology.class);


    public static void main(String[] args) throws IOException {
        Properties props = readProperties();
        final Topology topology = buildTopology(props);


        try (KafkaStreams streams = new KafkaStreams(topology, props)) {
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

    private static Topology buildTopology(Properties properties) {
        Topology topology = new Topology();

        try (RecordExecutionSerde recordExecutionSerde = new RecordExecutionSerde();
             RecordExecutionKeySerde recordExecutionKeySerde = new RecordExecutionKeySerde();
             RecordExecutionResultSerde recordExecutionResultSerde = new RecordExecutionResultSerde();
             RecordExecutionExceptionSerde recordExecutionExceptionSerde = new RecordExecutionExceptionSerde()
        ) {
            topology.addSource(VALIDATION_TOPOLOGY_SOURCE_NAME, recordExecutionKeySerde.deserializer(), recordExecutionSerde.deserializer(), VALIDATION_SOURCE_TOPIC_NAME);
            topology.addProcessor(VALIDATION_PROCESSOR_NAME, () -> new ValidationProcessor(properties), VALIDATION_TOPOLOGY_SOURCE_NAME);
//            topology.addSink(VALIDATION_DATABASE_TRANSFER_EXECUTION_RESULTS_SINK_NAME, DATABASE_TRANSFER_RECORD_EXECUTION_RESULT_TOPIC_NAME, recordExecutionKeySerde.serializer(), recordExecutionResultSerde.serializer(), VALIDATION_PROCESSOR_NAME);
//            topology.addSink(VALIDATION_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME, DATABASE_TRANSFER_RECORD_EXECUTION_EXCEPTION_TOPIC_NAME, recordExecutionKeySerde.serializer(), recordExecutionExceptionSerde.serializer(),
//                    VALIDATION_PROCESSOR_NAME);

        }
        return topology;
    }

    private static Properties readProperties() throws IOException {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(Thread.currentThread().getContextClassLoader()
                .getResource("validationTopology.properties").getPath())) {
            properties.load(fis);
            properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "validation-topology");
            properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getProperty(KAFKA_HOSTS));
            properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
            properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
            properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        } catch (FileNotFoundException e) {
            LOGGER.error("Property file not found", e);
        }
        return properties;
    }
}
