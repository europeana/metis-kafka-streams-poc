package eu.europeana.cloud.processors.mediaProcessors;

import eu.europeana.cloud.commons.TopologyMetadataPropertyKeys;
import eu.europeana.cloud.dto.queues.ErrorInformation;
import eu.europeana.cloud.dto.queues.Message;
import eu.europeana.metis.mediaprocessing.exception.RdfDeserializationException;
import eu.europeana.metis.mediaprocessing.model.RdfResourceEntry;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;

import static eu.europeana.cloud.commons.TopologyNodeNames.MEDIA_INTERMEDIATE_SINK_NAME;
import static eu.europeana.cloud.commons.TopologyNodeNames.MEDIA_RESOURCE_EXTRACTOR_PROCESSOR_NAME;

public class RdfResourceProcessor extends MediaProcessor implements Processor<String, Message, String, Message> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RdfResourceProcessor.class);

    private ProcessorContext<String, Message> context;

    public RdfResourceProcessor(Properties properties) {
        super(properties);
    }

    @Override
    public void init(ProcessorContext<String, Message> context) {
        this.context = context;
    }

    @Override
    public void process(Record<String, Message> processedRecord) {
        if (!isTaskDropped(processedRecord.value().getTaskId())) {
            try {
                LOGGER.info("Received key: {} value: {}", processedRecord.key(), processedRecord.value());
                Properties messageProperties = new Properties();
                byte[] fileData = processedRecord.value().getSourceFileData().getBytes();
                RdfResourceEntry mainEntry = rdfDeserializer.getMainThumbnailResourceForMediaExtraction(fileData);
                LOGGER.info("Main entry resource: {}", mainEntry);
                List<RdfResourceEntry> allEntries = rdfDeserializer.getRemainingResourcesForMediaExtraction(fileData);
                LOGGER.info("Other entry resources: {}", allEntries);
                messageProperties.put(TopologyMetadataPropertyKeys.MAIN_ENTRY_URL, gson.toJson(mainEntry));
                messageProperties.put(TopologyMetadataPropertyKeys.REST_ENTRIES_URL, gson.toJson(allEntries));

                Message message = processedRecord.value();
                message.setMetadataProperties(messageProperties);
                context.forward(new Record<>(processedRecord.key(), message, processedRecord.timestamp()),
                        MEDIA_RESOURCE_EXTRACTOR_PROCESSOR_NAME);
            } catch (RdfDeserializationException e) {
                LOGGER.info("Error during rdfResourceLinkProcess: {}", e.getMessage());
                context.forward(new Record<>(processedRecord.value().getTaskId(), Message.builder()
                                .errorInformation(new ErrorInformation(e.getMessage(), "Error during rdfResourceLinkProcess")).build(), processedRecord.timestamp()),
                        MEDIA_INTERMEDIATE_SINK_NAME);
            }
        } else {
            LOGGER.info("Task with id {} is dropped", processedRecord.value().getTaskId());
        }
    }

    @Override
    public void close() {
        Processor.super.close();
    }

}
