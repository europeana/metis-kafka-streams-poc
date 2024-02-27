package eu.europeana.cloud.dto.queues;

import lombok.Builder;
import lombok.Data;

import java.util.Arrays;
import java.util.Properties;

/**
 * Message that is sent vi Kafka topics
 */
@Data
@Builder
public class Message {
    private String sourceFileData;
    private Properties metadataProperties;
    private ErrorInformation errorInformation;
    private String throttleLevel;
    private String taskId;
    private String fileUrl;
    private byte[] enrichedFileData;
    private String outputDataset;
    private String outputFilename;


    @Override
    public String toString() {
        return "Message{" +
                "metadataProperties=" + metadataProperties +
                ", errorInformation=" + errorInformation +
                ", throttleLevel='" + throttleLevel + '\'' +
                ", taskId='" + taskId + '\'' +
                ", fileUrl='" + fileUrl + '\'' +
                ", sourceFileDataSize='" + ((sourceFileData != null) ? sourceFileData.length() : null) + '\'' +
                ", fileDataSize=" + ((enrichedFileData != null) ? Arrays.toString(enrichedFileData).length() : null) +
                ", outputDataset='" + outputDataset + '\'' +
                ", outputFilename='" + outputFilename + '\'' +
                '}';
    }
}
