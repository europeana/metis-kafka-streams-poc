package eu.europeana.cloud.processors.commonProcessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Properties;

import static eu.europeana.cloud.commons.TopologyPropertyKeys.*;

public class CommonProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonProcessor.class);
    private final Connection connection;
    private final PreparedStatement taskStatusStatement;

    public CommonProcessor(Properties properties) {
        try {
            String databaseName = properties.getProperty(DATABASE_NAME);
            String databaseUser = properties.getProperty(DATABASE_USER);
            String databasePassword = properties.getProperty(DATABASE_PASSWORD);
            String databaseHost = properties.getProperty(DATABASE_HOST);
            String databasePort = properties.getProperty(DATABASE_PORT);
            String databaseUrl = String.format("jdbc:postgresql://%s:%s/%s", databaseHost, databasePort, databaseName);
            connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword);
            LOGGER.info("Connected to database successfully");
            taskStatusStatement = connection.prepareStatement("select status from execution_status where execution_id = ?");
            LOGGER.info("Prepared statement successfully");
        } catch (SQLException e) {
            LOGGER.warn("Error connecting to database: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    protected boolean isTaskDropped(String taskId) {
        try {
            taskStatusStatement.setString(1, taskId);
            ResultSet rs = taskStatusStatement.executeQuery();
            if (rs.first()) {
                String taskStatus = rs.getString(1);
                LOGGER.info("task with id {} has status {}", taskId, taskStatus);
                return (taskStatus.equals("DROPPED") || taskStatus.equals("FAILED"));
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
