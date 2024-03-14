package eu.europeana.cloud.processors.commonProcessors;

import eu.europeana.cloud.dto.RecordExecutionException;
import eu.europeana.cloud.dto.RecordExecutionKey;
import eu.europeana.cloud.dto.RecordExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Properties;

import static eu.europeana.cloud.commons.TopologyPropertyKeys.*;

public class CommonProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonProcessor.class);
    private final Connection connection;
    private final PreparedStatement taskStatusStatement;
    private final PreparedStatement insertRecordExecutionResultStatement;
    private final PreparedStatement insertRecordExecutionExceptionStatement;

    public CommonProcessor(Properties properties) {
        try {
            String databaseName = properties.getProperty(DATABASE_NAME);
            String databaseUser = properties.getProperty(DATABASE_USER);
            String databasePassword = properties.getProperty(DATABASE_PASSWORD);
            String databaseHost = properties.getProperty(DATABASE_HOST);
            String databasePort = properties.getProperty(DATABASE_PORT);
            String databaseUrl = String.format("jdbc:postgresql://%s:%s/%s", databaseHost, databasePort, databaseName);
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword);
            LOGGER.info("Connected to database successfully");
            taskStatusStatement = connection.prepareStatement("select status from execution_status where execution_id = ?");
            insertRecordExecutionResultStatement = connection.prepareStatement("insert into record_execution_result (dataset_id, execution_id, record_id, record_result_data, execution_name) values (?, ?, ?, ?, ?)");
            insertRecordExecutionExceptionStatement = connection.prepareStatement("insert into record_execution_exception (dataset_id, execution_id, record_id, execution_name, exception_name, exception_content) values (?, ?, ?, ?, ?, ?)");
            LOGGER.info("Prepared statement successfully");
        } catch (SQLException | ClassNotFoundException e) {
            LOGGER.warn("Error connecting to database: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    protected boolean isTaskDropped(String taskId) {
        try {
            taskStatusStatement.setString(1, taskId);
            ResultSet rs = taskStatusStatement.executeQuery();
            if (rs.next()) {
                String taskStatus = rs.getString(1);
                LOGGER.info("task with id {} has status {}", taskId, taskStatus);
                return (taskStatus.equals("DROPPED") || taskStatus.equals("FAILED"));
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected void insertRecordExecutionResult(RecordExecutionKey key, RecordExecutionResult resultValue) {
        try {
            insertRecordExecutionResultStatement.setString(1, key.getDatasetId());
            insertRecordExecutionResultStatement.setString(2, key.getExecutionId());
            insertRecordExecutionResultStatement.setString(3, key.getRecordId());
            insertRecordExecutionResultStatement.setString(4, resultValue.getRecordResultData());
            insertRecordExecutionResultStatement.setString(5, resultValue.getExecutionName());
            insertRecordExecutionResultStatement.execute();
        } catch (SQLException e) {
            LOGGER.error("Error during query execution: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    protected void insertRecordExecutionException(RecordExecutionKey key, RecordExecutionException exceptionValue) {
        try {
            insertRecordExecutionExceptionStatement.setString(1, key.getDatasetId());
            insertRecordExecutionExceptionStatement.setString(2, key.getExecutionId());
            insertRecordExecutionExceptionStatement.setString(3, key.getRecordId());
            insertRecordExecutionExceptionStatement.setString(4, exceptionValue.getExecutionName());
            insertRecordExecutionExceptionStatement.setString(5, exceptionValue.getExceptionName());
            insertRecordExecutionExceptionStatement.setString(6, exceptionValue.getExceptionContent());
            insertRecordExecutionExceptionStatement.execute();
        } catch (SQLException e) {
            LOGGER.error("Error during query execution: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    protected void closeDatabaseConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            LOGGER.warn("Can't close database connection: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
