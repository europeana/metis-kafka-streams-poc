package eu.europeana.cloud.processors.validationProcessors;

import com.google.gson.JsonObject;
import eu.europeana.cloud.dto.database.*;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import eu.europeana.validation.model.ValidationResult;
import eu.europeana.validation.service.ValidationExecutionService;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static eu.europeana.cloud.commons.ExecutionPropertyKeys.VALIDATION_SUB_TYPE;
import static eu.europeana.cloud.commons.TopologyNodeNames.VALIDATION_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME;

public class ValidationProcessor implements Processor<RecordExecutionKey, RecordExecution, RecordExecutionKey, RecordExecutionProduct> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationProcessor.class);
    private static final String EDM_SORTER_FILE_URL = "http://ftp.eanadev.org/schema_zips/edm_sorter_20230809.xsl";
    private final Properties properties = new Properties();
    private XsltTransformer xsltTransformer;
    private ProcessorContext<RecordExecutionKey, RecordExecutionProduct> context;


    @Override
    public void init(ProcessorContext<RecordExecutionKey, RecordExecutionProduct> context) {
        Processor.super.init(context);
        this.context = context;
        prepareProperties();
    }

    @Override
    public void process(Record<RecordExecutionKey, RecordExecution> record) {
        LOGGER.info("Received validation topology record. key : {} value: {}", record.key(), record.value());
        JsonObject executionParameters = record.value().getExecutionParameters();
        String validationType;
        if (executionParameters.get(VALIDATION_SUB_TYPE) == null) {
            validationType = "external";
        } else {
            validationType = executionParameters.get(VALIDATION_SUB_TYPE).getAsString();
        }
        String schema, rootFileLocation, schematronFileLocation;
        switch (validationType) {
            case "external" -> {
                schema = properties.getProperty("predefinedSchemas.edm-external.url");
                rootFileLocation = properties.getProperty("predefinedSchemas.edm-external.rootLocation");
                schematronFileLocation = properties.getProperty("predefinedSchemas.edm-external.schematronLocation");
            }
            case "internal" -> {
                schema = properties.getProperty("predefinedSchemas.edm-internal.url");
                rootFileLocation = properties.getProperty("predefinedSchemas.edm-internal.rootLocation");
                schematronFileLocation = properties.getProperty("predefinedSchemas.edm-internal.schematronLocation");
            }
            default -> throw new IllegalStateException("Unexpected validation type: " + validationType);
        }

        ValidationExecutionService validationService = new ValidationExecutionService(properties);
        try {
            xsltTransformer = new XsltTransformer(EDM_SORTER_FILE_URL);
            StringWriter writer = xsltTransformer.transform(record.value().getRecordData().getBytes(StandardCharsets.UTF_8), null);
            ValidationResult validationResult = validationService.singleValidation(schema, rootFileLocation, schematronFileLocation, writer.toString());
            if (validationResult.isSuccess()) {
                LOGGER.info("Validation success for record: key:{} value:{}", record.key(), record.value());
                context.forward(new Record<>(record.key(),
                        new RecordExecutionResult(record.value().getRecordData(), record.value().getExecutionName()),
                        record.timestamp()), VALIDATION_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME);
            } else {
                LOGGER.info("Validation failure for record: key:{} value:{}", record.key(), record.value());
                context.forward(new Record<>(record.key(),
                        new RecordExecutionException(record.value().getExecutionName(), "ValidationFailureException", validationResult.getMessage()),
                        record.timestamp()), VALIDATION_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME);
            }
        } catch (TransformationException e) {
            LOGGER.info("Exception occurred during validation for record: key:{} value:{}", record.key(), record.value());
            context.forward(new Record<>(record.key(),
                    new RecordExecutionException(record.value().getExecutionName(), e.getClass().getName(), e.getMessage()),
                    record.timestamp()), VALIDATION_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME);
        }


    }


    private Properties prepareProperties() {
        properties.setProperty("predefinedSchemas", "localhost");

        properties.setProperty("predefinedSchemas.edm-internal.url",
                "http://ftp.eanadev.org/schema_zips/europeana_schemas-20220809.zip");
        properties.setProperty("predefinedSchemas.edm-internal.rootLocation", "EDM-INTERNAL.xsd");
        properties.setProperty("predefinedSchemas.edm-internal.schematronLocation", "schematron/schematron-internal.xsl");

        properties.setProperty("predefinedSchemas.edm-external.url",
                "http://ftp.eanadev.org/schema_zips/europeana_schemas-20220809.zip");
        properties.setProperty("predefinedSchemas.edm-external.rootLocation", "EDM.xsd");
        properties.setProperty("predefinedSchemas.edm-external.schematronLocation", "schematron/schematron.xsl");
        return properties;
    }

    @Override
    public void close() {
        Processor.super.close();
    }
}