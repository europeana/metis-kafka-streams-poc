package eu.europeana.cloud.topologies;

import eu.europeana.cloud.processors.mediaProcessors.*;
import eu.europeana.cloud.serdes.MessageSerde;
import eu.europeana.cloud.suppliers.NotificationProcessorSupplier;
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

import static eu.europeana.cloud.commons.TopologyNodeNames.*;
import static eu.europeana.cloud.commons.TopologyTopicNames.MEDIA_INPUT_TOPIC_NAME;
import static eu.europeana.cloud.commons.TopologyTopicNames.MEDIA_INTERMEDIATE_TOPIC_NAME;

public class MediaTopology {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaTopology.class);


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
        topology.addSource(MEDIA_INPUT_SOURCE_NAME, Serdes.String().deserializer(), new MessageSerde().deserializer(), MEDIA_INPUT_TOPIC_NAME);
        topology.addProcessor(MEDIA_FILE_READER_PROCESSOR_NAME, () -> new FileReaderProcessor(properties), MEDIA_INPUT_SOURCE_NAME);
        topology.addProcessor(MEDIA_RESOURCE_GETTER_PROCESSOR_NAME, () -> new RdfResourceProcessor(properties), MEDIA_FILE_READER_PROCESSOR_NAME);
        topology.addProcessor(MEDIA_RESOURCE_EXTRACTOR_PROCESSOR_NAME, () -> new MediaExtractorProcessor(properties),
                MEDIA_RESOURCE_GETTER_PROCESSOR_NAME);
        topology.addProcessor(MEDIA_ENRICH_PROCESSOR_NAME, () -> new EnrichProcessor(properties),
                MEDIA_RESOURCE_EXTRACTOR_PROCESSOR_NAME);
        topology.addProcessor(MEDIA_RECORD_WRITER_PROCESSOR_NAME, () -> new WriteRecordProcessor(properties), MEDIA_ENRICH_PROCESSOR_NAME);
        topology.addSink(MEDIA_INTERMEDIATE_SINK_NAME, MEDIA_INTERMEDIATE_TOPIC_NAME, Serdes.String().serializer(), new MessageSerde().serializer(),
                MEDIA_RECORD_WRITER_PROCESSOR_NAME, MEDIA_ENRICH_PROCESSOR_NAME, MEDIA_RESOURCE_EXTRACTOR_PROCESSOR_NAME,
                MEDIA_RESOURCE_GETTER_PROCESSOR_NAME, MEDIA_FILE_READER_PROCESSOR_NAME);

        topology.addSource(MEDIA_NOTIFICATION_SOURCE_NAME, Serdes.String().deserializer(), new MessageSerde().deserializer(), MEDIA_INTERMEDIATE_TOPIC_NAME);
        topology.addProcessor(MEDIA_NOTIFICATION_PROCESSOR_NAME, new NotificationProcessorSupplier(properties),
                MEDIA_NOTIFICATION_SOURCE_NAME);
        return topology;
    }

    private static Properties readProperties() throws IOException {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(Thread.currentThread().getContextClassLoader()
                .getResource("mediaTopology.properties").getPath())) {
            properties.load(fis);
            properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "media-topology");
            properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
            properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
            properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
            properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        } catch (FileNotFoundException e) {
            LOGGER.error("Property file not found", e);
        }
        return properties;
    }
}
