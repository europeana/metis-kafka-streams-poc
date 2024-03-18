package eu.europeana.cloud.processors.enrichmentProcessors;

import eu.europeana.cloud.commons.TopologyPropertyKeys;
import eu.europeana.cloud.dto.*;
import eu.europeana.cloud.exceptions.TaskDroppedException;
import eu.europeana.cloud.processors.commonProcessors.CommonProcessor;
import eu.europeana.enrichment.rest.client.EnrichmentWorker;
import eu.europeana.enrichment.rest.client.EnrichmentWorkerImpl;
import eu.europeana.enrichment.rest.client.dereference.DereferencerProvider;
import eu.europeana.enrichment.rest.client.enrichment.EnricherProvider;
import eu.europeana.enrichment.rest.client.exceptions.DereferenceException;
import eu.europeana.enrichment.rest.client.exceptions.EnrichmentException;
import eu.europeana.enrichment.rest.client.report.ProcessedResult;
import eu.europeana.enrichment.rest.client.report.Report;
import eu.europeana.enrichment.rest.client.report.Type;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class EnrichmentProcessor extends CommonProcessor implements Processor<RecordExecutionKey, RecordExecution, RecordExecutionKey, RecordExecutionProduct> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnrichmentProcessor.class);
    private final EnrichmentWorker enrichmentWorker;
    private ProcessorContext<RecordExecutionKey, RecordExecutionProduct> context;

    public EnrichmentProcessor(Properties enrichmentProperties) {
        super(enrichmentProperties);
        String dereferenceURL = enrichmentProperties.getProperty(TopologyPropertyKeys.DEREFERENCE_SERVICE_URL);
        String enrichmentEntityApiKey = enrichmentProperties.getProperty(TopologyPropertyKeys.ENTITY_API_KEY);
        String enrichmentEntityManagementUrl = enrichmentProperties.getProperty(TopologyPropertyKeys.ENTITY_MANAGEMENT_URL);
        String enrichmentEntityApiUrl = enrichmentProperties.getProperty(TopologyPropertyKeys.ENTITY_API_URL);
        EnricherProvider enricherProvider = new EnricherProvider();
        DereferencerProvider dereferencerProvider = new DereferencerProvider();
        enricherProvider.setEnrichmentPropertiesValues(enrichmentEntityManagementUrl, enrichmentEntityApiUrl, enrichmentEntityApiKey);
        dereferencerProvider.setDereferenceUrl(dereferenceURL);
        dereferencerProvider.setEnrichmentPropertiesValues(enrichmentEntityManagementUrl, enrichmentEntityApiUrl, enrichmentEntityApiKey);
        try {
            enrichmentWorker = new EnrichmentWorkerImpl(dereferencerProvider.create(), enricherProvider.create());
        } catch (DereferenceException | EnrichmentException e) {
            LOGGER.warn("Enrichment initialization error: {}", e.getMessage());
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
        if (!isTaskDropped(record.key().getExecutionId())) {
            LOGGER.info("Received enrichment topology record {}", record.key());
            ProcessedResult<String> results = enrichmentWorker.process(record.value().getRecordData());
            if (results.getRecordStatus() == ProcessedResult.RecordStatus.CONTINUE) {
//                context.forward(new Record<>(record.key(),
//                        new RecordExecutionResult(results.getProcessedRecord(),
//                                record.value().getExecutionName()),
//                        record.timestamp()), ENRICHMENT_DATABASE_TRANSFER_EXECUTION_RESULTS_SINK_NAME);
                insertRecordExecutionResult(record.key(), new RecordExecutionResult(results.getProcessedRecord(),
                        record.value().getExecutionName()));
                LOGGER.info("Enrichment ended successfully for record {}", record.key());
            } else {
                Set<Report> enrichmentErrorReports = results.getReport().stream().filter(report -> report.getMessageType() == Type.ERROR).collect(Collectors.toSet());
                String errorReportContent = enrichmentErrorReports.stream().map(Report::getMessage).collect(Collectors.joining("\n"));
                LOGGER.warn("Enrichment ended with error for record: key:{}, Reports: {}", record.key(), results.getReport());
//                context.forward(new Record<>(record.key(),
//                        new RecordExecutionException(record.value().getExecutionName(), EnrichmentException.class.getName(), errorReportContent),
//                        record.timestamp()), ENRICHMENT_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME);
                insertRecordExecutionException(record.key(), new RecordExecutionException(record.value().getExecutionName(), EnrichmentException.class.getName(), errorReportContent));
            }
        } else {
            LOGGER.warn("Task was dropped: key:{}", record.key());
//            context.forward(new Record<>(record.key(),
//                    new RecordExecutionException(record.value().getExecutionName(), TaskDroppedException.class.getName(), new TaskDroppedException().getMessage()),
//                    record.timestamp()), ENRICHMENT_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME);
            insertRecordExecutionException(record.key(), new RecordExecutionException(record.value().getExecutionName(), TaskDroppedException.class.getName(), new TaskDroppedException().getMessage()));
        }
    }

    @Override
    public void close() {
        Processor.super.close();
        closeDatabaseConnection();
    }
}
