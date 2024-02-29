package eu.europeana.cloud.processors.normalizationProcessors;

import eu.europeana.cloud.dto.database.*;
import eu.europeana.normalization.NormalizerFactory;
import eu.europeana.normalization.model.NormalizationResult;
import eu.europeana.normalization.util.NormalizationConfigurationException;
import eu.europeana.normalization.util.NormalizationException;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static eu.europeana.cloud.commons.TopologyNodeNames.NORMALIZATION_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME;
import static eu.europeana.cloud.commons.TopologyNodeNames.NORMALIZATION_DATABASE_TRANSFER_EXECUTION_RESULTS_SINK_NAME;

public class NormalizationProcessor implements Processor<RecordExecutionKey, RecordExecution, RecordExecutionKey, RecordExecutionProduct> {
    private static final Logger LOGGER = LoggerFactory.getLogger(NormalizationProcessor.class);
    private ProcessorContext<RecordExecutionKey, RecordExecutionProduct> context;

    private final NormalizerFactory normalizationFactory = new NormalizerFactory();

    @Override
    public void init(ProcessorContext<RecordExecutionKey, RecordExecutionProduct> context) {
        Processor.super.init(context);
        this.context = context;
    }

    @Override
    public void process(Record<RecordExecutionKey, RecordExecution> record) {
        try {
            NormalizationResult normalizedResult = normalizationFactory.getNormalizer().normalize(record.value().getRecordData());
            if (normalizedResult.getErrorMessage() != null) {
                throw new RuntimeException(normalizedResult.getErrorMessage());
            }
            String normalizedData = normalizedResult.getNormalizedRecordInEdmXml();
            LOGGER.info("Normalization success for record: key:{} value:{}", record.key(), record.value());
            context.forward(new Record<>(record.key(),
                    new RecordExecutionResult(normalizedData,
                            record.value().getExecutionName()),
                    record.timestamp()), NORMALIZATION_DATABASE_TRANSFER_EXECUTION_RESULTS_SINK_NAME);
        } catch (NormalizationException | NormalizationConfigurationException e) {
            LOGGER.info("Exception occurred during normalization for record: key:{}, Exception e: {}", record.key(), e);
            context.forward(new Record<>(record.key(),
                    new RecordExecutionException(record.value().getExecutionName(), e.getClass().getName(), e.getMessage()),
                    record.timestamp()), NORMALIZATION_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME);
        }
    }

    @Override
    public void close() {
        Processor.super.close();
    }
}
