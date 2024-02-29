package eu.europeana.cloud.processors.oaiHarvestProcessors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import eu.europeana.cloud.dto.database.*;
import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.HarvesterFactory;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecord;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import eu.europeana.metis.transformation.service.EuropeanaIdException;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static eu.europeana.cloud.commons.ExecutionPropertyKeys.*;
import static eu.europeana.cloud.commons.TopologyNodeNames.OAI_HARVEST_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME;
import static eu.europeana.cloud.commons.TopologyNodeNames.OAI_HARVEST_DATABASE_TRANSFER_EXECUTION_RESULTS_SINK_NAME;

public class HarvesterProcessor implements Processor<RecordExecutionKey, RecordExecution, RecordExecutionKey, RecordExecutionProduct> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterProcessor.class);
    private final OaiHarvester oaiHarvester = HarvesterFactory.createOaiHarvester();
    private final Gson gson = new Gson();
    private ProcessorContext<RecordExecutionKey, RecordExecutionProduct> context;

    @Override
    public void init(ProcessorContext<RecordExecutionKey, RecordExecutionProduct> context) {
        Processor.super.init(context);
        this.context = context;
    }

    @Override
    public void process(Record<RecordExecutionKey, RecordExecution> record) {
        LOGGER.info("Received oai topology record for harvesting. key : {} value: {}", record.key(), record.value());
        JsonObject executionParameters = record.value().getExecutionParameters();
        String oaiEndpoint = executionParameters.get(OAI_ENDPOINT_PROPERTY_KEY).getAsString();
        String oaiMetadataPrefix = executionParameters.get(OAI_METADATA_PREFIX_PROPERTY_KEY).getAsString();
        String oaiDataset = executionParameters.get(OAI_SET_PROPERTY_KEY).getAsString();
        OaiHarvest oaiHarvest = new OaiHarvest(oaiEndpoint, oaiMetadataPrefix, oaiDataset);
        OaiRecordHeader oaiRecordHeader = gson.fromJson(record.value().getRecordData(), OaiRecordHeader.class);
        try {
            OaiRecord oaiRecord = oaiHarvester.harvestRecord(oaiHarvest, oaiRecordHeader.getOaiIdentifier());
            LOGGER.info("Harvested oai record. key : {} value: {}", record.key(), record.value());
            RecordExecutionKey recordExecutionKey = record.key();
            String resultData = new String(oaiRecord.getRecord().readAllBytes(), StandardCharsets.UTF_8);
            recordExecutionKey.setRecordId(prepareEuropeanaGeneratedId(record, resultData));
            context.forward(new Record<>(
                    recordExecutionKey,
                    new RecordExecutionResult(resultData, record.value().getExecutionName()),
                    record.timestamp()), OAI_HARVEST_DATABASE_TRANSFER_EXECUTION_RESULTS_SINK_NAME);
        } catch (HarvesterException | IOException | EuropeanaIdException e) {
            LOGGER.warn("Error processing oai record. key : {} value: {}", record.key(), record.value(), e);
            context.forward(new Record<>(
                    record.key(),
                    new RecordExecutionException(record.value().getExecutionName(), e.getClass().getName(), e.getMessage()),
                    record.timestamp()), OAI_HARVEST_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME);
        }


    }

    private String prepareEuropeanaGeneratedId(Record<RecordExecutionKey, RecordExecution> record, String resultData) throws EuropeanaIdException {
        EuropeanaIdCreator europeanaIdCreator = new EuropeanaIdCreator();
        EuropeanaGeneratedIdsMap europeanaGeneratedIdsMap = europeanaIdCreator.constructEuropeanaId(resultData, record.key().getDatasetId());
        return europeanaGeneratedIdsMap.getEuropeanaGeneratedId();
    }

    @Override
    public void close() {
        Processor.super.close();
    }
}
