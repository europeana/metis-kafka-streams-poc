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
    public static final String DATABASE_QUEUE_READER_SOURCE_NAME = "database-queue-source";
    public static final String DATABASE_QUEUE_READER_PROCESSOR_NAME = "database-queue-processor";
    public static final String DATABASE_QUEUE_READER_OAI_HARVEST_SINK_NAME = "database-harvest-sink";
    public static final String DATABASE_QUEUE_READER_MEDIA_SINK_NAME = "database-media-sink";
    public static final String DATABASE_QUEUE_READER_VALIDATION_SINK_NAME = "database-validation-sink";

    public static final String DATABASE_QUEUE_READER_NORMALIZATION_SINK_NAME = "database-normalization-sink";
    public static final String DATABASE_QUEUE_READER_TRANSFORMATION_SINK_NAME = "database-transformation-sink";
    public static final String DATABASE_QUEUE_READER_ENRICHMENT_SINK_NAME = "database-enrichment-sink";
}
