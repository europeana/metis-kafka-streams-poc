package eu.europeana.cloud.processors.oaiHarvestProcessors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import eu.europeana.cloud.dto.database.RecordExecution;
import eu.europeana.cloud.dto.database.RecordExecutionException;
import eu.europeana.cloud.dto.database.RecordExecutionKey;
import eu.europeana.cloud.dto.database.RecordExecutionProduct;
import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.HarvesterFactory;
import eu.europeana.metis.harvesting.ReportingIteration;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeaderIterator;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static eu.europeana.cloud.commons.ExecutionPropertyKeys.*;
import static eu.europeana.cloud.commons.TopologyNodeNames.OAI_HARVEST_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME;
import static eu.europeana.cloud.commons.TopologyNodeNames.OAI_HARVEST_HARVESTER_PROCESSOR_NAME;

public class HeaderExtractorProcessor implements Processor<RecordExecutionKey, RecordExecutionProduct, RecordExecutionKey, RecordExecutionProduct> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderExtractorProcessor.class);
    private final OaiHarvester oaiHarvester = HarvesterFactory.createOaiHarvester();
    private final Gson gson = new Gson();
    private ProcessorContext<RecordExecutionKey, RecordExecutionProduct> context;

    @Override
    public void init(ProcessorContext<RecordExecutionKey, RecordExecutionProduct> context) {
        Processor.super.init(context);
        this.context = context;
    }

    @Override
    public void process(Record<RecordExecutionKey, RecordExecutionProduct> record) {
        LOGGER.info("Received oai topology record. key : {} value: {}", record.key(), record.value());
        RecordExecution recordExecution = (RecordExecution) record.value();
        JsonObject executionParameters = recordExecution.getExecutionParameters();
        String oaiEndpoint = executionParameters.get(OAI_ENDPOINT_PROPERTY_KEY).getAsString();
        String oaiMetadataPrefix = executionParameters.get(OAI_METADATA_PREFIX_PROPERTY_KEY).getAsString();
        String oaiDataset = executionParameters.get(OAI_SET_PROPERTY_KEY).getAsString();

        try (OaiRecordHeaderIterator oaiRecordHeaderIterator = oaiHarvester.harvestRecordHeaders(new OaiHarvest(oaiEndpoint, oaiMetadataPrefix, oaiDataset))) {
            oaiRecordHeaderIterator.forEach(oaiRecordHeader -> {
                RecordExecution recordWithHeader = recordExecution;
                recordWithHeader.setRecordData(gson.toJson(oaiRecordHeader));
                context.forward(new Record<>(
                        record.key(),
                        recordWithHeader,
                        record.timestamp()
                ), OAI_HARVEST_HARVESTER_PROCESSOR_NAME);
                return ReportingIteration.IterationResult.CONTINUE;
            });
        } catch (HarvesterException | IOException e) {
            LOGGER.warn("Error processing oai record. key : {} value: {}", record.key(), record.value(), e);
            context.forward(new Record<>(
                    record.key(),
                    new RecordExecutionException(recordExecution.getExecutionName(), e.getClass().getName(), e.getMessage()),
                    record.timestamp()), OAI_HARVEST_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME);
        }

    }

    @Override
    public void close() {
        Processor.super.close();
    }
}
