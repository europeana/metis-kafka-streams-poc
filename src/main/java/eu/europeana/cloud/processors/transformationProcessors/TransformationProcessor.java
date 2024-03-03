package eu.europeana.cloud.processors.transformationProcessors;

import com.google.gson.JsonObject;
import eu.europeana.cloud.dto.*;
import eu.europeana.cloud.exceptions.TaskDroppedException;
import eu.europeana.cloud.processors.commonProcessors.CommonProcessor;
import eu.europeana.metis.transformation.service.*;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static eu.europeana.cloud.commons.ExecutionPropertyKeys.*;
import static eu.europeana.cloud.commons.TopologyNodeNames.TRANSFORMATION_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME;
import static eu.europeana.cloud.commons.TopologyNodeNames.TRANSFORMATION_DATABASE_TRANSFER_EXECUTION_RESULTS_SINK_NAME;

public class TransformationProcessor extends CommonProcessor implements Processor<RecordExecutionKey, RecordExecution, RecordExecutionKey, RecordExecutionProduct> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransformationProcessor.class);
    private ProcessorContext<RecordExecutionKey, RecordExecutionProduct> context;

    public TransformationProcessor(Properties properties) {
        super(properties);
    }

    @Override
    public void init(ProcessorContext<RecordExecutionKey, RecordExecutionProduct> context) {
        Processor.super.init(context);
        this.context = context;
    }

    @Override
    public void process(Record<RecordExecutionKey, RecordExecution> record) {
        if (!isTaskDropped(record.key().getExecutionId())) {
            JsonObject taskProperties = record.value().getExecutionParameters();
            String xsltUrl = taskProperties.get(TRANSFORMATION_XSLT_URL).getAsString();
            String datasetLanguage = taskProperties.get(TRANSFORMATION_DATASET_LANGUAGE).getAsString();
            String datasetCountry = taskProperties.get(TRANSFORMATION_DATASET_COUNTRY).getAsString();
            String datasetName = taskProperties.get(TRANSFORMATION_DATASET_NAME).getAsString();
            try (XsltTransformer xsltTransformer = new XsltTransformer(xsltUrl, datasetName, datasetCountry, datasetLanguage)) {
                byte[] recordData = record.value().getRecordData().getBytes(StandardCharsets.UTF_8);
                StringWriter writer = xsltTransformer.transform(recordData, prepareEuropeanaGeneratedIdsMap(recordData, record.key()));
                String result = writer.toString();
                LOGGER.info("Transformation success for record: key:{} value:{}", record.key(), record.value());
                context.forward(new Record<>(record.key(),
                        new RecordExecutionResult(result,
                                record.value().getExecutionName()),
                        record.timestamp()), TRANSFORMATION_DATABASE_TRANSFER_EXECUTION_RESULTS_SINK_NAME);
            } catch (TransformationException | EuropeanaIdException e) {
                LOGGER.info("Exception occurred during transformation for record: key:{}, Exception e: {}", record.key(), e);
                context.forward(new Record<>(record.key(),
                        new RecordExecutionException(record.value().getExecutionName(), e.getClass().getName(), e.getMessage()),
                        record.timestamp()), TRANSFORMATION_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME);
            }
        } else {
            LOGGER.warn("Task was dropped: key:{}", record.key());
            context.forward(new Record<>(record.key(),
                    new RecordExecutionException(record.value().getExecutionName(), TaskDroppedException.class.getName(), new TaskDroppedException().getMessage()),
                    record.timestamp()), TRANSFORMATION_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME);
        }
    }

    private EuropeanaGeneratedIdsMap prepareEuropeanaGeneratedIdsMap(byte[] recordData, RecordExecutionKey recordExecutionKey) throws EuropeanaIdException {
        EuropeanaGeneratedIdsMap europeanaGeneratedIdsMap = null;
        if (!recordExecutionKey.getDatasetId().isBlank()) {
            String fileDataString = new String(recordData, StandardCharsets.UTF_8);
            EuropeanaIdCreator europeanaIdCreator = new EuropeanaIdCreator();
            europeanaGeneratedIdsMap = europeanaIdCreator.constructEuropeanaId(fileDataString, recordExecutionKey.getDatasetId());
        }
        return europeanaGeneratedIdsMap;
    }

    @Override
    public void close() {
        Processor.super.close();
        closeDatabaseConnection();
    }
}
