package eu.europeana.cloud.processors;

import com.github.f4b6a3.uuid.UuidCreator;
import eu.europeana.cloud.common.model.Representation;
import eu.europeana.cloud.commons.TopologyMetadataPropertyKeys;
import eu.europeana.cloud.dto.ErrorInformation;
import eu.europeana.cloud.dto.Message;
import eu.europeana.cloud.service.dps.storm.utils.UUIDWrapper;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

import static eu.europeana.cloud.commons.TopologyNodeNames.MEDIA_INTERMEDIATE_SINK_NAME;

public class WriteRecordProcessor extends MediaProcessor implements
        Processor<String, Message, String, Message> {
    private static final Logger LOGGER = LoggerFactory.getLogger(WriteRecordProcessor.class);

    private ProcessorContext<String, Message> context;

    public WriteRecordProcessor(Properties topologyProperties) {
        super(topologyProperties);
    }

    @Override
    public void init(ProcessorContext<String, Message> context) {
        Processor.super.init(context);
        this.context = context;
    }

    @Override
    public void process(Record<String, Message> record) {
        if (!isTaskDropped(record.value().getTaskId())) {
            try {
                LOGGER.info("Received key: {} value: {}", record.key(), record.value());
                Properties properties = record.value().getMetadataProperties();

                String markedAsDeleted = properties.getProperty(TopologyMetadataPropertyKeys.MARKED_AS_DELETED);
                UUID newVersion = UUIDWrapper.generateRepresentationVersion(
                        new Date(record.timestamp()).toInstant(),
                        record.value().getFileUrl());
                String newFilename =
                        (record.value().getOutputFilename()) != null ?
                                record.value().getOutputFilename() :
                                UuidCreator.getNameBasedMd5(record.value().getTaskId()).toString();
                String outputDatasetId = record.value().getOutputDataset();
                Representation rep = recordServiceClient.getRepresentation(
                        properties.getProperty(TopologyMetadataPropertyKeys.CLOUD_ID),
                        properties.getProperty(TopologyMetadataPropertyKeys.REPRESENTATION),
                        properties.getProperty(TopologyMetadataPropertyKeys.VERSION));
                LOGGER.info("Representation: {}", rep);
                if (rep != null) {
                    String dataProvider = rep.getDataProvider();
                    URI outputURI;
                    if (properties.getProperty(TopologyMetadataPropertyKeys.MARKED_AS_DELETED) != null && Boolean.parseBoolean(markedAsDeleted)) {
                        outputURI = recordServiceClient.createRepresentation(
                                properties.getProperty(TopologyMetadataPropertyKeys.CLOUD_ID),
                                properties.getProperty(TopologyMetadataPropertyKeys.REPRESENTATION),
                                dataProvider,
                                newVersion,
                                outputDatasetId);
                    } else {
                        outputURI = recordServiceClient.createRepresentation(
                                properties.getProperty(TopologyMetadataPropertyKeys.CLOUD_ID),
                                properties.getProperty(TopologyMetadataPropertyKeys.REPRESENTATION),
                                dataProvider,
                                newVersion,
                                outputDatasetId,
                                new ByteArrayInputStream(record.value().getEnrichedFileData()),
                                newFilename,
                                "text/xml"
                        );
                    }
                    properties.remove(TopologyMetadataPropertyKeys.CLOUD_ID);
                    properties.remove(TopologyMetadataPropertyKeys.REPRESENTATION);
                    properties.remove(TopologyMetadataPropertyKeys.VERSION);

                    // I kinda don't get from does list of revision to be applied comes, so I need to discuss it to implement it
                    // TODO
                    // Implement revision writer bolt functionality

                    Message message = record.value();
                    message.setMetadataProperties(properties);

                    context.forward(new Record<>(record.value().getTaskId(), message, record.timestamp()),
                            MEDIA_INTERMEDIATE_SINK_NAME);
                } else {
                    LOGGER.error("Couldn't find representation for could id: {}, representation: {}, version: {}",
                            properties.getProperty(TopologyMetadataPropertyKeys.CLOUD_ID),
                            properties.getProperty(TopologyMetadataPropertyKeys.REPRESENTATION),
                            properties.getProperty(TopologyMetadataPropertyKeys.VERSION));
                }
            } catch (Exception e) {
                LOGGER.error("Error during writing result record");
                context.forward(new Record<>(record.value().getTaskId(), Message.builder()
                                .errorInformation(new ErrorInformation(e.getMessage(), "Error during writing result")).build(), record.timestamp()),
                        MEDIA_INTERMEDIATE_SINK_NAME);
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
