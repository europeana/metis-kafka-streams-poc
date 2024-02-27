package eu.europeana.cloud.processors.mediaProcessors;

import com.google.gson.reflect.TypeToken;
import eu.europeana.cloud.commons.TopologyConstants;
import eu.europeana.cloud.commons.TopologyMetadataPropertyKeys;
import eu.europeana.cloud.dto.queues.ErrorInformation;
import eu.europeana.cloud.dto.queues.Message;
import eu.europeana.metis.mediaprocessing.exception.MediaExtractionException;
import eu.europeana.metis.mediaprocessing.model.RdfResourceEntry;
import eu.europeana.metis.mediaprocessing.model.ResourceExtractionResult;
import eu.europeana.metis.mediaprocessing.model.ResourceMetadata;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static eu.europeana.cloud.commons.TopologyNodeNames.MEDIA_ENRICH_PROCESSOR_NAME;
import static eu.europeana.cloud.commons.TopologyNodeNames.MEDIA_INTERMEDIATE_SINK_NAME;

public class MediaExtractorProcessor extends MediaProcessor implements
        Processor<String, Message, String, Message> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaExtractorProcessor.class);
    private ProcessorContext<String, Message> context;

    public MediaExtractorProcessor(Properties topologyProperties) {
        super(topologyProperties);
    }

    private static long getThrottling(Record<String, Message> record) {
        long sleepTime = 0L;
        String throttleLevel = record.value().getThrottleLevel();
        if (throttleLevel != null) {
            switch (throttleLevel) {
                case "MEDIUM" -> sleepTime = TopologyConstants.MEDIUM_TROTTING_SLEEP_TIME_IN_SECONDS * 1000;
                case "LOW" -> sleepTime = TopologyConstants.LOW_TROTTING_SLEEP_TIME_IN_SECONDS * 1000;
                case "HIGH" -> sleepTime = TopologyConstants.HIGH_TROTTING_SLEEP_TIME_IN_SECONDS * 1000;
                default -> sleepTime = TopologyConstants.MEDIUM_TROTTING_SLEEP_TIME_IN_SECONDS * 1000;
            }
        }
        return sleepTime;
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
            Properties messageProperties = record.value().getMetadataProperties();
            long sleepTime = getThrottling(record);
            RdfResourceEntry mainEntry = gson.fromJson(messageProperties.getProperty(TopologyMetadataPropertyKeys.MAIN_ENTRY_URL), RdfResourceEntry.class);
            List<RdfResourceEntry> otherEntries = gson.fromJson(messageProperties.getProperty(TopologyMetadataPropertyKeys.REST_ENTRIES_URL), new TypeToken<List<RdfResourceEntry>>() {
            }.getType());
            try {
                boolean mainThumbnailAvailable = false;

                if (mainEntry != null) {
                    mainThumbnailAvailable = extractMainMediaResource(record, messageProperties, sleepTime, mainEntry, mainThumbnailAvailable);
                }

                List<ResourceMetadata> otherEntriesMetadata = new ArrayList<>();
                for (RdfResourceEntry resourceEntry :
                        otherEntries) {
                    ResourceMetadata resourceMetadata = extractMediaResource(record, messageProperties, sleepTime, mainThumbnailAvailable, resourceEntry);
                    if (resourceMetadata != null) {
                        otherEntriesMetadata.add(resourceMetadata);
                    }
                }
                messageProperties.put(TopologyMetadataPropertyKeys.REST_ENTRIES_METADATA, gson.toJson(otherEntriesMetadata));

                messageProperties.remove(TopologyMetadataPropertyKeys.MAIN_ENTRY_URL);
                messageProperties.remove(TopologyMetadataPropertyKeys.REST_ENTRIES_URL);

                Message message = record.value();
                message.setMetadataProperties(messageProperties);

                context.forward(new Record<>(record.key(), message, record.timestamp()),
                        MEDIA_ENRICH_PROCESSOR_NAME);
            } catch (Exception e) {
                LOGGER.error("Error in media extractor: {}", e.getMessage());
                context.forward(new Record<>(record.value().getTaskId(), Message.builder()
                                .errorInformation(new ErrorInformation(e.getMessage(), "Error in media extractor")).build(), record.timestamp()),
                        MEDIA_INTERMEDIATE_SINK_NAME);
            }
        } else {
            LOGGER.info("Task with id {} is dropped", record.value().getTaskId());
        }
    }

    private boolean extractMainMediaResource(Record<String, Message> record, Properties messageProperties, long sleepTime,
                                             RdfResourceEntry mainEntry, boolean mainThumbnailAvailable) throws IOException, InterruptedException {

        Thread.sleep(sleepTime);
        try {
            ResourceExtractionResult mainExtractionResult = null;
            mainExtractionResult = mediaExtractor.performMediaExtraction(mainEntry, mainThumbnailAvailable);
            if (mainExtractionResult != null) {
                LOGGER.info("Main thumbnail extraction result: {}", mainExtractionResult.getMetadata());
                Set<String> thumbnailTargetNames = null;
                String metadataJson = null;
                if (mainExtractionResult.getMetadata() != null) {
                    metadataJson = gson.toJson(mainExtractionResult.getMetadata());
                    messageProperties.put(TopologyMetadataPropertyKeys.MAIN_ENTRY_METADATA, metadataJson);
                    thumbnailTargetNames = mainExtractionResult.getMetadata().getThumbnailTargetNames();
                    mainThumbnailAvailable = !thumbnailTargetNames.isEmpty();
                }

//        thumbnailUploader.storeThumbnails(StormTaskTuple.builder().taskId(
//                Integer.parseInt(record.value().getTaskId())).build(),
//            new StringBuilder(), mainExtractionResult);
            }
        } catch (MediaExtractionException e) {
            LOGGER.error("Error during main media extraction: {}", e.getMessage());
            context.forward(new Record<>(record.value().getTaskId(), Message.builder()
                            .metadataProperties(messageProperties)
                            .errorInformation(new ErrorInformation(e.getMessage(), "Error during media extraction")).build(), record.timestamp()),
                    MEDIA_INTERMEDIATE_SINK_NAME);
        }
        return mainThumbnailAvailable;
    }

    private ResourceMetadata extractMediaResource(Record<String, Message> record, Properties messageProperties, long sleepTime,
                                                  boolean mainThumbnailAvailable, RdfResourceEntry resourceEntry) throws InterruptedException, IOException {
        Thread.sleep(sleepTime);
        try {
            ResourceExtractionResult resourceExtractionResult =
                    mediaExtractor.performMediaExtraction(
                            resourceEntry,
                            mainThumbnailAvailable
                    );
            if (resourceExtractionResult != null) {
                LOGGER.info("Other media resource extraction result: {}", resourceExtractionResult.getMetadata());
                return resourceExtractionResult.getMetadata();
            }
//      thumbnailUploader.storeThumbnails(StormTaskTuple.builder().taskId(Integer.parseInt(record.value().getTaskId())).build(),
//          new StringBuilder(), resourceExtractionResult);
        } catch (MediaExtractionException e) {
            LOGGER.error("Error during other media extraction: {}", e.getMessage());
            context.forward(new Record<>(record.key(), Message.builder()
                            .metadataProperties(messageProperties)
                            .errorInformation(new ErrorInformation(e.getMessage(), "Error during media extraction")).build(), record.timestamp()),
                    MEDIA_INTERMEDIATE_SINK_NAME);
        }
        return null;
    }

    @Override
    public void close() {
        Processor.super.close();
    }
}
