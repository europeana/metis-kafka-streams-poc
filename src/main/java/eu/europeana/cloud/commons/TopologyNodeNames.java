package eu.europeana.cloud.commons;

public class TopologyNodeNames {
    public static final String MEDIA_INPUT_SOURCE_NAME = "media-input";
    public static final String MEDIA_FILE_READER_PROCESSOR_NAME = "file-reader";
    public static final String MEDIA_RESOURCE_GETTER_PROCESSOR_NAME = "rdf-resource-getter";
    public static final String MEDIA_RESOURCE_EXTRACTOR_PROCESSOR_NAME = "media-extractor";
    public static final String MEDIA_ENRICH_PROCESSOR_NAME = "media-enricher";
    public static final String MEDIA_RECORD_WRITER_PROCESSOR_NAME = "record-writer";
    public static final String MEDIA_INTERMEDIATE_SINK_NAME = "media-output";
    public static final String MEDIA_NOTIFICATION_SOURCE_NAME = "notification-input";
    public static final String MEDIA_NOTIFICATION_PROCESSOR_NAME = "notification-node";

    public static final String DATABASE_TRANSFER_SOURCE_NAME = "database-queue-source";
    public static final String DATABASE_TRANSFER_PROCESSOR_NAME = "database-queue-processor";
    public static final String DATABASE_TRANSFER_OAI_HARVEST_SINK_NAME = "database-harvest-sink";
    public static final String DATABASE_TRANSFER_MEDIA_SINK_NAME = "database-media-sink";
    public static final String DATABASE_TRANSFER_VALIDATION_SINK_NAME = "database-validation-sink";
    public static final String DATABASE_TRANSFER_NORMALIZATION_SINK_NAME = "database-normalization-sink";
    public static final String DATABASE_TRANSFER_TRANSFORMATION_SINK_NAME = "database-transformation-sink";
    public static final String DATABASE_TRANSFER_ENRICHMENT_SINK_NAME = "database-enrichment-sink";

    public static final String OAI_HARVEST_TOPOLOGY_SOURCE_NAME = "oai-harvest-source";
    public static final String OAI_HARVEST_HEADER_EXTRACTOR_PROCESSOR_NAME = "oai-header-extractor";
    public static final String OAI_HARVEST_HARVESTER_PROCESSOR_NAME = "oai-harvester";
    public static final String OAI_HARVEST_DATABASE_TRANSFER_EXECUTION_RESULTS_SINK_NAME = "oai-harvest-database-transfer-execution-results-sink";
    public static final String OAI_HARVEST_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME = "oai-harvest-database-transfer-execution-exception-sink";

    public static final String VALIDATION_TOPOLOGY_SOURCE_NAME = "validation-source";
    public static final String VALIDATION_PROCESSOR_NAME = "validation-processor";
    public static final String VALIDATION_DATABASE_TRANSFER_EXECUTION_RESULTS_SINK_NAME = "validation-database-transfer-execution-results-sink";
    public static final String VALIDATION_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME = "validation-database-transfer-execution-exception-sink";


    public static final String TRANSFORMATION_TOPOLOGY_SOURCE_NAME = "transformation-source";
    public static final String TRANSFORMATION_PROCESSOR_NAME = "transformation-processor";
    public static final String TRANSFORMATION_DATABASE_TRANSFER_EXECUTION_RESULTS_SINK_NAME = "transformation-database-transfer-execution-results-sink";
    public static final String TRANSFORMATION_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME = "transformation-database-transfer-execution-exception-sink";

    public static final String NORMALIZATION_TOPOLOGY_SOURCE_NAME = "normalization-source";
    public static final String NORMALIZATION_PROCESSOR_NAME = "normalization-processor";
    public static final String NORMALIZATION_DATABASE_TRANSFER_EXECUTION_RESULTS_SINK_NAME = "normalization-database-transfer-execution-results-sink";
    public static final String NORMALIZATION_DATABASE_TRANSFER_EXECUTION_EXCEPTION_SINK_NAME = "normalization-database-transfer-execution-exception-sink";

    private TopologyNodeNames() {
    }
}
