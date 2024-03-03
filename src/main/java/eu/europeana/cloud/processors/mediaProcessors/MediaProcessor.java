package eu.europeana.cloud.processors.mediaProcessors;

import eu.europeana.cloud.dto.*;
import eu.europeana.metis.mediaprocessing.*;
import eu.europeana.metis.mediaprocessing.exception.MediaExtractionException;
import eu.europeana.metis.mediaprocessing.exception.MediaProcessorException;
import eu.europeana.metis.mediaprocessing.exception.RdfDeserializationException;
import eu.europeana.metis.mediaprocessing.exception.RdfSerializationException;
import eu.europeana.metis.mediaprocessing.model.EnrichedRdf;
import eu.europeana.metis.mediaprocessing.model.RdfResourceEntry;
import eu.europeana.metis.mediaprocessing.model.ResourceExtractionResult;
import eu.europeana.metis.mediaprocessing.model.Thumbnail;
import org.apache.commons.collections.CollectionUtils;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import static eu.europeana.cloud.commons.TopologyNodeNames.MEDIA_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME;
import static eu.europeana.cloud.commons.TopologyNodeNames.MEDIA_DATABASE_TRANSFER_EXECUTION_RESULTS_SINK_NAME;
import static java.util.Objects.nonNull;

public class MediaProcessor implements Processor<RecordExecutionKey, RecordExecution, RecordExecutionKey, RecordExecutionProduct> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaProcessor.class);
    private final MediaExtractor mediaExtractor;
    private final RdfSerializer rdfSerializer;
    private final RdfDeserializer rdfDeserializer;
    private ProcessorContext<RecordExecutionKey, RecordExecutionProduct> context;

    public MediaProcessor(Properties properties) {
        try {
            RdfConverterFactory rdfConverterFactory = new RdfConverterFactory();
            rdfDeserializer = rdfConverterFactory.createRdfDeserializer();
            rdfSerializer = rdfConverterFactory.createRdfSerializer();
            MediaProcessorFactory mediaProcessorFactory = new MediaProcessorFactory();
            mediaExtractor = mediaProcessorFactory.createMediaExtractor();
        } catch (MediaProcessorException e) {
            LOGGER.error("Error during initialization of MediaProcessor", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void init(ProcessorContext<RecordExecutionKey, RecordExecutionProduct> context) {
        Processor.super.init(context);
        this.context = context;
    }

    @Override
    public void process(Record<RecordExecutionKey, RecordExecution> record) {
        final byte[] rdfBytes = record.value().getRecordData().getBytes(StandardCharsets.UTF_8);
        final EnrichedRdf enrichedRdf;

        try {
            enrichedRdf = getEnrichedRdf(rdfBytes);

            RdfResourceEntry resourceMainThumbnail;
            resourceMainThumbnail = rdfDeserializer.getMainThumbnailResourceForMediaExtraction(rdfBytes);

            boolean hasMainThumbnail = false;
            if (resourceMainThumbnail != null) {
                LOGGER.info("Processed record contain main thumbnail: {}", resourceMainThumbnail.getResourceUrl());
                hasMainThumbnail = processResourceWithoutThumbnail(resourceMainThumbnail,
                        record.key().getRecordId(), enrichedRdf, mediaExtractor);
            }
            List<RdfResourceEntry> remainingResourcesList;
            remainingResourcesList = rdfDeserializer.getRemainingResourcesForMediaExtraction(rdfBytes);
            if (hasMainThumbnail) {
                remainingResourcesList.forEach(entry ->
                        processResourceWithThumbnail(entry, record.key().getRecordId(), enrichedRdf,
                                mediaExtractor)
                );
            } else {
                remainingResourcesList.forEach(entry ->
                        processResourceWithoutThumbnail(entry, record.key().getRecordId(), enrichedRdf,
                                mediaExtractor)
                );
            }
            String resultFileData = new String(getOutputRdf(enrichedRdf), StandardCharsets.UTF_8);
            context.forward(new Record<>(record.key(),
                    new RecordExecutionResult(resultFileData, record.value().getExecutionName()),
                    record.timestamp()), MEDIA_DATABASE_TRANSFER_EXECUTION_RESULTS_SINK_NAME);
        } catch (RdfDeserializationException | RdfSerializationException e) {
            LOGGER.warn("Exception during enrichment of record: key:{}, Exception e: {}", record.key(), e);
            context.forward(new Record<>(record.key(),
                    new RecordExecutionException(record.value().getExecutionName(), e.getClass().getName(), e.getMessage()),
                    record.timestamp()), MEDIA_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME);
        }
    }


    private EnrichedRdf getEnrichedRdf(byte[] rdfBytes) throws RdfDeserializationException {
        return rdfDeserializer.getRdfForResourceEnriching(rdfBytes);
    }

    private byte[] getOutputRdf(EnrichedRdf rdfForEnrichment) throws RdfSerializationException {
        return rdfSerializer.serialize(rdfForEnrichment);
    }

    private boolean processResourceWithThumbnail(RdfResourceEntry resourceToProcess, String recordId,
                                                 EnrichedRdf rdfForEnrichment, MediaExtractor extractor) {
        return processResource(resourceToProcess, recordId, rdfForEnrichment, extractor, true);
    }

    private boolean processResourceWithoutThumbnail(RdfResourceEntry resourceToProcess, String recordId,
                                                    EnrichedRdf rdfForEnrichment, MediaExtractor extractor) {
        return processResource(resourceToProcess, recordId, rdfForEnrichment, extractor, false);
    }

    private boolean processResource(RdfResourceEntry resourceToProcess, String recordId,
                                    EnrichedRdf rdfForEnrichment, MediaExtractor extractor, boolean gotMainThumbnail) {
        ResourceExtractionResult extraction;
        boolean successful = false;

        try {
            // Perform media extraction
            extraction = extractor.performMediaExtraction(resourceToProcess, gotMainThumbnail);

            // Check if extraction for media was successful
            successful = extraction != null;
            LOGGER.info("Extraction of resource {} was successful: {}", resourceToProcess.getResourceUrl(), successful);
            // If successful then store data
            if (successful) {
                rdfForEnrichment.enrichResource(extraction.getMetadata());
                if (!CollectionUtils.isEmpty(extraction.getThumbnails())) {
                    storeThumbnails(recordId, extraction.getThumbnails());
                }
            }

        } catch (MediaExtractionException e) {
            LOGGER.warn("Error while extracting media for record {}. ", recordId, e);
        }

        return successful;
    }

    private void storeThumbnails(String recordId, List<Thumbnail> thumbnails) {
        if (nonNull(thumbnails)) {
            LOGGER.debug("Fake storing thumbnail");
        }
    }

    @Override
    public void close() {
        Processor.super.close();
    }
}
