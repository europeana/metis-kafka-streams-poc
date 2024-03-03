package eu.europeana.cloud.commons;

public class TopologyTopicNames {
    public static final String DATABASE_TRANSFER_RECORD_EXECUTION_TOPIC_NAME = "connect.public.record_execution";
    public static final String DATABASE_TRANSFER_RECORD_EXECUTION_RESULT_TOPIC_NAME = "connect.public.record_execution_result";
    public static final String DATABASE_TRANSFER_RECORD_EXECUTION_EXCEPTION_TOPIC_NAME = "connect.public.record_execution_exception";
    public static final String OAI_HARVEST_SOURCE_TOPIC_NAME = "oai-harvest-topology";
    public static final String VALIDATION_SOURCE_TOPIC_NAME = "validation-topology";
    public static final String MEDIA_SOURCE_TOPIC_NAME = "media-topology";
    public static final String NORMALIZATION_SOURCE_TOPIC_NAME = "normalization-topology";
    public static final String TRANSFORMATION_SOURCE_TOPIC_NAME = "transformation-topology";
    public static final String ENRICHMENT_SOURCE_TOPIC_NAME = "enrichment-topology";

    private TopologyTopicNames() {
    }
}
