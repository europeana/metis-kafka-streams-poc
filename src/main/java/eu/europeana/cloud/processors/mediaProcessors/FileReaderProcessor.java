package eu.europeana.cloud.processors.mediaProcessors;

import eu.europeana.cloud.dto.queues.ErrorInformation;
import eu.europeana.cloud.dto.queues.Message;
import eu.europeana.cloud.service.mcs.exception.MCSException;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Properties;

import static eu.europeana.cloud.commons.TopologyNodeNames.MEDIA_INTERMEDIATE_SINK_NAME;
import static eu.europeana.cloud.commons.TopologyNodeNames.MEDIA_RESOURCE_GETTER_PROCESSOR_NAME;

public class FileReaderProcessor extends MediaProcessor implements
        Processor<String, Message, String, Message> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileReaderProcessor.class);
    private ProcessorContext<String, Message> context;

    public FileReaderProcessor(Properties topologyProperties) {
        super(topologyProperties);
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
            try {
                Path path = Path.of(record.value().getFileUrl());
                LOGGER.info("File path: {}", path);
                String content = new String(fileServiceClient.getFile(record.value().getFileUrl()).readAllBytes(), StandardCharsets.UTF_8);

                Message message = record.value();
                message.setSourceFileData(content);
                context.forward(new Record<>(record.key(), message, record.timestamp()), MEDIA_RESOURCE_GETTER_PROCESSOR_NAME);
            } catch (IOException e) {
                LOGGER.error("Error during file download process: {}", e.getMessage());
                context.forward(new Record<>(record.value().getTaskId(), Message.builder()
                                .errorInformation(new ErrorInformation(e.getMessage(),
                                        "Error during file download process")).build(), record.timestamp()),
                        MEDIA_INTERMEDIATE_SINK_NAME);
            } catch (MCSException e) {
                LOGGER.error("Error during file download process due to mcs connectivity error: {}", e.getMessage());
                context.forward(new Record<>(record.value().getTaskId(), Message.builder()
                                .errorInformation(new ErrorInformation(e.getMessage(),
                                        "Error during file download process due to mcs connectivity error")).build(), record.timestamp()),
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
