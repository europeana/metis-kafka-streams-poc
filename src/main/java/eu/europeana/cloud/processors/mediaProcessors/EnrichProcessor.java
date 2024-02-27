package eu.europeana.cloud.processors.mediaProcessors;

import com.google.gson.reflect.TypeToken;
import eu.europeana.cloud.commons.TopologyMetadataPropertyKeys;
import eu.europeana.cloud.commons.TopologyNodeNames;
import eu.europeana.cloud.dto.queues.ErrorInformation;
import eu.europeana.cloud.dto.queues.Message;
import eu.europeana.cloud.service.commons.urls.UrlParser;
import eu.europeana.metis.mediaprocessing.exception.RdfDeserializationException;
import eu.europeana.metis.mediaprocessing.exception.RdfSerializationException;
import eu.europeana.metis.mediaprocessing.model.EnrichedRdf;
import eu.europeana.metis.mediaprocessing.model.ResourceMetadata;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import static eu.europeana.cloud.commons.TopologyNodeNames.MEDIA_INTERMEDIATE_SINK_NAME;

public class EnrichProcessor extends MediaProcessor implements
        Processor<String, Message, String, Message> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnrichProcessor.class);
    private ProcessorContext<String, Message> context;

    public EnrichProcessor(Properties topologyProperties) {
        super(topologyProperties);
    }

    @Override
    public void init(ProcessorContext<String, Message> context) {
        Processor.super.init(context);
        this.context = context;
    }

    @Override
    public void process(Record<String, Message> record) {
        LOGGER.info("Received key: {} value: {}", record.key(), record.value());
        if (!isTaskDropped(record.value().getTaskId())) {
            try {
                String fileContent = record.value().getSourceFileData();
                EnrichedRdf enrichedRdf = rdfDeserializer.getRdfForResourceEnriching(fileContent.getBytes(StandardCharsets.UTF_8));
                Properties recordProperties = record.value().getMetadataProperties();
                ResourceMetadata mainMetadata = gson.fromJson(recordProperties.getProperty(TopologyMetadataPropertyKeys.MAIN_ENTRY_METADATA), ResourceMetadata.class);
                if (mainMetadata != null) {
                    enrichedRdf.enrichResource(mainMetadata);
                }
                LOGGER.info("Enriched main resource");
                List<ResourceMetadata> otherMetadata = gson.fromJson(recordProperties.getProperty(TopologyMetadataPropertyKeys.REST_ENTRIES_METADATA), new TypeToken<List<ResourceMetadata>>() {
                }.getType());
                for (ResourceMetadata metadata
                        : otherMetadata) {
                    enrichedRdf.enrichResource(metadata);
                }
                LOGGER.info("Enriched rest of resources");
                byte[] enrichedFile = rdfSerializer.serialize(enrichedRdf);
                recordProperties.remove(TopologyMetadataPropertyKeys.MAIN_ENTRY_METADATA);
                recordProperties.remove(TopologyMetadataPropertyKeys.REST_ENTRIES_METADATA);

                UrlParser urlParser = new UrlParser(record.value().getFileUrl());
                // For some reason I can't access UrlPart that is public enum defined in ecloud?
                // For now I leave it commented but it require some discussion why it might be the case
//                recordProperties.put(TopologyMetadataPropertyKeys.CLOUD_ID, urlParser.getPart(UrlPart.RECORDS));
//                recordProperties.put(TopologyMetadataPropertyKeys.REPRESENTATION, urlParser.getPart(UrlPart.REPRESENTATIONS));
//                recordProperties.put(TopologyMetadataPropertyKeys.VERSION, urlParser.getPart(UrlPart.VERSIONS));

                recordProperties.put(TopologyMetadataPropertyKeys.CLOUD_ID, "F4VSSQKDRUEECT7OUSLLLBTLPI2SD57P7R5E6JCBOENATL3DVRFA");
                recordProperties.put(TopologyMetadataPropertyKeys.REPRESENTATION, "metadataRecord");
                recordProperties.put(TopologyMetadataPropertyKeys.VERSION, "3ef665b0-b933-11ee-8000-4e008e54a287");

                Message message = record.value();
                message.setMetadataProperties(recordProperties);
                message.setEnrichedFileData(enrichedFile);
                context.forward(new Record<>(record.key(), message, record.timestamp()),
                        TopologyNodeNames.MEDIA_RECORD_WRITER_PROCESSOR_NAME);
            } catch (RdfDeserializationException e) {
                LOGGER.error("Error during deserialization", e);
                context.forward(new Record<>(record.value().getTaskId(), Message.builder()
                                .errorInformation(new ErrorInformation(e.getMessage(), "Error during deserialization")).build(), record.timestamp()),
                        MEDIA_INTERMEDIATE_SINK_NAME);
            } catch (RdfSerializationException e) {
                LOGGER.error("Error during serialization", e);
                context.forward(new Record<>(record.value().getTaskId(), Message.builder()
                                .errorInformation(new ErrorInformation(e.getMessage(), "Error during serialization")).build(), record.timestamp()),
                        MEDIA_INTERMEDIATE_SINK_NAME);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            LOGGER.info("Task with id {} is dropped", record.value().getTaskId());
        }

    }

    @Override
    public void close() {
        Processor.super.close();
    }
}
