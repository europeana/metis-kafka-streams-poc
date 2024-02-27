package eu.europeana.cloud.dto.database;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class RecordExecutionKey {
    @SerializedName("dataset_id")
    private String datasetId;
    @SerializedName("execution_id")
    private String executionId;
    @SerializedName("record_id")
    private String recordId;
}
