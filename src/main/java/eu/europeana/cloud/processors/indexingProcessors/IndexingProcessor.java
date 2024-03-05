package eu.europeana.cloud.processors.indexingProcessors;

import com.google.gson.JsonObject;
import eu.europeana.cloud.dto.RecordExecution;
import eu.europeana.cloud.dto.RecordExecutionException;
import eu.europeana.cloud.dto.RecordExecutionKey;
import eu.europeana.cloud.dto.RecordExecutionProduct;
import eu.europeana.cloud.exceptions.TaskDroppedException;
import eu.europeana.cloud.exceptions.TaskNotSuitableForPublicationException;
import eu.europeana.cloud.processors.commonProcessors.CommonProcessor;
import eu.europeana.cloud.service.dps.metis.indexing.TargetIndexingDatabase;
import eu.europeana.cloud.service.dps.service.utils.indexing.IndexWrapper;
import eu.europeana.indexing.IndexingProperties;
import eu.europeana.indexing.exception.IndexingException;
import eu.europeana.indexing.tiers.model.MediaTier;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static eu.europeana.cloud.commons.ExecutionPropertyKeys.*;
import static eu.europeana.cloud.commons.TopologyNodeNames.INDEXING_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME;
import static eu.europeana.cloud.commons.TopologyNodeNames.INDEXING_DATABASE_TRANSFER_EXECUTION_RESULTS_SINK_NAME;

public class IndexingProcessor extends CommonProcessor implements Processor<RecordExecutionKey, RecordExecution, RecordExecutionKey, RecordExecutionProduct> {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexingProcessor.class);
    private ProcessorContext<RecordExecutionKey, RecordExecutionProduct> context;
    private final IndexWrapper indexWrapper;
    private IndexingProperties executionIndexingProperties;

    public IndexingProcessor(Properties properties) {
        super(properties);
        this.indexWrapper = IndexWrapper.getInstance(properties);
        LOGGER.info("Created index wrapper");
    }

    @Override
    public void init(ProcessorContext<RecordExecutionKey, RecordExecutionProduct> context) {
        Processor.super.init(context);
        this.context = context;
    }

    @Override
    public void process(Record<RecordExecutionKey, RecordExecution> record) {
        if (isTaskDropped(record.key().getExecutionId())) {
            JsonObject taskParameters = record.value().getExecutionParameters();
            prepareExecutionIndexingProperties(taskParameters);

            try {
                AtomicBoolean suitableForPublication = null;
                suitableForPublication = indexRecord(record, taskParameters);
                if (suitableForPublication.get()) {
                    LOGGER.info("Record {} was indexed properly", record.key());
                    context.forward(record, INDEXING_DATABASE_TRANSFER_EXECUTION_RESULTS_SINK_NAME);
                } else {
                    removeIndexedRecords(record.key().getRecordId(), TargetIndexingDatabase.valueOf(taskParameters.get(INDEXING_TARGET_DATABASE).getAsString()));
                    throw new TaskNotSuitableForPublicationException();
                }
            } catch (IndexingException | TaskNotSuitableForPublicationException e) {
                LOGGER.warn("Record {} was not indexed due to: {}", record.key(), e.getMessage());
                context.forward(new Record<>(record.key(),
                        new RecordExecutionException(record.value().getExecutionName(), e.getClass().getName(), e.getMessage()),
                        record.timestamp()), INDEXING_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME);
            }
        } else {
            LOGGER.warn("Task was dropped: key:{}", record.key());
            context.forward(new Record<>(record.key(),
                    new RecordExecutionException(record.value().getExecutionName(), TaskDroppedException.class.getName(), new TaskDroppedException().getMessage()),
                    record.timestamp()), INDEXING_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME);
        }
    }

    @NotNull
    private AtomicBoolean indexRecord(Record<RecordExecutionKey, RecordExecution> record, JsonObject taskParameters) throws IndexingException {
        AtomicBoolean suitableForPublication = new AtomicBoolean();
        String recordData = record.value().getRecordData();
        TargetIndexingDatabase database = TargetIndexingDatabase.valueOf(taskParameters.get(INDEXING_TARGET_DATABASE).getAsString());
        indexWrapper.getIndexer(database).index(recordData, executionIndexingProperties, tier -> {
            suitableForPublication.set((database == TargetIndexingDatabase.PREVIEW) || (tier.getMetadataTier() != MediaTier.T0));
            return suitableForPublication.get();
        });
        return suitableForPublication;
    }

    private void removeIndexedRecords(String recordsId, TargetIndexingDatabase database) throws IndexingException {
        LOGGER.debug("Removing indexed record with recordsId: {} from database for {}", recordsId, database.toString());
        indexWrapper.getIndexer(database).remove(recordsId);
    }

    private void prepareExecutionIndexingProperties(JsonObject taskParameters) {
        Date date = (taskParameters.get(INDEXING_RECORD_DATE).isJsonNull() ? new Date() : new Date(taskParameters.get(INDEXING_RECORD_DATE).getAsString()));
        Boolean preserveTimestamps = (!taskParameters.get(INDEXING_PRESERVE_TIMESTAMPS).isJsonNull() && taskParameters.get(INDEXING_PRESERVE_TIMESTAMPS).getAsBoolean());
        List<String> datasetIdsForRedirection = (taskParameters.get(INDEXING_DATASET_IDS_FOR_REDIRECTION).isJsonNull() ? Collections.emptyList() :
                Arrays.stream(taskParameters.get(INDEXING_DATASET_IDS_FOR_REDIRECTION)
                        .getAsString().split(",")).map(String::trim).toList());
        Boolean isPerformRedirects = (!taskParameters.get(INDEXING_PERFORM_REDIRECTS).isJsonNull() && taskParameters.get(INDEXING_PERFORM_REDIRECTS).getAsBoolean());
        executionIndexingProperties = new IndexingProperties(date, preserveTimestamps, datasetIdsForRedirection, isPerformRedirects, true);
    }

    @Override
    public void close() {
        Processor.super.close();
        closeDatabaseConnection();
        indexWrapper.close();
    }
}
