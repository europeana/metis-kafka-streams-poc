package eu.europeana.cloud.commons;

public class TopologyPropertyKeys {
    public static final String MCS_USER = "MCS_USER";
    public static final String MCS_PASSWORD = "MCS_PASSWORD";
    public static final String MCS_ADDRESS = "MCS_URL";
    public static final String CASSANDRA_HOSTS = "CASSANDRA_HOSTS";
    public static final String CASSANDRA_PORT = "CASSANDRA_PORT";
    public static final String CASSANDRA_KEYSPACE_NAME = "CASSANDRA_KEYSPACE_NAME";
    public static final String CASSANDRA_USERNAME = "CASSANDRA_USERNAME";
    public static final String CASSANDRA_SECRET_TOKEN = "CASSANDRA_PASSWORD";
    public static final String AWS_CREDENTIALS_ACCESSKEY = "AWS_CREDENTIALS_ACCESSKEY";
    public static final String AWS_CREDENTIALS_SECRETKEY = "AWS_CREDENTIALS_SECRETKEY";
    public static final String AWS_CREDENTIALS_ENDPOINT = "AWS_CREDENTIALS_ENDPOINT";
    public static final String AWS_CREDENTIALS_BUCKET = "AWS_CREDENTIALS_BUCKET";

    public static final String DATABASE_NAME = "DB_NAME";
    public static final String DATABASE_USER = "DB_USER";
    public static final String DATABASE_PASSWORD = "DB_PASSWORD";
    public static final String DATABASE_HOST = "DB_ADDRESS";
    public static final String DATABASE_PORT = "DB_PORT";

    public static final String KAFKA_HOSTS = "KAFKA_HOSTS";

    public static final String DEREFERENCE_SERVICE_URL = "DEREFERENCE_SERVICE_URL";
    public static final String ENTITY_MANAGEMENT_URL = "ENTITY_MANAGEMENT_URL";
    public static final String ENTITY_API_URL = "ENTITY_API_URL";
    public static final String ENTITY_API_KEY = "ENTITY_API_KEY";

    private TopologyPropertyKeys() {
    }
}
