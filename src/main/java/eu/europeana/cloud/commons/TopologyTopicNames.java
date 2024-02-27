package eu.europeana.cloud.commons;

public class TopologyTopicNames {
    public static final String MEDIA_INPUT_TOPIC_NAME = "media-input";
    public static final String MEDIA_INTERMEDIATE_TOPIC_NAME = "media-output";

    public static final String DATABASE_QUEUE_READER_TOPIC_NAME = "connect.public.record_execution";
    public static final String OAI_HARVEST_SOURCE_TOPIC_NAME = "oai-harvest-topology";
    public static final String VALIDATION_SOURCE_TOPIC_NAME = "validation-topology";
    public static final String MEDIA_SOURCE_TOPIC_NAME = "media-topology";
    public static final String NORMALIZATION_SOURCE_TOPIC_NAME = "normalization-topology";
    public static final String TRANSFORMATION_SOURCE_TOPIC_NAME = "transformation-topology";
    public static final String ENRICHMENT_SOURCE_TOPIC_NAME = "enrichment-topology";
}
