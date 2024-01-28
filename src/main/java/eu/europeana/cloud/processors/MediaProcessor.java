package eu.europeana.cloud.processors;

import com.google.gson.Gson;
import eu.europeana.cloud.cassandra.CassandraConnectionProvider;
import eu.europeana.cloud.commons.TopologyPropertyKeys;
import eu.europeana.cloud.mcs.driver.FileServiceClient;
import eu.europeana.cloud.mcs.driver.RecordServiceClient;
import eu.europeana.cloud.service.dps.storm.dao.CassandraTaskInfoDAO;
import eu.europeana.cloud.service.dps.storm.topologies.media.service.AmazonClient;
import eu.europeana.cloud.service.dps.storm.topologies.media.service.ThumbnailUploader;
import eu.europeana.cloud.service.dps.storm.utils.TaskStatusChecker;
import eu.europeana.metis.mediaprocessing.*;
import eu.europeana.metis.mediaprocessing.exception.MediaProcessorException;

import java.util.Properties;

public class MediaProcessor {

    protected final MediaExtractor mediaExtractor;
    protected final ThumbnailUploader thumbnailUploader;
    protected final RdfDeserializer rdfDeserializer;
    protected final RdfSerializer rdfSerializer;
    protected final Properties topologyProperties;
    protected final TaskStatusChecker taskStatusChecker;
    protected final CassandraConnectionProvider cassandraConnectionProvider;
    protected final Gson gson;
    protected final FileServiceClient fileServiceClient;
    protected final CassandraTaskInfoDAO taskInfoDAO;
    protected final AmazonClient amazonClient;
    protected final RecordServiceClient recordServiceClient;

    public MediaProcessor(Properties topologyProperties) {
        this.topologyProperties = topologyProperties;
        try {
            fileServiceClient = new FileServiceClient(topologyProperties.getProperty(TopologyPropertyKeys.MCS_ADDRESS),
                    topologyProperties.getProperty(TopologyPropertyKeys.MCS_USER),
                    topologyProperties.getProperty(TopologyPropertyKeys.MCS_PASSWORD));
            rdfDeserializer = new RdfConverterFactory().createRdfDeserializer();
            rdfSerializer = new RdfConverterFactory().createRdfSerializer();
            mediaExtractor = new MediaProcessorFactory().createMediaExtractor();
            amazonClient = AmazonClient.builder()
                    .awsAccessKey(topologyProperties.getProperty(TopologyPropertyKeys.AWS_CREDENTIALS_ACCESSKEY))
                    .awsBucket(topologyProperties.getProperty(TopologyPropertyKeys.AWS_CREDENTIALS_BUCKET))
                    .awsEndPoint(topologyProperties.getProperty(TopologyPropertyKeys.AWS_CREDENTIALS_ENDPOINT))
                    .awsSecretKey(topologyProperties.getProperty(TopologyPropertyKeys.AWS_CREDENTIALS_SECRETKEY))
                    .build();
            amazonClient.init();
            cassandraConnectionProvider = new CassandraConnectionProvider(
                    topologyProperties.getProperty(TopologyPropertyKeys.CASSANDRA_HOSTS),
                    Integer.parseInt(topologyProperties.getProperty(TopologyPropertyKeys.CASSANDRA_PORT)),
                    topologyProperties.getProperty(TopologyPropertyKeys.CASSANDRA_KEYSPACE_NAME),
                    topologyProperties.getProperty(TopologyPropertyKeys.CASSANDRA_USERNAME),
                    topologyProperties.getProperty(TopologyPropertyKeys.CASSANDRA_SECRET_TOKEN)
            );
            taskInfoDAO = new CassandraTaskInfoDAO(cassandraConnectionProvider);
            taskStatusChecker = new TaskStatusChecker(new CassandraTaskInfoDAO(cassandraConnectionProvider));
            recordServiceClient = new RecordServiceClient(topologyProperties.getProperty(TopologyPropertyKeys.MCS_ADDRESS),
                    topologyProperties.getProperty(TopologyPropertyKeys.MCS_USER),
                    topologyProperties.getProperty(TopologyPropertyKeys.MCS_PASSWORD));
            thumbnailUploader = new ThumbnailUploader(taskStatusChecker, amazonClient);
            gson = new Gson();
        } catch (MediaProcessorException e) {
            throw new RuntimeException(e);
        }
    }

    // For testing purposes
    public boolean isTaskDropped(String taskId) {
//    try {
        return false;
//      return taskInfoDAO.isDroppedTask(Long.parseLong(taskId));
//    } catch (TaskInfoDoesNotExistException e) {
//      return true;
//    }
    }
}
