package eu.europeana.cloud.processors.databaseQueueReaderProcessors;

import eu.europeana.cloud.dto.database.RecordExecution;
import eu.europeana.cloud.dto.database.RecordExecutionKey;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static eu.europeana.cloud.commons.TopologyNames.*;
import static eu.europeana.cloud.commons.TopologyNodeNames.*;

public class DatabaseQueueReaderProcessor implements Processor<RecordExecutionKey, RecordExecution, RecordExecutionKey, RecordExecution> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseQueueReaderProcessor.class);
    private ProcessorContext<RecordExecutionKey, RecordExecution> context;

    @Override
    public void init(ProcessorContext<RecordExecutionKey, RecordExecution> context) {
        Processor.super.init(context);
        this.context = context;
    }

    @Override
    public void process(Record<RecordExecutionKey, RecordExecution> record) {
        switch (record.value().getExecutionName()) {
            case OAI_TOPOLOGY_NAME -> {
                LOGGER.info("Received oai topology record. key : {} value: {}", record.key(), record.value());
                context.forward(new Record<>(record.key(), record.value(), record.timestamp()), DATABASE_QUEUE_READER_OAI_HARVEST_SINK_NAME);
            }
            case MEDIA_TOPOLOGY_NAME -> {
                LOGGER.info("Received media topology record. key : {} value: {}", record.key(), record.value());
                context.forward(new Record<>(record.key(), record.value(), record.timestamp()), DATABASE_QUEUE_READER_MEDIA_SINK_NAME);
            }
            case VALIDATION_TOPOLOGY_NAME -> {
                LOGGER.info("Received validation topology record. key : {} value: {}", record.key(), record.value());
                context.forward(new Record<>(record.key(), record.value(), record.timestamp()), DATABASE_QUEUE_READER_VALIDATION_SINK_NAME);
            }
            case NORMALIZATION_TOPOLOGY_NAME -> {
                LOGGER.info("Received normalization topology record. key : {} value: {}", record.key(), record.value());
                context.forward(new Record<>(record.key(), record.value(), record.timestamp()), DATABASE_QUEUE_READER_NORMALIZATION_SINK_NAME);
            }
            case ENRICHMENT_TOPOLOGY_NAME -> {
                LOGGER.info("Received enrichment topology record. key : {} value: {}", record.key(), record.value());
                context.forward(new Record<>(record.key(), record.value(), record.timestamp()), DATABASE_QUEUE_READER_ENRICHMENT_SINK_NAME);
            }
            case TRANSFORMATION_TOPOLOGY_NAME -> {
                LOGGER.info("Received transformation topology record. key : {} value: {}", record.key(), record.value());
                context.forward(new Record<>(record.key(), record.value(), record.timestamp()), DATABASE_QUEUE_READER_TRANSFORMATION_SINK_NAME);
            }
            default ->
                    LOGGER.warn("Received unknown topology record. key : {} value: {}", record.key(), record.value());
        }
    }

    @Override
    public void close() {
        Processor.super.close();
    }
}
