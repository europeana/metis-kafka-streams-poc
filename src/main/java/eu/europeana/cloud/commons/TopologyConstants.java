package eu.europeana.cloud.commons;

public class TopologyConstants {
    private TopologyConstants() {
    }

    public static final int HIGH_TROTTING_SLEEP_TIME_IN_SECONDS = 10;
    public static final int MEDIUM_TROTTING_SLEEP_TIME_IN_SECONDS = 5;
    public static final int LOW_TROTTING_SLEEP_TIME_IN_SECONDS = 1;

    public static final int DEFAULT_TOPIC_PARTITION_COUNT = 1;
    public static final int DEFAULT_TOPIC_REPLICATION_FACTOR = 3;

    public static final String VALIDATION_TOPOLOGY_EXTERNAL_VALIDATION = "external";
    public static final String VALIDATION_TOPOLOGY_INTERNAL_VALIDATION = "internal";
}
