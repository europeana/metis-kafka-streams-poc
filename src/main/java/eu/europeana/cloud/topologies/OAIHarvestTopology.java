package eu.europeana.cloud.topologies;

import eu.europeana.cloud.commons.PropertiesUtil;
import eu.europeana.cloud.processors.oaiHarvestProcessors.HarvesterProcessor;
import eu.europeana.cloud.processors.oaiHarvestProcessors.HeaderExtractorProcessor;
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

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import static eu.europeana.cloud.commons.TopologyNodeNames.*;
import static eu.europeana.cloud.commons.TopologyPropertyKeys.KAFKA_HOSTS;
import static eu.europeana.cloud.commons.TopologyTopicNames.OAI_HARVEST_SOURCE_TOPIC_NAME;

public class OAIHarvestTopology {
    private static final Logger LOGGER = LoggerFactory.getLogger(OAIHarvestTopology.class);


    public static void main(String[] args) throws IOException {
        String providedPropertyFilename = "";
        if (args.length > 0) providedPropertyFilename = args[0];
        Properties props = readProperties(providedPropertyFilename);
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
                LOGGER.info("Starting oai harvest topology...");
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
            topology.addSource(OAI_HARVEST_TOPOLOGY_SOURCE_NAME, recordExecutionKeySerde.deserializer(), recordExecutionSerde.deserializer(), OAI_HARVEST_SOURCE_TOPIC_NAME);
            topology.addProcessor(OAI_HARVEST_HEADER_EXTRACTOR_PROCESSOR_NAME, () -> new HeaderExtractorProcessor(properties), OAI_HARVEST_TOPOLOGY_SOURCE_NAME);
            topology.addProcessor(OAI_HARVEST_HARVESTER_PROCESSOR_NAME, () -> new HarvesterProcessor(properties), OAI_HARVEST_HEADER_EXTRACTOR_PROCESSOR_NAME);
//            topology.addSink(OAI_HARVEST_DATABASE_TRANSFER_EXECUTION_RESULTS_SINK_NAME, DATABASE_TRANSFER_RECORD_EXECUTION_RESULT_TOPIC_NAME, recordExecutionKeySerde.serializer(), recordExecutionResultSerde.serializer(), OAI_HARVEST_HARVESTER_PROCESSOR_NAME);
//            topology.addSink(OAI_HARVEST_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME, DATABASE_TRANSFER_RECORD_EXECUTION_EXCEPTION_TOPIC_NAME, recordExecutionKeySerde.serializer(), recordExecutionExceptionSerde.serializer(),
//                    OAI_HARVEST_HEADER_EXTRACTOR_PROCESSOR_NAME, OAI_HARVEST_HARVESTER_PROCESSOR_NAME);

        }
        return topology;
    }

    private static Properties readProperties(String providedPropertyFilename) throws IOException {
        Properties properties = PropertiesUtil.getProperties("oaiTopology.properties", providedPropertyFilename);
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "oai-harvest-topology");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getProperty(KAFKA_HOSTS));
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return properties;
    }
}
